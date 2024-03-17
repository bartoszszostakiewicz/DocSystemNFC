package com.docsysnfc.sender.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.CreateAccountState
import com.docsysnfc.sender.model.NFCSysScreen
import com.docsysnfc.sender.ui.theme.backgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(navController: NavController, viewModel: MainViewModel, context: Context) {

    // Remember the state of the email and password input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }
    var isEmailValid by remember { mutableStateOf(false) }

    // Determine if the form is valid
    LaunchedEffect(email, password, repeatPassword) {


        isFormValid = (password == repeatPassword && isEmailValid && password.length >= 6 && password.contains(Regex(".*[0-9].*")))



    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = backgroundColor)) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(id = R.string.create_account), style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(8.dp))

            CustomTextField(
                text = email,
                onTextChange = {
                    email = it
                    isEmailValid = isValidEmail(email)
                },
                label = stringResource(id = R.string.email),
                isTextValid = isEmailValid,
                visualTransformation = VisualTransformation.None // Normal text field for email
            )

          
//            Spacer(modifier = Modifier.height(8.dp))

            CustomTextField(
                text = password,
                onTextChange = { password = it },
                label = stringResource(id = R.string.password),
                visualTransformation = PasswordVisualTransformation(),
            )

//            Spacer(modifier = Modifier.height(8.dp))

            CustomTextField(
                text = repeatPassword,
                onTextChange = { repeatPassword = it },
                label = stringResource(id = R.string.repeat_password),
                visualTransformation = PasswordVisualTransformation(),
            )

//            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid) {

                    viewModel.createAccount(email, password)
                        // Optionally navigate to a different screen after account creation
                        // navController.navigate("next_screen_route")
                        if (viewModel.createAccountState.value == CreateAccountState.SUCCESS) {
                            navController.navigate(NFCSysScreen.Home.name)
                        }

                        //and add information about the account creation
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text(stringResource(id = R.string.create_account))
            }

            // Consider adding additional UI elements like terms and conditions, privacy policy, etc.
        }
    }
}

