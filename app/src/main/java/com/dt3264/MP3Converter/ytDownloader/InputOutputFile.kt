package com.dt3264.MP3Converter.ytDownloader

import android.content.Context
import android.net.Uri
import android.support.v4.provider.DocumentFile
import android.widget.Toast
import com.dt3264.MP3Converter.SingletonInstances
import com.dt3264.MP3Converter.util.checkFileExists
import com.dt3264.MP3Converter.worker.ConverterService
import java.io.File



data class InputOutputData(val title: String, val inputUri: String, val outputUri: String)

class InputOutputFile{

    fun validateAndGetInputOutputData(inputUri: String, fileName: String, context: Context): InputOutputData {
        val sharedPrefs = SingletonInstances.getSharedPrefs()
        val outputFolderUri = sharedPrefs.lastOutputFolderUri?.let { Uri.parse(it) }
        val existingFile = context.checkFileExists(outputFolderUri!!, fileName)?.toString()
        if (existingFile != null) {
            return InputOutputData(fileName, inputUri, existingFile)
        }
        val outputUri: Uri = when {
            outputFolderUri.scheme == "file" -> Uri.fromFile(File(outputFolderUri.path, fileName))
            else -> {
                try {
                    val documentTree = DocumentFile.fromTreeUri(context, outputFolderUri)
                    documentTree.createFile(null, fileName).uri
                } catch (error: Throwable) {
                    Toast.makeText(context, "Error ubicando carpeta de descargas", Toast.LENGTH_SHORT).show()
                    return InputOutputData("", "", "")
                }
            }
        }
        return InputOutputData(fileName, inputUri, outputUri.toString())
    }

    fun startConversion(input:String, output:String, context: Context) {
        val inputOutputData = validateAndGetInputOutputData(input, output, context)
        val cmdArgsBuilder = StringBuffer("-hide_banner -map 0:a -map_metadata 0:g -codec:a libmp3lame -q:a 6 ")
        ConverterService.newJob(
                context,
                title = inputOutputData.title,
                inputs = listOf(inputOutputData.inputUri),
                args = cmdArgsBuilder.toString(),
                outputUri = inputOutputData.outputUri,
                outputFormat = "mp3"
        )
    }

}
