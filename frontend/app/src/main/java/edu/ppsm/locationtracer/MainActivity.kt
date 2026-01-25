package edu.ppsm.locationtracer

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingerprintjs.android.fingerprint.Fingerprinter
import com.fingerprintjs.android.fingerprint.FingerprinterFactory
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

class MainActivity : AppCompatActivity() {
    private var locMan: LocationManager? = null
    private var tracing = false
    var curUuid: String = ""

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

    private val refreshIntervalMs = 2_000L
    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)

                if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX < 0) {
                        val selectedIdx = devicesSpinner?.selectedItemPosition

                        var uuid = ""
                        if(devicesSpinner?.adapter != null) {
                            val adapter = devicesSpinner?.adapter as SpinnerAdapter
                            if (selectedIdx != null) {
                                uuid = adapter.devices[selectedIdx].uuid
                            }
                        }

                        val intent = Intent(this@MainActivity, MapsActivity::class.java)
                        intent.putExtra("DEVICE_UUID", uuid)
                        startActivity(intent)
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
    private fun startAutoRefresh() {
        if (refreshRunnable != null) return

        refreshRunnable = Runnable {
            getDevices();
            if(devicesSpinner?.adapter != null) {
                val adapter = devicesSpinner?.adapter as SpinnerAdapter
                val idx = devicesSpinner?.selectedItemPosition
                if (idx != null) {
                    val curUuid = adapter.devices[idx].uuid
                    getPoints(curUuid)
                }
            }
            handler.postDelayed(refreshRunnable!!, refreshIntervalMs)
        }

        handler.post(refreshRunnable!!)
    }

    private fun stopAutoRefresh() {
        refreshRunnable?.let {
            handler.removeCallbacks(it)
        }
        refreshRunnable = null
    }

    override fun onStart() {
        super.onStart()
        val fingerprinter = FingerprinterFactory.create(this)

        fingerprinter.getFingerprint(version = Fingerprinter.Version.V_5) { fingerprint ->
             this.curUuid = UUID.nameUUIDFromBytes(fingerprint.toByteArray()).toString()
            getDevices()
        }
        startAutoRefresh()
    }

    override fun onStop() {
        super.onStop()
        stopAutoRefresh()
    }

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
                                //if(!ret.isEmpty())
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

        if(!devices.map{it.uuid}.contains(curUuid))
            traceButton?.setEnabled(false)
        else
            traceButton?.setEnabled(true)

        var selectedIdx: Int?
        if(devicesSpinner?.adapter == null)
             selectedIdx = 0
        else
            selectedIdx = devicesSpinner?.selectedItemPosition
        val spinnerAdapter = SpinnerAdapter(
                this,
                devices
            )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        devicesSpinner?.adapter = spinnerAdapter
        spinnerAdapter.notifyDataSetChanged()

        if (selectedIdx != null) {
            devicesSpinner?.setSelection(selectedIdx)
            val curUuid = spinnerAdapter.devices[selectedIdx].uuid
            getPoints(curUuid)
        }
        //}

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
                                if(!ret.isEmpty())
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

    @RequiresApi(Build.VERSION_CODES.O)
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

        locMan = getSystemService(LOCATION_SERVICE) as LocationManager
        addDeviceButton = findViewById<Button>(R.id.buttonAddDevice)
        devicesSpinner = findViewById<Spinner>(R.id.devicesSpinner)
        traceButton = findViewById<Button>(R.id.buttonTrace)


        //TODO:Check if current device is already on list. May want to unhide the button if it's not or create a error text information
        addDeviceButton?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddDeviceActivity::class.java)
            addDeviceLauncher.launch(intent)
        }

        findViewById<View>(R.id.buttonSignout)?.setOnClickListener {
            JwtManager.clearJwt(this)
            var intent = Intent(this, ForegroundLocationService::class.java)
            intent.action = ForegroundLocationService.ACTION_STOP
            startService(intent)
            intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        traceButton?.setOnClickListener {
            if (!tracing) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) !=
                    PackageManager.PERMISSION_GRANTED)
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                if(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) !=
                    PackageManager.PERMISSION_GRANTED)
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                if(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    ) !=
                    PackageManager.PERMISSION_GRANTED)
                    requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                if(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE
                    ) !=
                    PackageManager.PERMISSION_GRANTED)
                    requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE)



                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(this, ForegroundLocationService::class.java)
                    intent.action = ForegroundLocationService.ACTION_START
                    intent.putExtra("uuid", curUuid)
                    startForegroundService(intent)

                    findViewById<Button>(R.id.buttonTrace).text =
                        getResources().getString(R.string.buttonStop)
                    tracing = true
                }

            } else {
                //locMan!!.removeUpdates(this)

                val intent = Intent(this, ForegroundLocationService::class.java)
                intent.action = ForegroundLocationService.ACTION_STOP
                startService(intent)
                findViewById<Button>(R.id.buttonTrace).text =
                    getResources().getString(R.string.buttonStart)
                tracing = false
            }

        }
    }
}