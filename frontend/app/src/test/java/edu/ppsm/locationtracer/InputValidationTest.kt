package edu.ppsm.locationtracer

import android.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InputValidationTest {

    @Test
    fun login_showsError_whenLoginEmpty() {
        val activity = Robolectric.buildActivity(LoginActivity::class.java)
            .setup().get()

        activity.findViewById<android.widget.EditText>(R.id.editTextInsertLogin)
            .setText("")
        activity.findViewById<android.widget.EditText>(R.id.editTextInsertPassword)
            .setText("password")

        activity.findViewById<View>(R.id.buttonLogIn).performClick()

        val error = activity.findViewById<android.widget.TextView>(R.id.errorText)
        assertThat(error.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun register_showsError_whenPasswordEmpty() {
        val activity = Robolectric.buildActivity(RegisterActivity::class.java)
            .setup().get()

        activity.findViewById<android.widget.EditText>(R.id.editTextInsertLogin)
            .setText("user")
        activity.findViewById<android.widget.EditText>(R.id.editTextInsertPassword)
            .setText("")

        activity.findViewById<View>(R.id.buttonCreateAccount).performClick()

        val error = activity.findViewById<android.widget.TextView>(R.id.errorText)
        assertThat(error.visibility).isEqualTo(View.VISIBLE)
    }
}
