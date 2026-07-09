package com.example.animatedkeyboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * An InputMethodService can't show the system's runtime-permission dialog
 * itself — only an Activity can. This transparent, no-UI activity exists
 * solely to request RECORD_AUDIO on behalf of the keyboard's Mic key, then
 * closes immediately. The user just taps the mic key again afterward to
 * actually start listening.
 */
class SpeechPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            val message = if (granted) "Microphone enabled — tap the mic key again to speak" else "Microphone permission denied"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 4201
    }
}
