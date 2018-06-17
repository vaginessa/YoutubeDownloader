package com.dt3264.MP3Converter.worker

import com.dt3264.MP3Converter.DEFAULT_IO_BUFFER_LENGTH
import com.dt3264.MP3Converter.util.closeQuietly
import com.dt3264.MP3Converter.util.copy
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by Khang NT on 1/1/18.
 * Email: khang.neon.1997@gmail.com
 */

class CopierThread(
        private val sourceInput: SourceInputStream,
        private val sourceOutput: SourceOutputStream,
        private val bufferLength: Int = DEFAULT_IO_BUFFER_LENGTH,
        private val onError: (Throwable) -> Unit,
        private val onSuccess: () -> Unit = {}
) : Thread() {

    override fun run() {
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            input = sourceInput.openInputStream()
            output = sourceOutput.openOutputStream()
            copy(input, output, bufferLength)
            onSuccess.invoke()
        } catch (anyError: Throwable) {
            onError(anyError)
        } finally {
            // close sources
            input.closeQuietly()
            output.closeQuietly()
            sourceInput.closeQuietly()
            sourceOutput.closeQuietly()
        }
    }

}