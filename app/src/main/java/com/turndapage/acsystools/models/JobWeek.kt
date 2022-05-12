package com.turndapage.acsystools.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.time.DayOfWeek
import java.time.temporal.ChronoField
import kotlin.collections.ArrayList

class JobWeek(val date: LocalDate, var project: Project?, var taskCode: TaskCode?, jobs: ArrayList<Job>) {
    constructor() : this(LocalDate.now().with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.value.toLong()), null,null, ArrayList())

    var jobs = ArrayList<Job>()
    init {
        this.jobs = ArrayList(jobs.filter { it.project == project && it.taskCode == taskCode })
    }

    fun getJob(dayOfWeek: DayOfWeek): Job? {
        return jobs.firstOrNull { it.date.dayOfWeek == dayOfWeek }
    }
}