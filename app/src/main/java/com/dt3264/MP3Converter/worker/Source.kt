package com.dt3264.MP3Converter.worker

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dt3264.MP3Converter.DEFAULT_CONNECTION_TIMEOUT
import com.dt3264.MP3Converter.util.closeQuietly
import java.io.*
import java.net.ServerSocket

/**
 * Created by Khang NT on 1/1/18.
 * Email: khang.neon.1997@gmail.com
 */

object Sources {
    fun from(inputStream: InputStream): SourceInputStream {
        return object : SourceInputStream {
            override fun openInputStream(): InputStream = inputStream

            override fun close() {
                inputStream.closeQuietly()
            }
        }
    }
}

interface SourceInputStream : Closeable {
    fun openInputStream(): InputStream
}

interface SourceOutputStream : Closeable {
    fun openOutputStream(): OutputStream
}

class ContentResolverSource(
        context: Context,
        private val uri: Uri
) : SourceInputStream, SourceOutputStream {

    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    init {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT
                && uri.scheme != ContentResolver.SCHEME_FILE) {
            throw IllegalArgumentException("Only accept scheme '${ContentResolver.SCHEME_CONTENT}'" +
                    "and '${ContentResolver.SCHEME_FILE}' but found ${uri.scheme}")
        }
    }

    override fun openInputStream(): InputStream = BufferedInputStream(contentResolver.openInputStream(uri))

    override fun openOutputStream(): OutputStream = BufferedOutputStream(contentResolver.openOutputStream(uri))

    override fun close() {
        // does nothing
    }

}

class ServerSocketSourceOutput(
        private val serverSocket: ServerSocket,
        timeout: Int = DEFAULT_CONNECTION_TIMEOUT
) : SourceOutputStream {

    private var acceptedConnection = false

    init {
        serverSocket.soTimeout = timeout
    }

    private val outputStream: OutputStream by lazy {
        acceptedConnection = true
        val socket = serverSocket.accept()
        object : BufferedOutputStream(socket.getOutputStream()) {
            override fun close() {
                socket.closeQuietly()
                super.close()
            }
        }
    }

    override fun openOutputStream(): OutputStream = outputStream

    override fun close() {
        if (acceptedConnection) {
            outputStream.closeQuietly()
        }
        serverSocket.closeQuietly()
    }
}