package edu.ppsm.locationtracer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecyclerViewAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val parent = FrameLayout(context) as ViewGroup

    @Test
    fun itemCount_matchesDataSetSize() {
        val points = listOf(
            Point(1.0, 2.0, "t1"),
            Point(3.0, 4.0, "t2")
        )

        val adapter = RecyclerViewAdapter(context, points)

        assertThat(adapter.itemCount).isEqualTo(2)
    }

    @Test
    fun numbering_startsFromOne() {
        val points = listOf(Point(10.0, 20.0, "time"))
        val adapter = RecyclerViewAdapter(context, points)

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertThat(holder.textNum.text.toString()).isEqualTo("1")
    }

    @Test
    fun coordinates_areFormattedCorrectly() {
        val point = Point(10.1299, 20.9876, "time")
        val adapter = RecyclerViewAdapter(context, listOf(point))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertThat(holder.textLonLat.text.toString())
            .isEqualTo("Position: (10.13 , 20.99)")
    }
}
