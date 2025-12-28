package com.jackwu.nomorescamtw

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jackwu.nomorescamtw.worker.UpdateDatabaseWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var llWarningBar: LinearLayout
    private lateinit var tvWarningDetail: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvNextUpdate: TextView
    private lateinit var btnUpdate: Button
    private lateinit var etUrl: EditText
    private lateinit var btnCheck: Button
    private lateinit var tvSafeResult: TextView

    private val PREFS_NAME = "app_prefs"
    private val KEY_LAST_DB_UPDATE = "last_db_update_ts"
    private val KEY_LAST_MANUAL_ATTEMPT = "last_manual_attempt_ts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        llWarningBar = findViewById(R.id.llWarningBar)
        tvWarningDetail = findViewById(R.id.tvWarningDetail)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvNextUpdate = findViewById(R.id.tvNextUpdate)
        btnUpdate = findViewById(R.id.btnUpdate)
        etUrl = findViewById(R.id.etUrl)
        btnCheck = findViewById(R.id.btnCheck)
        tvSafeResult = findViewById(R.id.tvSafeResult)
        
        btnUpdate.setTextColor(Color.WHITE)
        btnCheck.setTextColor(Color.WHITE)

        val app = application as MyApplication
        val repository = app.repository

        // Schedule Background Daily Update
        val updateRequest = PeriodicWorkRequestBuilder<UpdateDatabaseWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyDatabaseUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )

        updateUIStatus()
        checkAutoUpdate()

        // Handle shared text
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (sharedText != null) {
                etUrl.setText(sharedText)
                btnCheck.performClick()
            }
        }

        btnUpdate.setOnClickListener {
            handleManualUpdate()
        }

        btnCheck.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                lifecycleScope.launch {
                    val result = repository.checkUrl(url)
                    if (result != null) {
                        // Show Red Warning Bar at top
                        llWarningBar.visibility = View.VISIBLE
                        tvWarningDetail.text = String.format(
                            "%s: %s | %s: %d\n%s: %s",
                            getString(R.string.label_name), result.name,
                            getString(R.string.label_count), result.count,
                            getString(R.string.label_url), result.url
                        )
                        
                        tvSafeResult.text = getString(R.string.msg_scam_detected)
                        tvSafeResult.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
                    } else {
                        // Hide Warning Bar, show safe message
                        llWarningBar.visibility = View.GONE
                        tvSafeResult.text = getString(R.string.msg_safe)
                        tvSafeResult.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                    }
                }
            }
        }
    }

    private fun checkAutoUpdate() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_DB_UPDATE, 0)
        val now = System.currentTimeMillis()

        // Update if never updated or > 24 hours ago
        if (lastUpdate == 0L || (now - lastUpdate) > 24 * 60 * 60 * 1000) {
             Toast.makeText(this, getString(R.string.auto_update_started), Toast.LENGTH_SHORT).show()
             performUpdate(isManual = false)
        }
    }

    private fun handleManualUpdate() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAttempt = prefs.getLong(KEY_LAST_MANUAL_ATTEMPT, 0)
        val now = System.currentTimeMillis()
        val cooldown = 10 * 60 * 1000 // 10 minutes

        if (now - lastAttempt < cooldown) {
            val minutesLeft = (cooldown - (now - lastAttempt)) / 60000 + 1
            Toast.makeText(this, getString(R.string.update_cooldown, minutesLeft), Toast.LENGTH_SHORT).show()
        } else {
            performUpdate(isManual = true)
        }
    }

    private fun performUpdate(isManual: Boolean) {
        if (isManual) {
            Toast.makeText(this, getString(R.string.toast_updating), Toast.LENGTH_SHORT).show()
            // Update attempt time for cooldown
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_MANUAL_ATTEMPT, System.currentTimeMillis()).apply()
        }

        val workRequest = OneTimeWorkRequestBuilder<UpdateDatabaseWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)
        
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    // Update success timestamp
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putLong(KEY_LAST_DB_UPDATE, System.currentTimeMillis()).apply()
                    
                    updateUIStatus()
                    if (isManual) {
                        Toast.makeText(this, getString(R.string.toast_update_finished), Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateUIStatus() {
        lifecycleScope.launch {
            val app = application as MyApplication
            val count = app.repository.getDatabaseSize()
            tvStatus.text = getString(R.string.db_status, count)
            
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastUpdate = prefs.getLong(KEY_LAST_DB_UPDATE, 0)
            
            if (lastUpdate > 0) {
                val date = Date(lastUpdate)
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dateStr = format.format(date)
                
                val nextDate = Date(lastUpdate + 24 * 60 * 60 * 1000)
                val nextStr = format.format(nextDate)
                
                tvLastUpdated.text = getString(R.string.last_updated, dateStr)
                tvNextUpdate.text = getString(R.string.next_update, nextStr)
            } else {
                tvLastUpdated.text = getString(R.string.last_updated, getString(R.string.never_updated))
                tvNextUpdate.text = getString(R.string.next_update, getString(R.string.never_updated))
            }
        }
    }
}