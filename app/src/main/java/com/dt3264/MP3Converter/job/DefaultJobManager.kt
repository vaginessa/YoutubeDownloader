package com.dt3264.MP3Converter.job

import com.dt3264.MP3Converter.annotation.JobStatus
import com.dt3264.MP3Converter.db.JobDb
import com.dt3264.MP3Converter.util.catchAll
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

/**
 * Created by Khang NT on 1/3/18.
 * Email: khang.neon.1997@gmail.com
 */

class DefaultJobManager(private val jobDb: JobDb) : JobManager {
    private val lock = Any()
    private var loadedJobToMemory = false

    private val mapJobList = mapOf(
            JobStatus.PENDING to mutableListOf<Job>(),
            JobStatus.PREPARING to mutableListOf(),
            JobStatus.READY to mutableListOf(),
            JobStatus.RUNNING to mutableListOf(),
            JobStatus.COMPLETED to mutableListOf(),
            JobStatus.FAILED to mutableListOf()
    )
    private var mapSubject = newMapSubject()

    private val liveLogSubject = BehaviorSubject.create<String>()

    override fun recordLiveLog(size: String) {
        liveLogSubject.onNext(size)
    }

    override fun getLiveLogObservable(): Observable<String> = liveLogSubject

    override fun addJob(job: Job): Job {
        loadJobToMemoryIfNeeded()
        synchronized(lock) {
            // execute db insert synchronously
            val newJobId = jobDb.insertJob(job)
            val newJob = job.copy(id = newJobId)
            mapJobList[newJob.status]!!.add(newJob)
            notifyJobListChanged(newJob.status)
            return newJob
        }
    }

    override fun deleteJob(job: Job) {
        loadJobToMemoryIfNeeded()
        synchronized(lock) {
            val jobList = mapJobList[job.status]!!
            if (jobList.remove(job)) {
                notifyJobListChanged(job.status)
                runOnDbThread {
                    catchAll(printLog = true) { jobDb.deleteJob(job) }
                }
            }
        }
    }

    override fun deleteJob(jobId: Long) {
        loadJobToMemoryIfNeeded()
        synchronized(lock) {
            var jobStatus: Int? = null
            mapJobList.values.forEach { jobList ->
                val listIterator = jobList.listIterator()
                while (listIterator.hasNext()) {
                    val next = listIterator.next()
                    if (next.id == jobId) {
                        listIterator.remove()
                        jobStatus = next.status
                    }
                }
            }
            if (jobStatus != null) {
                notifyJobListChanged(jobStatus!!)
                runOnDbThread {
                    catchAll { jobDb.deleteJob(jobId) }
                }
            }
        }
    }

    override fun nextReadyJob(): Job? {
        loadJobToMemoryIfNeeded()
        synchronized(lock) {
            val readyJobs = mapJobList[JobStatus.READY]!!
            if (!readyJobs.isEmpty()) {
                return readyJobs[0]
            }
        }
        return null
    }

    override fun nextPendingJob(): Job? {
        loadJobToMemoryIfNeeded()
        synchronized(lock) {
            val pendingJobs = mapJobList[JobStatus.PENDING]!!
            if (!pendingJobs.isEmpty()) {
                return pendingJobs[0]
            }
        }
        return null
    }

    override fun updateJobStatus(job: Job, status: Int, statusDetail: String?): Job {
        loadJobToMemoryIfNeeded()

        val newJob = job.copy(status = status, statusDetail = statusDetail)
        synchronized(lock) {
            // update memory value first
            mapJobList[job.status]!!.remove(job)
            mapJobList[newJob.status]!!.add(newJob)

            if (job.status != newJob.status) {
                notifyJobListChanged(job.status, newJob.status)
            } else {
                notifyJobListChanged(job.status)
            }
        }

        // update db async
        runOnDbThread {
            catchAll(printLog = true) { jobDb.updateJob(newJob) }
        }
        return newJob
    }

    override fun getJob(vararg jobStatus: Int): Flowable<MutableList<Job>> {
        loadJobToMemoryIfNeeded()

        val subjects = jobStatus.map { mapSubject[it]!!.toFlowable(BackpressureStrategy.LATEST) }
        if (subjects.isEmpty()) {
            return Flowable.empty()
        }
        return Flowable.combineLatest(subjects, { jobListList ->
            val combinedList = mutableListOf<Job>()
            @Suppress("UNCHECKED_CAST")
            jobListList.map { it as List<Job> }.forEach { combinedList.addAll(it) }
            return@combineLatest combinedList
        })
    }

    private fun runOnDbThread(command: () -> Unit) {
        Completable.fromAction(command).subscribeOn(Schedulers.single()).subscribe()
    }

    private fun loadJobToMemoryIfNeeded() {
        synchronized(lock) {
            if (!loadedJobToMemory) {
                try {
                    jobDb.getAllJob().forEach { mapJobList[it.status]!!.add(it) }
                    loadedJobToMemory = true
                    notifyJobListChanged(JobStatus.RUNNING, JobStatus.PENDING, JobStatus.PREPARING, JobStatus.READY,
                            JobStatus.COMPLETED, JobStatus.FAILED)
                } catch (error: Throwable) {
                    loadedJobToMemory = false
                    // notify error
                    mapJobList.keys.forEach { status ->
                        mapJobList[status]!!.clear()
                        mapSubject[status]!!.onError(error)
                    }
                    mapSubject = newMapSubject()
                }
            }
        }
    }

    private fun notifyJobListChanged(@JobStatus vararg status: Int) {
        status.forEach { mapSubject[it]!!.onNext(mapJobList[it]!!) }
    }

    private fun newMapSubject() = mapOf(
            JobStatus.PENDING to BehaviorSubject.create<List<Job>>(),
            JobStatus.PREPARING to BehaviorSubject.create<List<Job>>(),
            JobStatus.READY to BehaviorSubject.create<List<Job>>(),
            JobStatus.RUNNING to BehaviorSubject.create<List<Job>>(),
            JobStatus.COMPLETED to BehaviorSubject.create<List<Job>>(),
            JobStatus.FAILED to BehaviorSubject.create<List<Job>>()
    )

}