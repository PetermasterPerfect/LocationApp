package edu.ppsm.locationtracer

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddDeviceActivity : AppCompatActivity() {

    private var textInformation: TextView? = null
    private var editDevice: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.buttonReturn).setOnClickListener{
            finish()
        }

        //TODO: For simplicity change one field to two TextViews and delete html parsing
        textInformation = findViewById(R.id.textInformation)
        textInformation!!.text = HtmlCompat.fromHtml(
            getString(R.string.informationText),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )


        //TODO: Set up actual communication with DB
        editDevice = findViewById(R.id.editDeviceName)

        findViewById<View>(R.id.buttonCreateDevice).setOnClickListener{
            val name = editDevice?.text.toString()

            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            val sysName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }

            val newDevice = Device(name, "Home", sysName)
            DeviceRepository.addDevice(newDevice)

            Log.i("Devices", "Devices"+ DeviceRepository.devices)
            finish()
        }
    }
}