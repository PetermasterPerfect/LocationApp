package edu.ppsm.locationtracer

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class LoginActivity : AppCompatActivity() {

    private var editLogin: EditText? = null
    private var editPassword: EditText? = null
    private var errorText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editLogin = findViewById(R.id.editTextInsertLogin)
        editPassword = findViewById(R.id.editTextInsertPassword)
        errorText = findViewById(R.id.errorText)

        findViewById<View>(R.id.buttonLogIn).setOnClickListener {
            val login = editLogin?.text.toString()
            val password = editPassword?.text.toString()

            if (login.isEmpty() || password.isEmpty()) {
                errorText?.text = "Please enter both login and password!"
                //TODO: hide error text again after passing correct credentials
                errorText?.visibility = View.VISIBLE
            }else{
                login(login, password)
            }
        }

        findViewById<View>(R.id.buttonRegister).setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(login: String, password: String){
        val client = OkHttpClient()

        val encodedLogin = URLEncoder.encode(login, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")
        val url = getResources().getString(R.string.URL) + "/login?login=$encodedLogin&password=$encodedPassword"
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
                                    this@LoginActivity,
                                    "Successful login",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }

                            400 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    response.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            401 -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    response.message,
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