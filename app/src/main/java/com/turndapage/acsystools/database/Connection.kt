package com.turndapage.acsystools.database
import android.R.attr.port
import android.os.StrictMode
import android.util.Log
import com.turndapage.acsystools.models.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.sql.*
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*


class Connection(val connectionListener: ConnectionListener) {
    private val localIP = "192.168.50.10" // your database server ip
    private val remoteIP = "98.187.121.131" // your database server ip
    private val db = "acsys_tools" // your database name
    private val username = "Admin" // your database username
    private val password = "B34nc4t?" // your database password

    companion object {
        // Projects
        const val TABLE_PROJECT = "acsys_tools.projects"
        const val PROJECT_ID = "project_id"
        const val PROJECT_CODE = "project_code"
        const val PROJECT_NAME = "project_name"
        const val PROJECT_ACTIVE = "project_active"

        // Task Codes
        const val TABLE_TASK_CODE = "acsys_tools.task_codes";
        const val TASK_CODE_ID = "task_code_id";
        const val TASK_CODE_NAME = "task_code_name";

        // Jobs
        const val TABLE_JOB = "jobs_test";
        const val JOB_ID = "job_id";
        const val JOB_USER = "job_user";
        const val JOB_PROJECT = "job_project";
        const val JOB_TASK_CODE = "job_task_code";
        const val JOB_HOURS = "job_hours";
        const val JOB_DATE = "job_date";
        const val JOB_COST = "job_cost";

        // Users Table
        const val TABLE_USER = "acsys_tools.users";
        const val USER_ID = "user_id";
        const val USER_FIRST_NAME = "user_first_name";
        const val USER_LAST_NAME = "user_last_name";
        const val USER_ADMIN = "user_admin";
        const val USER_EMAIL = "user_email";
        const val USER_PASSWORD = "user_password";
        const val USER_HOURLY_RATE = "user_hourly_rate";
        const val USER_ARCHIVED = "user_archived";
        const val USER_VACATION_RATE = "user_vacation_rate";

        lateinit var CurrentUser: User

        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private var connection: Connection? = null

    init {
        checkForAddress(localIP, object: PingListener {
            override fun onResponse() {
                connect(localIP)
            }

            override fun onTimeout() {
                // handle timeout
            }
        })
        checkForAddress(remoteIP, object: PingListener {
            override fun onResponse() {
                connect(remoteIP)
            }

            override fun onTimeout() {
                // handle timeout
            }
        })
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun connect(ip: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val dbURL = "jdbc:jtds:sqlserver://$ip:1433/$db"
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connection = DriverManager.getConnection(dbURL, username, password)
            connectionListener.onConnected()
        }.start()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkForAddress(ip: String, pingListener: PingListener) {
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            try {
                val socketAddress: SocketAddress = InetSocketAddress(ip, 1433)
                // Create an unbound socket
                val sock = Socket()

                // This method will block no more than timeoutMs.
                // If the timeout occurs, SocketTimeoutException is thrown.
                val timeoutMs = 5000 // 5 seconds
                sock.connect(socketAddress, timeoutMs)
                pingListener.onResponse()
            } catch (e: IOException) {
                // Handle exception
                pingListener.onTimeout()
            }
        }
    }

    interface ConnectionListener {
        fun onConnected()
    }

    interface PingListener {
        fun onResponse()
        fun onTimeout()
    }

    fun getProjects() : ArrayList<Project> {
        val projects: ArrayList<Project> = arrayListOf(Project(-1,-1,"Project"))
        var statement: Statement? = null
        var resultSet: ResultSet? = null

        try {
            statement = connection?.createStatement()
            resultSet = statement?.executeQuery("SELECT $PROJECT_ID,$PROJECT_CODE,$PROJECT_NAME,$PROJECT_ACTIVE FROM $TABLE_PROJECT WHERE $PROJECT_ACTIVE=1")
            while (resultSet?.next() == true) {
                projects.add(
                    Project(
                        resultSet.getInt(PROJECT_ID),
                        resultSet.getInt(PROJECT_CODE),
                        resultSet.getString(PROJECT_NAME)))
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                resultSet?.close()
            } catch (sqlEx: SQLException) {
            }
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }

        return projects
    }

    fun getTaskCodes() : ArrayList<TaskCode> {
        val taskCodes: ArrayList<TaskCode> = arrayListOf(TaskCode(-1,"Task Code"))
        var statement: Statement? = null
        var resultSet: ResultSet? = null

        try {
            statement = connection?.createStatement()
            resultSet = statement?.executeQuery("SELECT $TASK_CODE_ID,$TASK_CODE_NAME FROM $TABLE_TASK_CODE")
            while (resultSet?.next() == true) {
                taskCodes.add(
                    TaskCode(
                        resultSet.getInt(TASK_CODE_ID),
                        resultSet.getString(TASK_CODE_NAME))
                )
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                resultSet?.close()
            } catch (sqlEx: SQLException) {
            }
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }

        return taskCodes
    }

    private fun convertToLocalDateViaInstant(dateToConvert: Date): LocalDate {
        return dateToConvert.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun getJobWeeks(date: LocalDate, projects: ArrayList<Project>, tasksCodes: ArrayList<TaskCode>) : ArrayList<JobWeek> {
        val fieldUS = WeekFields.of(Locale.US).dayOfWeek()
        val firstDayOfWeek = date.with(fieldUS,1)
        val jobWeeks: ArrayList<JobWeek> = ArrayList()
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        val dateString = dateFormatter.format(firstDayOfWeek)

        try {
            statement = connection?.createStatement()
            resultSet = statement?.executeQuery(
                "SELECT $JOB_ID," +
                        "$JOB_PROJECT," +
                        "$JOB_TASK_CODE," +
                        "$JOB_HOURS," +
                        "$JOB_DATE," +
                        "$JOB_COST " +
                        "FROM $TABLE_JOB " +
                        "WHERE $JOB_USER = ${CurrentUser.id} " +
                        "AND $JOB_DATE BETWEEN '$dateString' AND DATEADD(day,6,'$dateString')")

            Log.d("Debug", "Added jobs for date $dateString")
            // get all the jobs
            val jobs = ArrayList<Job>()
            while (resultSet?.next() == true) {
                val projectId = resultSet.getInt(JOB_PROJECT)
                val taskCodeId = resultSet.getInt(JOB_TASK_CODE)

                jobs.add(Job(resultSet.getInt(JOB_ID),
                    projects.firstOrNull { it.id == projectId } ?: Project(projectId,0,""),
                    tasksCodes.first { it.id == taskCodeId },
                    resultSet.getDouble(JOB_HOURS),
                    convertToLocalDateViaInstant(resultSet.getDate(JOB_DATE)),
                    resultSet.getDouble(JOB_COST)
                ))
            }

            Log.d("Debug", "Loaded ${jobs.count()} jobs")
            // get unique pairs of projects and tasks
            val pairs = mutableMapOf<Project, TaskCode>()
            for(job in jobs) {
                pairs.putIfAbsent(job.project,job.taskCode)
            }

            Log.d("Debug", "Found ${pairs.count()} pairs")

            // create a job week for every pair
            for(pair in pairs) {
                val weekJobs = ArrayList(jobs.filter { it.project == pair.key && it.taskCode == pair.value })
                jobWeeks.add(JobWeek(date,pair.key,pair.value,weekJobs))
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                resultSet?.close()
            } catch (sqlEx: SQLException) {
            }
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }
        // add a blank job week for new entries
        jobWeeks.add(JobWeek())

        Log.d("Debug", "Loaded ${jobWeeks.count()} job weeks")

        return jobWeeks
    }

    fun insertJob(project: Project, taskCode: TaskCode, hours: Double, date: LocalDate): Job? {
        var job: Job? = null
        var statement: Statement? = null

        val cost = hours * CurrentUser.hourlyRate

        try {
            statement = connection?.prepareStatement("INSERT INTO $TABLE_JOB (" +
                    "$JOB_USER," +
                    "$JOB_PROJECT," +
                    "$JOB_TASK_CODE," +
                    "$JOB_HOURS," +
                    "$JOB_DATE," +
                    "$JOB_COST) " +
                    "VALUES (" +
                    "${CurrentUser.id}," +
                    "${project.id}," +
                    "${taskCode.id}," +
                    "$hours," +
                    "'${dateFormatter.format(date.plusDays(-1))}'," +
                    "$cost)", Statement.RETURN_GENERATED_KEYS)

            statement?.executeUpdate()
            val keys = statement?.generatedKeys
            if(keys?.next() == true) {
                job = Job(keys.getInt(1),project,taskCode,hours,date,cost)
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }
        return job
    }

    fun updateJob(job: Job) {
        var statement: Statement? = null

        try {
            statement = connection?.createStatement()
            statement?.executeUpdate("UPDATE $TABLE_JOB " +
                    "SET $JOB_PROJECT = ${job.project.id}," +
                    "$JOB_TASK_CODE = ${job.taskCode.id}," +
                    "$JOB_HOURS = ${job.hours}," +
                    "$JOB_COST = ${job.hours * CurrentUser.hourlyRate} " +
                    "WHERE $JOB_ID = ${job.id}");
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }
    }

    fun deleteJob(job: Job) {
        var statement: Statement? = null
        try {
            statement = connection?.prepareStatement("DELETE FROM $TABLE_JOB WHERE $JOB_ID = ${job.id}")

            statement?.executeUpdate()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }
    }

    fun login(email: String, password: String) : Boolean {
        var statement: Statement? = null
        var resultSet: ResultSet? = null

        try {
            statement = connection?.createStatement()
            resultSet = statement?.executeQuery(
                "SELECT $USER_ID," +
                        "$USER_FIRST_NAME," +
                        "$USER_LAST_NAME," +
                        "$USER_HOURLY_RATE," +
                        "$USER_VACATION_RATE " +
                        "FROM $TABLE_USER " +
                        "WHERE $USER_EMAIL='$email' AND $USER_PASSWORD='$password'"
            )
            while (resultSet?.next() == true) {
                CurrentUser = User(
                    resultSet.getInt(USER_ID),
                    resultSet.getString(USER_FIRST_NAME),
                    resultSet.getString(USER_LAST_NAME),
                    email,
                    password,
                    resultSet.getDouble(USER_HOURLY_RATE),
                    resultSet.getDouble(USER_VACATION_RATE)
                )
                return true
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } finally {
            try {
                resultSet?.close()
            } catch (sqlEx: SQLException) {
            }
            try {
                statement?.close()
            } catch (sqlEx: SQLException) {
            }
        }

        return false
    }
}