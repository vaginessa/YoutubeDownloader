package com.dt3264.MP3Converter

import android.app.Application
import android.os.StrictMode
import com.dt3264.MP3Converter.util.reportNonFatal
import com.singhajit.sherlock.core.Sherlock
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
/**
 * Created by Khang NT on 1/2/18.
 * Email: khang.neon.1997@gmail.com
 */

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        //setupStrictMode()

        SingletonInstances.init(this)
        Sherlock.init(this);

        setUpRxPlugins()
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .permitDiskReads()
                    .permitDiskWrites()
                    .penaltyDialog()
                    .penaltyLog()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build())
        } else {
            // on release, don't detect any thing
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().penaltyLog().build())
        }
    }

    private fun setUpRxPlugins() {
        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException) {
                reportNonFatal(throwable.cause!!, "rx_undeliverable_exception")
            } else {
                reportNonFatal(throwable, "rx_undeliverable_exception")
            }
        }
    }
}