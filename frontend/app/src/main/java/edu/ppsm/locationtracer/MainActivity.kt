package edu.ppsm.locationtracer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import kotlin.concurrent.thread


data class UuidAndName (
    val uuid: String,
    val name: String
)

class MainActivity : AppCompatActivity() {

    private var deviceView: RecyclerView? = null
    private var devicesSpinner: Spinner? = null
    private var addDeviceButton: View? = null
    private val addDeviceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        getDevices()
    }
    private fun updateDevicesWidgets(devices: List<UuidAndName>) {

        val deviceUuid = UUID.nameUUIDFromBytes(Settings.Secure.ANDROID_ID.toByteArray()).toString()
        for(device in devices) {
            if(device.uuid == deviceUuid)
                addDeviceButton?.setEnabled(false)
        }
        val spinnerAdapter = ArrayAdapter<String?>(
            this,
            android.R.layout.simple_spinner_item,
            devices?.map {it.name} as MutableList<String>
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        devicesSpinner?.adapter = spinnerAdapter
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun getDevices() {
        val token = JwtManager.getJwt(this)
        val client = OkHttpClient()

        val url = getString(R.string.URL) + "/devices"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .build()

        thread {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    runOnUiThread {
                        when (response.code) {
                            200, 201 -> {
                                val gson = Gson()
                                val listType = object : TypeToken<List<UuidAndName>>() {}.type
                                val ret = gson.fromJson<List<UuidAndName>>(body, listType)
                                updateDevicesWidgets(ret)
                            }
                            else -> {
                                Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error: $e")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        addDeviceButton = findViewById<View>(R.id.buttonAddDevice)
        devicesSpinner = findViewById<Spinner>(R.id.devicesSpinner)
        getDevices()

        //TODO:Check if current device is already on list. May want to unhide the button if it's not or create a error text information
        addDeviceButton?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddDeviceActivity::class.java)
            addDeviceLauncher.launch(intent)
        }
    }

}