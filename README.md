# Youtube Downloader 

## Build app 

##### 
To build the app, you need to create [Fabric](https://fabric.io) account, then place your Fabric's api
key in `app/fabric.properties` file:


```
apiKey=your_api_key_here
```

Also change `signingConfigs` in `app/build.gradle` file with your own key store if you want build signed release 
apks.

## FFmpeg license
This software uses code of <a href=http://ffmpeg.org>FFmpeg</a> licensed under the <a href=http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>LGPLv2.1</a>.
FFmpeg binary files are prebuilt, you can download source and build script from this repo [ffmpeg-binary-android](https://github.com/Khang-NT/ffmpeg-binary-android).


