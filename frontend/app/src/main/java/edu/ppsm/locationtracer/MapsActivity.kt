package edu.ppsm.locationtracer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.ppsm.locationtracer.databinding.ActivityMapsBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    var selectedUuid : String = ""
    val EXTRA_DEVICE_UUID = "DEVICE_UUID"

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
                                var point : LatLng? = null
                                if(!ret.isEmpty()) {
                                    for (r in ret) {
                                        val mark = LatLng(r.lat, r.lon)
                                        if(point == null)
                                            point = mark
                                        mMap.addMarker(MarkerOptions().position(mark))
                                    }
                                    if(point!=null)
                                        mMap.moveCamera(CameraUpdateFactory.newLatLng(point))
                                }
                            }
                            else -> {
                                Toast.makeText(this@MapsActivity, response.message, Toast.LENGTH_SHORT).show()
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
        val uuid = intent.getStringExtra(EXTRA_DEVICE_UUID)
        if(uuid != null)
            selectedUuid = uuid
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getPoints(selectedUuid)
    }
}