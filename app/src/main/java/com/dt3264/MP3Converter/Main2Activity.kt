package com.dt3264.MP3Converter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.dt3264.MP3Converter.util.appPermissions
import com.dt3264.MP3Converter.util.hasWriteStoragePermission
import org.w3c.dom.Text

class Main2Activity : AppCompatActivity() {

    private var outputFolderUri: Uri? = null
    private val sharedPrefs = SingletonInstances.getSharedPrefs()
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        if (!hasWriteStoragePermission(this)) {
            // request permission without checking result
            requestPermissions(appPermissions, 0)
        }
        else {
            if (sharedPrefs.lastOutputFolderUri.isNullOrEmpty()) {
                selectNewDownloadFolder()
            }
        }
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                selectNewDownloadFolder()
            }
        })
        textView = findViewById(R.id.downloadPathText) as TextView
        val uri = Uri.parse(sharedPrefs.lastOutputFolderUri)
        var text = getString(R.string.pathOfDownloads) + " \n" + (uri.path.toString()
                .replace("tree", "storage")
                .replace(":", "/"))
        if(!text.endsWith('/')) text+="/"
        textView!!.text = text

        val sharedPrefs = SingletonInstances.getSharedPrefs()
        if (sharedPrefs.lastKnownVersionCode < BuildConfig.VERSION_CODE) {
            sharedPrefs.lastKnownVersionCode = BuildConfig.VERSION_CODE
        }


    }

    fun selectNewDownloadFolder(){
        Toast.makeText(this, "Select the folder to download the music", Toast.LENGTH_SHORT).show()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra("android.content.extra.SHOW_ADVANCED", outputFolderUri === null)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
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
                    if(intent.getBooleanExtra("fromShare", false)) {
                        finish()
                    }
                    var text = getString(R.string.pathOfDownloads) + " \n" + (uri.path.toString()
                            .replace("tree", "storage")
                            .replace(":", "/"))
                    if(!text.endsWith('/')) text+="/"
                    textView.text = text
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(sharedPrefs.lastOutputFolderUri.isNullOrEmpty()) {
            selectNewDownloadFolder()
        }
    }
}
