package com.turndapage.acsystools.models

import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletionService

class Job(val id: Int, var project: Project, var taskCode: TaskCode, var hours: Double, var date: LocalDate, cost: Double) {
}