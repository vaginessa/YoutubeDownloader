package com.dt3264.MP3Converter.util

import android.content.Context
import com.dt3264.MP3Converter.BuildConfig
import com.dt3264.MP3Converter.R
import java.io.EOFException
import java.io.InterruptedIOException
import java.lang.Exception
import java.net.HttpRetryException
import java.net.ProtocolException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Created by Khang NT on 1/13/18.
 * Email: khang.neon.1997@gmail.com
 */

fun <T : Throwable> rootCauseIs(clazz: Class<T>, error: Throwable): Boolean {
    var temp: Throwable? = error
    while (temp !== null) {
        if (clazz.isInstance(temp)) {
            return true
        }
        temp = temp.cause
    }
    return false
}

private fun inWhiteList(error: Throwable): Boolean =
        error.javaClass == Exception::javaClass ||  // dumb error
                rootCauseIs(InterruptedException::class.java, error) ||
                rootCauseIs(InterruptedIOException::class.java, error) ||
                rootCauseIs(UnknownHostException::class.java, error) ||
                rootCauseIs(SSLException::class.java, error) ||
                rootCauseIs(SocketException::class.java, error) ||
                rootCauseIs(EOFException::class.java, error) ||
                rootCauseIs(HttpRetryException::class.java, error) ||
                rootCauseIs(ProtocolException::class.java, error) ||
                error.message?.contains("ENOSPC") == true// No space left on device)


fun reportNonFatal(throwable: Throwable, where: String, message: String? = null) {
    if (!BuildConfig.DEBUG && !inWhiteList(throwable)) {
    }
}

fun getKnownReasonOf(error: Throwable, context: Context, fallback: String): String {
    if (rootCauseIs(UnknownHostException::class.java, error) ||
            rootCauseIs(SSLException::class.java, error) ||
            rootCauseIs(SocketException::class.java, error) ||
            error.message?.contains("unexpected end of stream", ignoreCase = true) == true ||
            rootCauseIs(ProtocolException::class.java, error) ||
            rootCauseIs(HttpRetryException::class.java, error)) {
        return context.getString(R.string.network_error)
    }
    return fallback
}