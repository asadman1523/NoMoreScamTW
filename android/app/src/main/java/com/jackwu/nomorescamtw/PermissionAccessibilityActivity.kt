package com.jackwu.nomorescamtw

import android.content.res.ColorStateList
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.jackwu.nomorescamtw.service.ScamDetectionService

class PermissionAccessibilityActivity : AppCompatActivity() {

    private lateinit var btnGrant: Button
    private lateinit var btnNext: Button
    private lateinit var ivTutorial: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_accessibility)

        btnGrant = findViewById(R.id.btnGrantAccess)
        btnNext = findViewById(R.id.btnNextAccess)
        ivTutorial = findViewById(R.id.ivTutorial)
        val btnDebugSkip = findViewById<Button>(R.id.btnDebugSkip)
        
        // Ensure text is white
        btnGrant.setTextColor(Color.WHITE)
        btnNext.setTextColor(Color.WHITE)
        btnDebugSkip.setTextColor(Color.WHITE)

        // Show skip button only in debug builds
        if (BuildConfig.DEBUG) {
            btnDebugSkip.visibility = android.view.View.VISIBLE
            btnDebugSkip.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
        }

        playAnimation()

        btnGrant.setOnClickListener {
            showAccessibilityDisclosure()
        }

        btnNext.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun showAccessibilityDisclosure() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.accessibility_disclosure_title))
        builder.setMessage(getString(R.string.accessibility_disclosure_message))
        builder.setCancelable(false) // Required by policy: cannot be dismissed by touching outside
        builder.setPositiveButton(getString(R.string.btn_agree)) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton(getString(R.string.btn_deny)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
        // Set button colors for better visibility in dark mode
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.dialog_button))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.dialog_button))
    }

    private fun playAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resources, R.drawable.accessibility_tutorial)
            val drawable = ImageDecoder.decodeDrawable(source)
            ivTutorial.setImageDrawable(drawable)
            if (drawable is Animatable) {
                drawable.start()
            }
        } else {
            // For API < 28, it will show as a static image
            ivTutorial.setImageResource(R.drawable.accessibility_tutorial)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        // Restart animation if needed when returning to the app
        val drawable = ivTutorial.drawable
        if (drawable is Animatable && !drawable.isRunning) {
            drawable.start()
        }
    }

    private fun checkPermission() {
        if (isAccessibilityServiceEnabled()) {
            btnGrant.isEnabled = false
            btnGrant.text = getString(R.string.btn_granted)
            btnGrant.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            btnNext.isEnabled = true
            btnNext.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary_blue))
        } else {
            btnGrant.isEnabled = true
            btnGrant.text = getString(R.string.btn_open_settings)
            btnGrant.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary_blue))
            btnNext.isEnabled = false
            btnNext.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/${ScamDetectionService::class.java.name}") == true
    }
}
