package com.jackwu.nomorescamtw

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.jackwu.nomorescamtw.service.ScamDetectionService

class DisclaimerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("terms_accepted", false)) {
            navigateForward()
            return
        }
        
        setContentView(R.layout.activity_disclaimer)

        val btnAccept = findViewById<Button>(R.id.btnAccept)
        btnAccept.setTextColor(Color.WHITE)

        btnAccept.setOnClickListener {
            prefs.edit().putBoolean("terms_accepted", true).apply()
            navigateForward()
        }
    }

    private fun navigateForward() {
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        val accessibilityGranted = isAccessibilityServiceEnabled()

        if (overlayGranted && accessibilityGranted) {
            startActivity(Intent(this, MainActivity::class.java))
        } else if (overlayGranted) {
            startActivity(Intent(this, PermissionAccessibilityActivity::class.java))
        } else {
            startActivity(Intent(this, PermissionOverlayActivity::class.java))
        }
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/${ScamDetectionService::class.java.name}") == true
    }
}
