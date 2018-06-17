package com.dt3264.MP3Converter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dt3264.MP3Converter.util.appPermissions
import com.dt3264.MP3Converter.util.hasWriteStoragePermission

class Main2Activity : AppCompatActivity() {

    private var outputFolderUri: Uri? = null
    private val sharedPrefs = SingletonInstances.getSharedPrefs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        if (!hasWriteStoragePermission(this)) {
            // request permission without checking result
            requestPermissions(appPermissions, 0)
        }

        val sharedPrefs = SingletonInstances.getSharedPrefs()
        if (sharedPrefs.lastKnownVersionCode < BuildConfig.VERSION_CODE) {
            sharedPrefs.lastKnownVersionCode = BuildConfig.VERSION_CODE
        }

        if(sharedPrefs.lastOutputFolderUri.isNullOrEmpty()) {
            Toast.makeText(this, "Select the folder to download the music", Toast.LENGTH_SHORT).show()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra("android.content.extra.SHOW_ADVANCED", outputFolderUri === null)
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            100 -> {
                // >= LOLLIPOP only
                val uri = data!!.data
                val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (data.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) {
                    this!!.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    sharedPrefs.lastOutputFolderUri = uri.toString()
                }
                outputFolderUri = uri
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finish()
    }
}
