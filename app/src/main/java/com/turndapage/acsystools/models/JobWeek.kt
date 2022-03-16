package com.turndapage.acsystools.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.time.DayOfWeek
import kotlin.collections.ArrayList

class JobWeek(val date: LocalDate, var project: Project?, var taskCode: TaskCode?, jobs: ArrayList<Job>) {
    constructor() : this(LocalDate.now(), null,null, ArrayList())

    var jobs = ArrayList<Job>()
    init {
        this.jobs = ArrayList(jobs.filter { it.project == project && it.taskCode == taskCode })
    }

    fun getJob(dayOfWeek: DayOfWeek): Job? {
        return jobs.firstOrNull { it.date.dayOfWeek == dayOfWeek }
    }
}