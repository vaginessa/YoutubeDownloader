package com.dt3264.MP3Converter.worker

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.dt3264.MP3Converter.annotation.JobStatus
import com.dt3264.MP3Converter.annotation.JobStatus.RUNNING
import com.dt3264.MP3Converter.util.getKnownReasonOf
import com.dt3264.MP3Converter.job.Job
import com.dt3264.MP3Converter.job.JobManager
import com.dt3264.MP3Converter.util.reportNonFatal
import com.dt3264.MP3Converter.util.UriUtils
import com.dt3264.MP3Converter.util.catchAll
import com.dt3264.MP3Converter.util.closeQuietly
import com.dt3264.MP3Converter.util.deleteRecursiveIgnoreError
import java.io.*

private const val MAX_LOG_FILE_SIZE = 50 * 1024 // 50 KB

class JobWorkerThread(
        private val appContext: Context,
        private var job: Job,
        private val jobManager: JobManager,
        private val onCompleteListener: (Job) -> Unit,
        private val onErrorListener: (Job, Throwable?) -> Unit,
        private val workingPaths: WorkingPaths = makeWorkingPaths(appContext)
) : Thread() {
    companion object {
        private fun getPid(process: Process): Int? {
            return try {
                val field = process.javaClass.getDeclaredField("pid")
                field.isAccessible = true
                field?.getInt(process)
            } catch (ignore: Throwable) {
                null
            }
        }
    }

    private var logFile: File? = null
    private var jobTempDir: File? = null

    override fun run() {

        jobTempDir = try {
            workingPaths.getTempDirForJob(job.id)
        } catch (error: Throwable) {
            onError(error, "Error: ${error.message}")
            return
        }

        logFile = catchAll(printLog = true) { workingPaths.getLogFileOfJob(job.id) }

        val commandResolver = try {
            CommandResolver.resolve(appContext, jobTempDir!!, workingPaths.ffmpegPath, job.command)
        } catch (resolveCommandError: Throwable) {
            onError(resolveCommandError, "Init failed: ${resolveCommandError.message}")
            return
        }

        //val startTime = System.currentTimeMillis()

        var process: Process? = null
        try {
            process = startProcess(commandResolver)
        } catch (securityException: SecurityException) {
            onError(securityException, "Security error when start " +
                    "FFmpeg process: ${securityException.message}")
        } catch (interruptException: InterruptedException) {
            onError(interruptException, "Job was canceled")
        } catch (startProcessError: Throwable) {
            onError(startProcessError, "Start FFmpeg failed: ${startProcessError.message}")
        }

        if (process === null) return  // error happened

        val loggerThread = LoggerThread(process.errorStream, jobManager)
        loggerThread.start()

        // wait process complete
        val exitCode = try {
            process.waitFor()
        } catch (interruptException: InterruptedException) {
            onError(interruptException, "Job was canceled")
            // shutdown process
            catchAll {
                process.inputStream.closeQuietly()
                process.outputStream.closeQuietly()
                getPid(process)?.let { pid ->
                    android.os.Process.killProcess(pid)
                }
                process.destroy()
            }

            catchAll {
                loggerThread.interrupt()
                loggerThread.join()
            }
            return
        }

        catchAll {
            // wait logger thread complete
            loggerThread.join()
        }

        // Convert successful, now copy temp output to real target output
        job = jobManager.updateJobStatus(job, RUNNING, "Convert success, copying output...")
        var tempIs: InputStream? = null
        var destOs: OutputStream? = null
        try {
            tempIs = commandResolver.tempFileSourceInput.openInputStream()
            destOs = commandResolver.sourceOutput.openOutputStream()
            tempIs.copyTo(destOs)
        } catch (error: Throwable) {
            onError(error, "Write output file failed: ${error.message}. Please check output path.")
            return
        } finally {
            tempIs.closeQuietly()
            destOs.closeQuietly()
            commandResolver.tempFileSourceInput.closeQuietly()
            commandResolver.sourceOutput.closeQuietly()
        }

        // completed
        job = jobManager.updateJobStatus(job, JobStatus.COMPLETED)
        onCompleteListener(job)
        catchAll {
            UriUtils.getPathFromUri(appContext, Uri.parse(commandResolver.command.output))
        }?.let { filePath ->
            // notify media scanner
            MediaScannerConnection.scanFile(appContext, arrayOf(filePath),
                    null, null)
        }

        // clean up temp folder
        jobTempDir?.deleteRecursiveIgnoreError()
    }

    private fun startProcess(commandResolver: CommandResolver): Process {
        val cmdArray = listOf(
                "sh", "-c",
                commandResolver.execCommand
        )
        return ProcessBuilder()
                .apply { environment().putAll(commandResolver.command.environmentVars) }
                .command(cmdArray)
                .start()
    }

    private fun onError(error: Throwable, message: String) {
        val errorDetail = getKnownReasonOf(error, appContext, message)
        job = jobManager.updateJobStatus(job, JobStatus.FAILED, errorDetail)
        onErrorListener(job, error)

        // clean up temp folder
        jobTempDir?.deleteRecursiveIgnoreError()

        reportNonFatal(error, "JobWorkerThread#onError", message)
    }

    private inner class LoggerThread(val input: InputStream, val jobManager: JobManager) : Thread() {

        private val regex = Regex("(\\w+=\\s*([^\\s]+))")
        private val durationRe = Regex("Duration:\\s(\\d\\d:\\d\\d:\\d\\d)")
        private var fileSize: Int = 0
        private var skippedLine: Int = 0
        private var durationSeconds: Long? = null

        var lastLine: String? = null


        init {
            jobManager.recordLiveLog("")
        }

        override fun run() {
            val logFileOutputStream: OutputStreamWriter? = catchAll {
                logFile?.let {
                    OutputStreamWriter(object : FileOutputStream(it) {
                        override fun write(b: ByteArray?, off: Int, len: Int) {
                            fileSize += len
                            super.write(b, off, len)
                        }
                    })
                }
            }
            var converting = false
            catchAll {
                InputStreamReader(input).use { inputReader ->
                    inputReader.forEachLine { line ->
                        lastLine = line

                        if (durationSeconds === null && !converting) {
                            durationRe.find(line)?.let { match ->
                                durationSeconds = parseDurationString(match.groupValues[1])
                            }
                        }

                        var size: String? = null
                        var bitrate: String? = null
                        var time: String? = null
                        regex.findAll(line).forEach { matchResult ->
                            with(matchResult.groupValues[1]) {
                                when {
                                    startsWith("size=") -> size = matchResult.groupValues[2]
                                    startsWith("bitrate=") -> bitrate = matchResult.groupValues[2]
                                    startsWith("time=") -> time = matchResult.groupValues[2]
                                }
                            }
                        }
                        val stringBuilder = StringBuilder()
                        if (size !== null) {
                            converting = true
                            stringBuilder.append(size)
                        }

                        if (durationSeconds !== null && durationSeconds!! > 0 && time !== null) {
                            val percent = (parseDurationString(time!!) ?: 0) * 100 / durationSeconds!!
                            stringBuilder.append(" $percent%")
                        } else if (bitrate !== null) {
                            stringBuilder.append(" br=").append(bitrate)
                        }

                        if (!stringBuilder.isBlank()) {
                            jobManager.recordLiveLog(stringBuilder.toString())
                        }
                        catchAll<Unit>(printLog = true) {
                            if (fileSize < MAX_LOG_FILE_SIZE) {
                                logFileOutputStream?.appendln(line)
                            } else {
                                skippedLine++
                            }
                        }
                    }
                }
            }
            if (skippedLine > 0) {
                logFileOutputStream?.appendln("[...]\n$skippedLine lines was skipped\n[...]\n$lastLine")
            }
            logFileOutputStream.closeQuietly()
            jobManager.recordLiveLog("")
        }

        /**
         * Parse time in format hh:mm:ss to number of seconds
         */
        private fun parseDurationString(duration: String): Long? {
            val split = duration.take(8).split(":")
            if (split.size == 3) {
                return (split[0].toLongOrNull() ?: 0) * 3600 +
                        (split[1].toLongOrNull() ?: 0) * 60 +
                        (split[2].toLongOrNull() ?: 0)
            }
            return null
        }
    }

}