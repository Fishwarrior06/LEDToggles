package com.fishwarrior06.ledtoggle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request required runtime permissions
        checkPermissions()
    }

    private fun checkPermissions() {
        // the explicit BLUETOOTH_CONNECT permission is required to interact with BLE devices.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {

            // If permissions are not granted, request them using the system dialog
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                101
            )
        } else {
            // Permissions were already granted previously
            showReadyMessage()
        }
    }

    // Automatically triggered when the user responds to the permission dialog (Allow/Deny)
    override fun onRequestPermissionsResult(rc: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(rc, perms, results)
        if (rc == 101 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            // User successfully granted permissions
            showReadyMessage()
        } else {
            // User denied permissions
            Toast.makeText(
                this,
                "Bluetooth permission is required for the Quick Settings tile to work.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun showReadyMessage() {
        Toast.makeText(
            this,
            "Permissions ready! You can now add the 'LED Lights' tile to your Quick Settings panel.",
            Toast.LENGTH_LONG
        ).show()

        // Immediately close the Activity so it doesn't stay open on the screen,
        // since the entire feature is controlled from the Quick Settings panel
        finish()
    }
}