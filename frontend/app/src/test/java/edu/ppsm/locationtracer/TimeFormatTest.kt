package edu.ppsm.locationtracer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TimeFormatTest {

    @Test
    fun timestampFormatsCorrectly() {
        val time = 0L // Jan 1, 1970 00:00:00 UTC

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val formatted = sdf.format(Date(time))

        assertEquals("1970-01-01 00:00:00", formatted)
    }
}
