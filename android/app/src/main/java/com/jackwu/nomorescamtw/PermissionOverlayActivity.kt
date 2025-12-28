package com.jackwu.nomorescamtw

import android.content.res.ColorStateList
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PermissionOverlayActivity : AppCompatActivity() {

    private lateinit var btnGrant: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_overlay)

        btnGrant = findViewById(R.id.btnGrantOverlay)
        btnNext = findViewById(R.id.btnNextOverlay)
        
        // Ensure text is white
        btnGrant.setTextColor(Color.WHITE)
        btnNext.setTextColor(Color.WHITE)

        btnGrant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        btnNext.setOnClickListener {
            startActivity(Intent(this, PermissionAccessibilityActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            btnGrant.isEnabled = false
            btnGrant.text = getString(R.string.btn_granted)
            btnGrant.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            btnNext.isEnabled = true
            btnNext.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary_blue))
        } else {
            // If Android < M, permission is granted by manifest usually, but logic holds for >= M
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
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
    }
}
