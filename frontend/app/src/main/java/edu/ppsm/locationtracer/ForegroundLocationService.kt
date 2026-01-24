package edu.ppsm.locationtracer
import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.location.LocationListenerCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ForegroundLocationService :
    Service(),
    LocationListenerCompat {

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
    }

    private var locationManager: LocationManager? = null
    private var deviceUuid: String = ""

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                deviceUuid = intent.getStringExtra("uuid") ?: return START_NOT_STICKY
                startForeground(1, buildNotification())
                startGps()
            }
            ACTION_STOP -> {
                stopGps()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startGps() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10_000L,
            10f,
            this
        )
    }

    private fun stopGps() {
        locationManager?.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        reportLocation(
            deviceUuid,
            location.longitude,
            location.latitude,
            location.time
        )
    }

    private fun reportLocation(
        uuid: String,
        lon: Double,
        lat: Double,
        time: Long
    ) {
        val token = JwtManager.getJwt(applicationContext) ?: return
        val client = OkHttpClient()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date(time))

        val url =
            getString(R.string.URL) +
                    "/gps?uuid=${URLEncoder.encode(uuid, "UTF-8")}" +
                    "&longitude=${URLEncoder.encode(lon.toString(), "UTF-8")}" +
                    "&latitude=${URLEncoder.encode(lat.toString(), "UTF-8")}" +
                    "&time=${URLEncoder.encode(formattedTime, "UTF-8")}"

        val request = Request.Builder()
            .url(url)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .addHeader("Authorization", "Bearer $token")
            .build()

        thread {
            try {
                client.newCall(request).execute().close()
            } catch (_: Exception) {}
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location tracking")
            .setContentText("GPS tracking active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
