package com.dt3264.MP3Converter

import android.content.Context
import android.support.constraint.solver.Cache
import com.dt3264.MP3Converter.db.JobDb
import com.dt3264.MP3Converter.db.MainSqliteOpenHelper
import com.dt3264.MP3Converter.job.DefaultJobManager
import com.dt3264.MP3Converter.job.JobManager
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by Khang NT on 1/2/18.
 * Email: khang.neon.1997@gmail.com
 */

class SingletonInstances private constructor(appContext: Context) {
    companion object {
        private const val CACHE_SIZE = 20 * 1024 * 1024L     //20MB

        private lateinit var INSTANCE: SingletonInstances
        private var initialized = false

        @JvmStatic
        fun init(context: Context) {
                //check(!initialized, { "Only init once" })
                INSTANCE = SingletonInstances(context.applicationContext)
                initialized = true
        }

        fun isInitialized() = initialized


        fun getJobManager(): JobManager = INSTANCE.jobManagerLazy
        @JvmStatic
        fun getSharedPrefs(): SharedPrefs = INSTANCE.sharedPrefsLazy

    }



    private val mainSqliteOpenHelperLazy by lazy { MainSqliteOpenHelper(appContext) }

    private val jobDatabaseLazy by lazy { JobDb(mainSqliteOpenHelperLazy) }

    private val jobManagerLazy by lazy { DefaultJobManager(jobDatabaseLazy) }

    private val sharedPrefsLazy by lazy { SharedPrefs(appContext) }
}