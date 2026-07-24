package com.example.animatedkeyboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SpeechPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppressTransitionAnimation()

        val alreadyGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            finish()
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val message = if (granted) {
                "Microphone enabled — tap the mic key again to speak"
            } else {
                "Microphone permission denied — you can enable it in system settings"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    override fun finish() {
        super.finish()
        suppressTransitionAnimation()
    }

    @Suppress("DEPRECATION")
    private fun suppressTransitionAnimation() {
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val REQUEST_CODE = 4201
    }
}
