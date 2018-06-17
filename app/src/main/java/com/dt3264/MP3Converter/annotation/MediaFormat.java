package com.dt3264.MP3Converter.annotation;

import android.support.annotation.StringDef;

import static com.dt3264.MP3Converter.annotation.MediaFormat.M4A;
import static com.dt3264.MP3Converter.annotation.MediaFormat.MP3;

/**
 * Created by Khang NT on 1/2/18.
 * Email: khang.neon.1997@gmail.com
 */

@StringDef({MP3, M4A})
public @interface MediaFormat {
    String MP3 = "mp3";
    String M4A = "m4a";
}
