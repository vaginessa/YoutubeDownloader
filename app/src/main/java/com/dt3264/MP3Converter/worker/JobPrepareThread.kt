package com.dt3264.MP3Converter.worker

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dt3264.MP3Converter.annotation.JobStatus
import com.dt3264.MP3Converter.util.getKnownReasonOf
import com.dt3264.MP3Converter.job.Job
import com.dt3264.MP3Converter.job.JobManager
import com.dt3264.MP3Converter.util.reportNonFatal
import com.dt3264.MP3Converter.util.deleteRecursiveIgnoreError
import java.io.BufferedOutputStream
import java.io.File
import java.lang.Exception

/**
 * Some inputs with scheme http://, https://, content:// can't be handled
 * by FFmpeg internally (only support file and pipe protocol). These inputs need to copy to temp folder
 * and will be deleted after job completed.
 *
 * JobPrepareThread was created to download/copy inputs to temp folder, where ffmpeg can read directly.
 */
class JobPrepareThread(
        private val appContext: Context,
        private var job: Job,
        private val jobManager: JobManager,
        private val onCompleteListener: (Job) -> Unit,
        private val onErrorListener: (Job, Throwable?) -> Unit,
        private val workingPaths: WorkingPaths = makeWorkingPaths(appContext)
) : Thread() {
    private var jobTempDir: File? = null

    override fun run() {
        // start preparing

        jobTempDir = try {
            workingPaths.getTempDirForJob(job.id)
        } catch (error: Throwable) {
            onError(error, "Error: ${error.message}")
            return
        }

        val contentResolver = appContext.contentResolver

        val inputs = job.command.inputs
        inputs.forEachIndexed { index, input ->
            val inputUri = Uri.parse(input)
            when (inputUri.scheme?.toLowerCase()) {
                ContentResolver.SCHEME_CONTENT -> {
                    // content:// can't be recognized by any ffmpeg protocol
                    val inputCopyTo = makeInputTempFile(jobTempDir!!, index)
                    job = jobManager.updateJobStatus(job, JobStatus.PREPARING, "Copying input $index")
                    try {
                        contentResolver.openInputStream(inputUri).use { inputStream ->
                            val outputStream = contentResolver.openOutputStream(Uri.fromFile(inputCopyTo))
                            BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                                inputStream.copyTo(bufferedOutputStream)
                            }
                        }
                    } catch (anyError: Throwable) {
                        onError(anyError, "Prepare input $index failed: ${anyError.message}")
                        return
                    }
                }
                "file" -> Unit // needn't prepare
                else -> {
                    val message = "Unsupported input scheme ${inputUri.scheme}"
                    onError(Exception(message), message)
                    return
                }
            }
        }

        // prepared -> ready to convert
        job = jobManager.updateJobStatus(job, JobStatus.READY)
        onCompleteListener(job)
    }

    private fun onError(error: Throwable, message: String) {
        val errorDetail = getKnownReasonOf(error, appContext, message)
        job = jobManager.updateJobStatus(job, JobStatus.FAILED, errorDetail)
        onErrorListener(job, error)

        // clean up temp dir
        jobTempDir?.deleteRecursiveIgnoreError()

        reportNonFatal(error, "JobPrepareThread#onError", message)
    }
}