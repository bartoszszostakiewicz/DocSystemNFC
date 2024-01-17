package com.docsysnfc.sender


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.docsysnfc.sender.ui.LoginScreen
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        //check if user is already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContent {
            Column (
                modifier = Modifier
                    .background(Color(0xFFDEF2FD))

            ) {
                LoginScreen(auth,this@LoginActivity)

            }


        }
    }
}



