package com.turndapage.acsystools

import android.content.Context
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView.OnEditorActionListener
import androidx.recyclerview.widget.RecyclerView
import com.turndapage.acsystools.models.Job
import com.turndapage.acsystools.models.JobWeek
import com.turndapage.acsystools.models.Project
import com.turndapage.acsystools.models.TaskCode
import com.turndapage.acsystools.ui.login.afterTextChanged
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList

@DelicateCoroutinesApi
class JobWeekAdapter(
    context: Context,
    var jobWeeks: ArrayList<JobWeek>,
    private val projects: ArrayList<Project>,
    private val tasks: ArrayList<TaskCode>) :
    RecyclerView.Adapter<JobWeekAdapter.ViewHolder>() {

    companion object {
        lateinit var projectAdapter: ArrayAdapter<Project>
        lateinit var taskAdapter: ArrayAdapter<TaskCode>
    }

    init {
        projectAdapter = ArrayAdapter(context,android.R.layout.simple_spinner_item, projects)
        taskAdapter =  ArrayAdapter(context,android.R.layout.simple_spinner_item,tasks)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val spinnerProject: Spinner = view.findViewById(R.id.spinner_project)
        val spinnerTask: Spinner = view.findViewById(R.id.spinner_task)

        val hourEditTexts = listOf<EditText>(
            view.findViewById(R.id.mondayEditText),
            view.findViewById(R.id.tuesdayEditText),
            view.findViewById(R.id.wednesdayEditText),
            view.findViewById(R.id.thursdayEditText),
            view.findViewById(R.id.fridayEditText),
            view.findViewById(R.id.saturdayEditText),
            view.findViewById(R.id.sundayEditText))
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.week_row_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val jobWeek = jobWeeks[position]

        viewHolder.spinnerProject.adapter = projectAdapter
        viewHolder.spinnerProject.setSelection(projects.indexOf(jobWeek.project))
        viewHolder.spinnerProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position > 0) {
                    jobWeek.project = projects[position]
                    // both are selected so check for existing hours and change type
                    for(dayOfWeek in DayOfWeek.values()) {
                        val job = jobWeek.getJob(dayOfWeek)
                        if(job != null) {
                            job.project = projects[position]
                            updateJob(job)
                        }
                    }
                } else
                    jobWeek.project = null
                updateEnabled(viewHolder,jobWeek)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateEnabled(viewHolder,jobWeek)
            }
        }
        viewHolder.spinnerTask.adapter = taskAdapter
        viewHolder.spinnerTask.setSelection(tasks.indexOf(jobWeek.taskCode))
        viewHolder.spinnerTask.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position > 0) {
                    jobWeek.taskCode = tasks[position]
                    // both are selected
                    if(jobWeek.jobs.size > 0) {
                        // check for existing hours and change type
                        for (dayOfWeek in DayOfWeek.values()) {
                            val job = jobWeek.getJob(dayOfWeek)
                            if (job != null) {
                                job.taskCode = tasks[position]
                                updateJob(job)
                            }
                        }
                    }
                } else
                    jobWeek.taskCode = null
                updateEnabled(viewHolder,jobWeek)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateEnabled(viewHolder,jobWeek)
            }
        }

        updateEnabled(viewHolder,jobWeek)

        for(dayOfWeek in DayOfWeek.values()) {
            val index = dayOfWeek.value - 1
            val editText = viewHolder.hourEditTexts[index]
            val currentHours = jobWeek.getJob(dayOfWeek)?.hours
            if(currentHours == null)
                editText.setText("")
            else
                editText.setText(currentHours.toString())

            editText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val job = jobWeek.getJob(dayOfWeek)
                    val text = v.text.toString()
                    var delete = text == ""
                    val hours = try {
                        text.toDouble()
                    } catch (ex: NumberFormatException) {
                        delete = true
                        0.0
                    }
                    if (delete && job != null) {
                        deleteJob(job)
                        jobWeek.jobs.remove(job)
                    }
                    else {
                        if (job != null) {
                            job.hours = hours
                            updateJob(job)
                        } else {
                            insertJob(
                                jobWeek,
                                hours,
                                LocalDate.from(jobWeek.date).plusDays(index.toLong() - 1)
                            )
                            if (jobWeeks.last() == jobWeek) {
                                // if this is the last one we need to add another placeholder job week
                                jobWeeks.add(JobWeek())
                            }
                        }
                    }
                }
                false
            }
        }
    }

    private fun updateJob(job: Job) {
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            MainActivity.connection.updateJob(job)
        }
    }

    private fun insertJob(jobWeek: JobWeek, hours: Double, date: LocalDate) {
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val project = jobWeek.project
            val taskCode = jobWeek.taskCode
            if(project != null && taskCode != null && hours > 0.0) {
                val job = MainActivity.connection.insertJob(project, taskCode, hours, date)
                if(job != null)
                    jobWeek.jobs.add(job)
            }
        }
    }

    private fun deleteJob(job: Job) {
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            MainActivity.connection.deleteJob(job)
        }
    }

    private fun updateEnabled(viewHolder: ViewHolder, jobWeek: JobWeek) {
        for(editBox in viewHolder.hourEditTexts)
            editBox.isEnabled = !(jobWeek.project == null || jobWeek.taskCode == null)
    }

    override fun getItemCount(): Int {
        return jobWeeks.size
    }
}