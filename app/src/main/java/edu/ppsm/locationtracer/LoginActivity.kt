package edu.ppsm.locationtracer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
        errorText = findViewById(R.id.errorText)
        Log.d("LoginActivity", "Login: $login")
        Log.d("LoginActivity", "Password: $password")

        //TODO: implement login functionality
        val correctLogin = login == "dev"

        if (correctLogin){
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            errorText?.text = "Incorrect login or password, please try again!"
            errorText?.visibility = View.VISIBLE
        }
    }
}