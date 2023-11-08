package com.docsysnfc.view


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.docsysnfc.R
import com.google.firebase.auth.FirebaseAuth



class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        //check if user is already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, SendActivity::class.java))
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


@OptIn(ExperimentalMaterial3Api::class)
//i want set this color to background color ->
@Composable
fun LoginScreen(auth : FirebaseAuth,context: Context) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    var isEmailValid by remember { mutableStateOf(false) }
    var isPasswordValid by remember { mutableStateOf(false) }

    /*****TO DO : get from xml strings*******/
    val loginName  = "login"
    val passwordName = "password"
    /****************************************/

    /*****TO DO : get from color strings*****/
    val loginButtonColor = Color.Blue
    val loginButtonTextColor = Color.White

    //val borderColor = Color.Blue

    val bitMap = painterResource(id = R.drawable.final_logo_)
    val backgroundColor = Color(0xFFDEF2FD)

    val scrollState = rememberScrollState()

    /****************************************/



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)// Thanks to this padding, the background does not spread over the entire screen, I don't want it
            .verticalScroll(state = scrollState)
            .background(color = backgroundColor),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = bitMap ,
            contentDescription = "Logo")

        OutlinedTextField(
            label = {Text(loginName)},
            value = email,
            onValueChange = {
                email = it
                isEmailValid = isValidEmail(email)

                },

            //singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textStyle = TextStyle(color = Color.Black)
        )



        OutlinedTextField(
            label = {Text(passwordName)},
            value = password,
            onValueChange = {
                password = it
                isPasswordValid = password.isNotEmpty()
                },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textStyle = TextStyle(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {

                if (isEmailValid && isPasswordValid) {

                    if (auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Log.d("qwertty", "1signInWithEmail:success")



                                context.startActivity(Intent(context, SendActivity::class.java))

                            } else {
                                Log.w("qwertty", "signInWithEmail:failure", it.exception)
                                //updateUI(null)
                            }
                        }.isSuccessful) {
                    }

                }


            },
            modifier = Modifier
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = loginButtonColor, contentColor = loginButtonTextColor)
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClickableText(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.Gray,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Don't remember password?")
                }
            },
            onClick = {
                /*********TODO PASSWORD RECOVERY  ***********************************/
                // Handle the click action here
            }
        )
    }
}



// Function to validate email format using regular expression
private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    return email.matches(emailRegex.toRegex())
}

