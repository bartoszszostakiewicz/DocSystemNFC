package com.docsysnfc.sender.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.docsysnfc.R
import com.docsysnfc.sender.MainViewModel
import com.docsysnfc.sender.model.AuthenticationState
import com.docsysnfc.sender.model.NFCSysScreen
import com.docsysnfc.sender.ui.theme.backgroundColor
import com.docsysnfc.sender.ui.theme.buttonsColor
import com.docsysnfc.sender.ui.theme.deleteButtonsColor
import com.docsysnfc.sender.ui.theme.outlineTextFieldCursorColor
import com.docsysnfc.sender.ui.theme.outlineTextFieldFocusedBorderColor
import com.docsysnfc.sender.ui.theme.outlineTextFieldUnfocusedBorderColor
import com.docsysnfc.sender.ui.theme.textColor
import com.docsysnfc.sender.ui.theme.tilesColor
import com.docsysnfc.sender.ui.theme.whiteColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: MainViewModel, context: Context) {

    val authenticationState by viewModel.authenticationState.collectAsState()

    if (authenticationState == AuthenticationState.FAILURE || authenticationState == AuthenticationState.UNKNOWN) {
        navController.navigate(NFCSysScreen.Login.name)
    }

//    val isEncryptionEnabled = viewModel.isEncryptionEnabled.collectAsState()

    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        var password by remember { mutableStateOf("") }
        var isReauthenticationFailed by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(stringResource(id = R.string.delete_account_confirm)) },
            text = {
                Column {
                    Text(stringResource(id = R.string.delete_account_desc))
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                stringResource(id = R.string.password),
                                color = textColor
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = outlineTextFieldFocusedBorderColor,
                            unfocusedBorderColor = outlineTextFieldUnfocusedBorderColor,
                            cursorColor = outlineTextFieldCursorColor,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        viewModel.reauthenticateAndDelete(password) { success ->
                            if (success) {
                                showDialog.value = false
                                Toast.makeText(
                                    context, // make sure 'context' is available in your composable
                                    context.getString(R.string.delete_account_success),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context, // make sure 'context' is available in your composable
                                    context.getString(R.string.wrong_password),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(deleteButtonsColor),
                    enabled = password.isNotEmpty()
                ) {
                    Text(stringResource(id = R.string.delete_confirm))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog.value = false },
                    colors = ButtonDefaults.buttonColors(
                        buttonsColor,
                        contentColor = whiteColor
                    ),
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HomeScreenTopBar(
                title = stringResource(id = R.string.settings_title),
                navController = NavController(context)
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .background(backgroundColor)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {


                    Setting(
                        viewModel = viewModel,
                        informationText = stringResource(id = R.string.additional_encryption_info)
                    )

                    Setting(
                        viewModel = viewModel,
                        informationText = stringResource(id = R.string.additional_encryption_info)
                    )

                    Setting(
                        viewModel = viewModel,
                        informationText = stringResource(id = R.string.additional_encryption_info)
                    )

                    Setting(
                        viewModel = viewModel,
                        informationText = stringResource(id = R.string.additional_encryption_info)
                    )

                    Setting(
                        viewModel = viewModel,
                        informationText = stringResource(id = R.string.additional_encryption_info)
                    )



                    // Delete Account Button

                    Button(
                        onClick = { showDialog.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(deleteButtonsColor)
                    ) {
                        Text(stringResource(id = R.string.delete_account))
                    }
                }
            }
        }
    }
}

@Composable
fun Setting(
    informationText: String = "This is a setting",
    viewModel: MainViewModel
){

    var expanded by remember { mutableStateOf(false) }
    val sizeOfCornerRadius = 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(tilesColor, shape = RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Additional Encryption",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    expanded = !expanded
                }
                .padding(16.dp),
        )

        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                title = {
                    Text("Additional Encryption")
                },
                text = {
                    Text(informationText)
                },
                confirmButton = {
                    Button(
                        onClick = { expanded = false },
                        colors = ButtonDefaults.buttonColors(buttonsColor)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        Switch(
            modifier = Modifier.padding(16.dp),
            checked = true,
            onCheckedChange = { enabled ->
                // viewModel.setEncryptionEnabled(enabled)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = whiteColor,
                checkedTrackColor = buttonsColor,
                uncheckedThumbColor = whiteColor,
                uncheckedTrackColor = deleteButtonsColor
            )
        )

    }

}

