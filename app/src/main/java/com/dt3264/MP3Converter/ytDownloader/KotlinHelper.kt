package com.dt3264.MP3Converter.ytDownloader

import android.content.Intent
import com.dt3264.MP3Converter.SingletonInstances

class KotlinHelper {
    companion object {
        @JvmStatic
        fun downloadFolderExist(): Boolean {
            val sharedPrefs = SingletonInstances.getSharedPrefs()
            return sharedPrefs.lastOutputFolderUri.isNullOrEmpty()
        }
        @JvmStatic
        fun updateDownloadFolder(uri: String){
            val sharedPrefs = SingletonInstances.getSharedPrefs()
            sharedPrefs.lastOutputFolderUri = uri
        }
        @JvmStatic
        fun checkTrue(data: Intent):Boolean{
            return data.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
    }
}