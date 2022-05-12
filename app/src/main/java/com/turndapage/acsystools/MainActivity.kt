package com.turndapage.acsystools

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.StackView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.turndapage.acsystools.database.Connection
import com.turndapage.acsystools.fragments.DatePickerFragment
import com.turndapage.acsystools.models.JobWeek
import com.turndapage.acsystools.models.Project
import com.turndapage.acsystools.models.TaskCode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Math.log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import kotlin.collections.ArrayList


@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    companion object {
        var now: LocalDate = LocalDate.now()
        var projects = ArrayList<Project>()
        var tasks = ArrayList<TaskCode>()
        var jobWeeks = ArrayList<JobWeek>()
        lateinit var connection: Connection
        lateinit var recyclerView: RecyclerView
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    }

    lateinit var jobWeekAdapter: JobWeekAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        val view = findViewById<ViewGroup>(R.id.main_layout)
        val header = findViewById<LinearLayout>(R.id.header)

        ViewCompat.setOnApplyWindowInsetsListener(view) { main, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. Here the system is setting
            // only the bottom, left, and right dimensions, but apply whichever insets are
            // appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            header.setPadding(0,insets.top,0,0)
            /*main.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = 0
            }*/
            main.setPadding(insets.left,0,insets.right,insets.bottom)

            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(R.color.secondary_600)))

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = FullScrollLayoutManager(this, findViewById(R.id.header_scroll))

        val dateButton = findViewById<Button>(R.id.dateButton)
        dateButton.setOnClickListener {
            showDatePickerDialog()
        }
        dateButton.text = dateFormatter.format(now)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            now = now.minusWeeks(1)
            loadJobs()
        }
        findViewById<ImageButton>(R.id.forwardButton).setOnClickListener {
            now = now.plusWeeks(1)
            loadJobs()
        }

        connection = Connection(object : Connection.ConnectionListener {
            override fun onConnected() {
                Log.d("debug: ", "Connected")
                GlobalScope.launch(Dispatchers.IO) {
                    val policy = ThreadPolicy.Builder().permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                    if (connection.login("jpage@acsysengineering.com", "ruffsbone")) {
                        Log.d("debug: ", "logged in")
                        projects = connection.getProjects()
                        tasks = connection.getTaskCodes()
                        Log.d("debug", "Got projects and task codes")

                        jobWeeks = connection.getJobWeeks(now, projects, tasks)

                        launch(Dispatchers.Main) {
                            jobWeekAdapter =
                                JobWeekAdapter(recyclerView.context, jobWeeks, projects, tasks)
                            recyclerView.adapter = jobWeekAdapter
                        }
                    }
                }.start()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadJobs() {
        GlobalScope.launch(Dispatchers.IO) {
            jobWeeks = connection.getJobWeeks(now,projects,tasks)
            launch(Dispatchers.Main) {
                if(::jobWeekAdapter.isInitialized) {
                    jobWeekAdapter.jobWeeks = jobWeeks
                    Log.d("debug", "Found ${jobWeekAdapter.jobWeeks.size} jobs for list")
                    jobWeekAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showDatePickerDialog() {
        val newFragment = DatePickerFragment()
        newFragment.show(supportFragmentManager, "datePicker")
    }
}