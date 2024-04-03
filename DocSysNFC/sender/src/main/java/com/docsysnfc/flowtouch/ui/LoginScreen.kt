package com.docsysnfc.flowtouch.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.model.flowtouchStates.AuthenticationStatus
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCSysScreen
import com.docsysnfc.flowtouch.ui.theme.backgroundColor
import com.docsysnfc.flowtouch.ui.theme.buttonsColor
import com.docsysnfc.flowtouch.ui.theme.clickableTextColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldCursorColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldFocusedBorderColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldUnfocusedBorderColor
import com.docsysnfc.flowtouch.ui.theme.textColor
import com.docsysnfc.flowtouch.ui.theme.whiteColor


const val TAG = "NFC123"

@Composable
fun LoginScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    viewModel.disableNFCReaderMode(context as Activity)

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.authenticationStatus == AuthenticationStatus.SUCCESS) {
        navController.navigate(NFCSysScreen.Home.name)
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    var isEmailValid by remember { mutableStateOf(false) }
    var isPasswordValid by remember { mutableStateOf(false) }


    val loginName = stringResource(id = R.string.login)
    val passwordName = stringResource(id = R.string.password)


    val loginButtonColor = buttonsColor
    val loginButtonTextColor = whiteColor

    val bitMap = painterResource(id = R.drawable.final_logo_)
    val backgroundColor = backgroundColor

    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.authenticationStatus) {
        when (uiState.authenticationStatus) {
            AuthenticationStatus.SUCCESS -> {
                Log.d(TAG, "Authentication success")

                if(uiState.navigationDestination == NFCSysScreen.Receive){
                    navController.navigate(NFCSysScreen.Receive.name)
                }else{
                    navController.navigate(NFCSysScreen.Home.name)
                }

            }

            AuthenticationStatus.FAILURE -> {
                //dialog authentication failed
                Log.d(TAG, "Authentication failed")
                // Handle failure
            }
            // ... other states
            AuthenticationStatus.UNKNOWN -> {

            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = com.docsysnfc.flowtouch.ui.theme.backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(state = scrollState)
                .imePadding()
                .background(color = backgroundColor),

            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = bitMap,
                contentDescription = stringResource(R.string.logo)
            )

            CustomTextField(
                text = email,
                onTextChange = {
                    email = it
                    isEmailValid = isValidEmail(email)
                },
                label = loginName,
                isTextValid = isEmailValid,
                visualTransformation = VisualTransformation.None
            )



            CustomTextField(
                text = password,
                onTextChange = {
                    password = it
                    isPasswordValid =
                        password.isNotEmpty()
                },
                label = "Password",
                isTextValid = isPasswordValid,
                visualTransformation = PasswordVisualTransformation(),
            )


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isEmailValid && isPasswordValid) {
                        viewModel.signInWithEmailAndPassword(email, password)
                    }

                },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = loginButtonColor,
                    contentColor = loginButtonTextColor
                )
            ) {
                Text(stringResource(id = R.string.login))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = clickableTextColor)) {
                            append(stringResource(id = R.string.register_now))
                        }
                    },
                    onClick = {
                        Log.d(TAG, "Navigate to registration screen")
                        navController.navigate(NFCSysScreen.Create.name)
                    },
                    modifier = Modifier.padding(end = 24.dp)
                )

                Text("|", color = Color.Gray)

                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = clickableTextColor)) {
                            append(stringResource(id = R.string.recovery_password))
                        }
                    },
                    onClick = {
                        navController.navigate(NFCSysScreen.Recovery.name)
                    },
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}


@Composable
fun CustomTextField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    isTextValid: Boolean? = null, // Optional argument for validity
    visualTransformation: VisualTransformation = VisualTransformation.None, // Optional argument for visual transformation
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = {
            Text(
                label,
                color = textColor
            )
                },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = outlineTextFieldFocusedBorderColor,
            unfocusedBorderColor = outlineTextFieldUnfocusedBorderColor,
            cursorColor = outlineTextFieldCursorColor
        ),
        singleLine = true,

        visualTransformation = visualTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}


fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)(@)(.+)(\\.)(.+)"
    return email.matches(emailRegex.toRegex())
}