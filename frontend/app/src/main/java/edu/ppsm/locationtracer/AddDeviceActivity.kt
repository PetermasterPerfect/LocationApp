package edu.ppsm.locationtracer

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.UUID
import kotlin.concurrent.thread

class AddDeviceActivity : AppCompatActivity() {

    private var textInformation: TextView? = null
    private var editDevice: EditText? = null

    private fun addDevice(uuid: String, name: String ) {
        val token = JwtManager.getJwt(this)
        val client = OkHttpClient()

        val encodedUuid = URLEncoder.encode(uuid, "UTF-8")
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val url = getResources().getString(R.string.URL) + "/devices?uuid=$encodedUuid&name=$encodedName"
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .build()

        thread {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    runOnUiThread {
                        when (response.code) {
                            201, 200 -> {
                                Toast.makeText(
                                    this@AddDeviceActivity,
                                    "Device was added successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()

                            }
                            else -> {
                                Toast.makeText(
                                    this@AddDeviceActivity,
                                    body,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error!!!! : $e")
            }
        }
    }

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
            addDevice( UUID.nameUUIDFromBytes(Settings.Secure.ANDROID_ID.toByteArray()).toString(), editDevice?.getText().toString())
            finish()
        }
    }
}