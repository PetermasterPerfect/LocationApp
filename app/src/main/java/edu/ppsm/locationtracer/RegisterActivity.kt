package edu.ppsm.locationtracer

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
                errorText?.visibility = View.VISIBLE
            }else{
                createAccount(login, password)
            }
        }
    }

    private fun createAccount(login: String, password: String){
        //TODO: Implement account creation
    }

}