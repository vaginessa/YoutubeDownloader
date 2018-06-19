package com.dt3264.MP3Converter.ytDownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class DownloadFinishedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            Bundle extras = intent.getExtras();
            DownloadManager.Query q = new DownloadManager.Query();
            long downloadId = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            q.setFilterById(downloadId);
            Cursor c = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(q);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {

                    String inPath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                    int i=0;
                    for(i=inPath.length()-1; i>=0; i--){
                        if(inPath.charAt(i)=='/'){
                            break;
                        }
                    }
                    i++;
                    String output="";
                    for(; i<inPath.length(); i++){
                        output+=inPath.charAt(i);
                    }

                    try{
                        output = URLDecoder.decode(output, "UTF-8");
                    }
                    catch(UnsupportedEncodingException e){
                        output = output.replace("%20", " ");
                    }
                    if(inPath.contains("m4a")) {
                        //This way the app only converts m4a to mp3 and not m4v
                        //String inuput = Uri.fromFile(new File(inPath)).toString();
                        output = output.replace("m4a", "mp3");
                        InputOutputFile inputOutputFile = new InputOutputFile();
                        inputOutputFile.startConversion(inPath, output, context);
                    }
                    c.close();
                }
            }
            if(!c.isClosed()){
                c.close();
            }

        }
    }

}
