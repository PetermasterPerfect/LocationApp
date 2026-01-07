package edu.ppsm.locationtracer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private var deviceView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //TODO:Check if current device is already on list. May want to unhide the button if it's not or create a error text information
        findViewById<View>(R.id.buttonAddDevice).setOnClickListener {

            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            val sysName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }

            val alreadyExists = DeviceRepository.devices.any { it.sysName == sysName }

            if (alreadyExists) {
                Toast.makeText(this, "Device already added!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this@MainActivity, AddDeviceActivity::class.java)
                startActivity(intent)
            }
        }
    }
}