package edu.ppsm.locationtracer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class PointParsingTest {

    @Test
    fun parsePointList_fromJson() {
        val json = """
            [
              {
                "longitude": 10.1234,
                "latitude": 50.5678,
                "timestamp": "2024-01-01 12:00:00"
              }
            ]
        """.trimIndent()

        val listType = object : TypeToken<List<Point>>() {}.type
        val points: List<Point> = Gson().fromJson(json, listType)

        assertEquals(1, points.size)
        assertEquals(10.1234, points[0].lon, 0.0001)
        assertEquals(50.5678, points[0].lat, 0.0001)
        assertEquals("2024-01-01 12:00:00", points[0].time)
    }
}
