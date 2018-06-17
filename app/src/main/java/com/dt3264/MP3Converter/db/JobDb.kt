package com.dt3264.MP3Converter.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dt3264.MP3Converter.ID_UNSET
import com.dt3264.MP3Converter.db.JobTable.Companion.C_COMMAND
import com.dt3264.MP3Converter.db.JobTable.Companion.C_ID
import com.dt3264.MP3Converter.db.JobTable.Companion.C_STATUS
import com.dt3264.MP3Converter.db.JobTable.Companion.C_STATUS_DETAIL
import com.dt3264.MP3Converter.db.JobTable.Companion.C_TITLE
import com.dt3264.MP3Converter.job.Command
import com.dt3264.MP3Converter.job.Job
import org.json.JSONObject

/**
 * Created by Khang NT on 1/2/18.
 * Email: khang.neon.1997@gmail.com
 */

class JobDb(private val sqLiteOpenHelper: SQLiteOpenHelper) {

    private val lock = Any()

    fun getAllJob(): List<Job> {
        synchronized(lock) {
            sqLiteOpenHelper.readableDatabase.use { db ->
                db.query(JobTable.NAME, null, null, null, null,
                        null, null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colId = cursor.getColumnIndex(C_ID)
                        val colTitle = cursor.getColumnIndex(C_TITLE)
                        val colStatus = cursor.getColumnIndex(C_STATUS)
                        val colStatusDetail = cursor.getColumnIndex(C_STATUS_DETAIL)
                        val colCommand = cursor.getColumnIndex(C_COMMAND)

                        val result = mutableListOf<Job>()
                        do {
                            result.add(Job(cursor.getLong(colId), cursor.getString(colTitle),
                                    cursor.getInt(colStatus), cursor.getString(colStatusDetail),
                                    Command.from(JSONObject(cursor.getString(colCommand)))))
                        } while (cursor.moveToNext())
                        return result
                    }
                }
                return emptyList()
            }
        }
    }

    private fun createContentValues(job: Job): ContentValues {
        val contentValues = ContentValues()
        if (job.id != ID_UNSET) {
            contentValues.put(C_ID, job.id)
        }
        contentValues.put(C_TITLE, job.title)
        contentValues.put(C_STATUS, job.status)
        contentValues.put(C_STATUS_DETAIL, job.statusDetail)
        contentValues.put(C_COMMAND, job.command.toJson().toString())
        return contentValues
    }

    fun updateJob(job: Job) {
        if (job.id == ID_UNSET) {
            throw IllegalArgumentException("Job ID must be set")
        }
        synchronized(lock) {
            sqLiteOpenHelper.writableDatabase.use { db ->
                db.updateWithOnConflict(JobTable.NAME, createContentValues(job),
                        "$C_ID == ?", arrayOf(job.id.toString()),
                        SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    fun insertJob(job: Job): Long {
        synchronized(lock) {
            sqLiteOpenHelper.writableDatabase.use { db ->
                return db.insert(JobTable.NAME, null, createContentValues(job))
            }
        }
    }

    fun deleteJob(job: Job): Int = deleteJob(job.id)

    fun deleteJob(id: Long): Int {
        synchronized(lock) {
            sqLiteOpenHelper.writableDatabase.use { db ->
                return db.delete(JobTable.NAME, "$C_ID == ?", arrayOf(id.toString()))
            }
        }
    }

}