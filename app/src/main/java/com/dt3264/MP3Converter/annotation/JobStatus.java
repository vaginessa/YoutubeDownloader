package com.dt3264.MP3Converter.annotation;

import android.support.annotation.IntDef;

import static com.dt3264.MP3Converter.annotation.JobStatus.COMPLETED;
import static com.dt3264.MP3Converter.annotation.JobStatus.FAILED;
import static com.dt3264.MP3Converter.annotation.JobStatus.PENDING;
import static com.dt3264.MP3Converter.annotation.JobStatus.PREPARING;
import static com.dt3264.MP3Converter.annotation.JobStatus.READY;
import static com.dt3264.MP3Converter.annotation.JobStatus.RUNNING;

/**
 * Created by Khang NT on 1/2/18.
 * Email: khang.neon.1997@gmail.com
 */

@IntDef({PENDING, PREPARING, READY, RUNNING, COMPLETED, FAILED})
public @interface JobStatus {
    int RUNNING = 0;
    int PENDING = 1;
    int COMPLETED = 2;
    int FAILED = 3;

    // new
    int PREPARING = 4;
    int READY = 5;
}
