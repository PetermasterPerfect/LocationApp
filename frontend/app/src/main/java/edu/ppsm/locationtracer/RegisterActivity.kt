package edu.ppsm.locationtracer

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlin.concurrent.thread


class RegisterActivity : AppCompatActivity() {

    private var backText: TextView? = null
    private var editLogin: EditText? = null
    private var editPassword: EditText? = null
    private var errorText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backText = findViewById<TextView>(R.id.backText)
        backText!!.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        editPassword = findViewById<EditText>(R.id.editTextInsertPassword)
        editLogin = findViewById<EditText>(R.id.editTextInsertLogin)
        errorText = findViewById<TextView>(R.id.errorText)

        backText!!.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.buttonCreateAccount).setOnClickListener{
            val login = editLogin?.text.toString()
            val password = editPassword?.text.toString()

            if (login.isEmpty() || password.isEmpty()) {
                errorText?.text = "Please enter both login and password!"
                //TODO: hide error text again after passing correct credentials
                errorText?.visibility = View.VISIBLE
            }else{
                createAccount(login, password)
            }
        }
    }

    private fun createAccount(login: String, password: String){
        val client = OkHttpClient()

        val encodedLogin = URLEncoder.encode(login, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")
        val url = getResources().getString(R.string.URL) + "/signup?login=$encodedLogin&password=$encodedPassword"
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .build()


        thread {
            try {
                client.newCall(request).execute().use { response ->
                    runOnUiThread {
                        when (response.code) {
                            201, 200 -> {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Successful signup",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }

                            409 -> {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Such login already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Error: ${response.code}",
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

}