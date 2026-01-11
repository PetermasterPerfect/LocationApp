package edu.ppsm.locationtracer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread


data class UuidAndName (
    val uuid: String,
    val name: String
)

data class Point(
    @SerializedName("longitude")
    val lon: Double,

    @SerializedName("latitude")
    val lat: Double,

    @SerializedName("timestamp")
    val time: String
)


class SpinnerAdapter (
    context: Context, devices: List<UuidAndName>
) : ArrayAdapter<String?>(context, android.R.layout.simple_spinner_item
    , devices?.map {it.name} as MutableList<String>) {
    public val devices = devices
}

class MainActivity : AppCompatActivity(), LocationListenerCompat, OnMapReadyCallback {
    private var locMan: LocationManager? = null
    private var tracing = false

    private var mapView: MapView? = null
    private var deviceView: RecyclerView? = null
    private var devicesSpinner: Spinner? = null
    private var addDeviceButton: Button? = null
    private var traceButton: Button? = null
    private val addDeviceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        getDevices()
    }

    private var pointsAdapter: RecyclerViewAdapter? = null
    private var pointsView: RecyclerView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(
                applicationContext, R.string.permissionGranted,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                applicationContext, R.string.noPermission,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun reportLocation(devUuid: String, lon: Double, lat: Double, time: Long) {
        val token = JwtManager.getJwt(this)
        val client = OkHttpClient()

        val encodedUuid = URLEncoder.encode(devUuid, "UTF-8")
        val encodedLon = URLEncoder.encode(lon.toString(), "UTF-8")
        val encodedLat = URLEncoder.encode(lat.toString(), "UTF-8")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatted = sdf.format(Date(time))

        val encodedTime = URLEncoder.encode(formatted.toString(), "UTF-8")
        val url = getString(R.string.URL) + "/gps?uuid=$encodedUuid&longitude=$encodedLon&latitude=$encodedLat&time=$encodedTime"
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .build()

        thread {
            try {
                client.newCall(request).execute().use { response ->
                    runOnUiThread {
                        when (response.code) {
                            200, 201 -> {
                                println("Added $lon | $lat")
                            }
                            else -> {
                                println("$response.code : $response.message")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error: $e")
            }
        }
    }

    private fun updatePointsWidgets(points: List<Point>) {
        pointsAdapter = RecyclerViewAdapter(this, points)
        pointsView = findViewById(R.id.pointsView)
        pointsView?.setHasFixedSize(true)
        pointsView?.layoutManager = LinearLayoutManager(this)
        pointsView?.adapter = pointsAdapter
        pointsView?.adapter!!.notifyItemRangeChanged(0, pointsAdapter!!.itemCount)
    }

    private fun getPoints(curUuid: String) {
        val token = JwtManager.getJwt(this)
        val client = OkHttpClient()

        val encodedUuid = URLEncoder.encode(curUuid, "UTF-8")
        val url = getString(R.string.URL) + "/gpsall?uuid=$encodedUuid"
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
                                val listType = object : TypeToken<List<Point>>() {}.type
                                val ret = gson.fromJson<List<Point>>(body, listType)
                                updatePointsWidgets(ret)
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
    private fun updateDevicesWidgets(devices: List<UuidAndName>) {

        val deviceUuid = UUID.nameUUIDFromBytes(Settings.Secure.ANDROID_ID.toByteArray()).toString()
        if(devices.isEmpty())
            traceButton?.setEnabled(false)

        for(device in devices) {
            if(device.uuid == deviceUuid)
                addDeviceButton?.setEnabled(false)
        }
        val spinnerAdapter = SpinnerAdapter(
            this,
            devices
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        devicesSpinner?.adapter = spinnerAdapter
        spinnerAdapter.notifyDataSetChanged()

        val idx = devicesSpinner?.selectedItemPosition
        if (idx != null) {
            val curUuid = spinnerAdapter.devices[idx].uuid
            getPoints(curUuid)
        }
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

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
        //mapFragment?.getMapAsync(this)

        locMan = getSystemService(LOCATION_SERVICE) as LocationManager
        addDeviceButton = findViewById<Button>(R.id.buttonAddDevice)
        devicesSpinner = findViewById<Spinner>(R.id.devicesSpinner)
        traceButton = findViewById<Button>(R.id.buttonTrace)
        getDevices()

        //TODO:Check if current device is already on list. May want to unhide the button if it's not or create a error text information
        addDeviceButton?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddDeviceActivity::class.java)
            addDeviceLauncher.launch(intent)
        }

        findViewById<View>(R.id.buttonSignout)?.setOnClickListener {
            JwtManager.clearJwt(this)
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        devicesSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val adapter = devicesSpinner?.adapter as SpinnerAdapter
                val idx = devicesSpinner?.selectedItemPosition
                if (idx != null) {
                    val curUuid = adapter.devices[idx].uuid
                    getPoints(curUuid)
                }

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        traceButton?.setOnClickListener {
            if(!tracing) {
                if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) !=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) !=
                        PackageManager.PERMISSION_GRANTED
                    )
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                if(ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED)
                    locMan!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, this)

                findViewById<Button>(R.id.buttonTrace).text = getResources().getString(R.string.buttonStop)
                tracing = true
            } else {
                locMan!!.removeUpdates(this)
                findViewById<Button>(R.id.buttonTrace).text = getResources().getString(R.string.buttonStart)
                tracing = false
            }

        }
    }

    override fun onLocationChanged(location: Location) {
        if(tracing &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED) {
            println("location changed")

            val adapter = devicesSpinner?.adapter as SpinnerAdapter
            val idx = devicesSpinner?.selectedItemPosition
            if (idx != null) {
                val curUuid = adapter.devices[idx].uuid
                reportLocation(curUuid, location.longitude, location.latitude, location.time)
                getPoints(curUuid)
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        TODO("Not yet implemented")
    }


}