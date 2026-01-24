package edu.ppsm.locationtracer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JwtManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        JwtManager.clearJwt(context) // Clean slate before each test
    }

    @After
    fun teardown() {
        JwtManager.clearJwt(context)
    }

    @Test
    fun saveAndGetJwt_returnsSameToken() {
        val token = "test.jwt.token"

        JwtManager.saveJwt(context, token)
        val result = JwtManager.getJwt(context)

        assertEquals(token, result)
    }

    @Test
    fun clearJwt_removesToken() {
        JwtManager.saveJwt(context, "token")
        JwtManager.clearJwt(context)

        val result = JwtManager.getJwt(context)

        assertNull(result)
    }

    @Test
    fun getJwt_whenNothingSaved_returnsNull() {
        val result = JwtManager.getJwt(context)

        assertNull(result)
    }
}
