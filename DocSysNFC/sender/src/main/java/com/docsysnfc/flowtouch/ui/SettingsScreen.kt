package com.docsysnfc.flowtouch.ui

import android.content.Context
import android.util.Log
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.docsysnfc.R
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.ui.theme.backgroundColor
import com.docsysnfc.flowtouch.ui.theme.buttonsColor
import com.docsysnfc.flowtouch.ui.theme.deleteButtonsColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldCursorColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldFocusedBorderColor
import com.docsysnfc.flowtouch.ui.theme.outlineTextFieldUnfocusedBorderColor
import com.docsysnfc.flowtouch.ui.theme.textColor
import com.docsysnfc.flowtouch.ui.theme.tilesColor
import com.docsysnfc.flowtouch.ui.theme.whiteColor


@Composable
fun SettingsScreen(navController: NavHostController, viewModel: MainViewModel, context: Context) {


    val uiState by viewModel.uiState.collectAsState()

    val showDialog = remember { mutableStateOf(false) }

    val additionalEncryption = uiState.additionalEncryption
    val cloudMirroring = uiState.cloudMirroring

    if (showDialog.value) {

        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { /*showDialog.value = false*/ },
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = outlineTextFieldFocusedBorderColor,
                            unfocusedBorderColor = outlineTextFieldUnfocusedBorderColor,
                            cursorColor = outlineTextFieldCursorColor
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
                navController = navController,
                viewModel = viewModel
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

                    Spacer(modifier = Modifier.height(32.dp))


                    Setting(
                        title = stringResource(id = R.string.additional_encryption),
                        informationText = stringResource(id = R.string.additional_encryption_info),
                        action = {
                            viewModel.toggleEncryption()
                            Log.d("nfc123", "switched $additionalEncryption")
                        },
                        check = additionalEncryption,
                    )

                    Setting(
                        title = stringResource(id = R.string.cloud_mirroring),
                        informationText = stringResource(id = R.string.additional_cloud_mirroring_info),
                        action = {
                            viewModel.toggleMirroring()
                        },
                        check = cloudMirroring,
                    )

                    Setting(
                        title = stringResource(id = R.string.auto_clear_cloud),
                        informationText = stringResource(id = R.string.additional_encryption_info),
                        action = {

                        }
                    )

                    Setting(
                        title = stringResource(id = R.string.integrity_check),
                        informationText = stringResource(id = R.string.integrity_check_info),
                        action = {
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))



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
    title: String = stringResource(id = R.string.setting_title),
    informationText: String = stringResource(id = R.string.setting_info),
    action: () -> Unit = {},
    check: Boolean = false
) {

    var checked by remember { mutableStateOf(check) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(tilesColor, shape = RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
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
                onDismissRequest = { /*expanded = false*/ },
                title = {
                    Text(stringResource(id = R.string.additional_encryption))
                },
                text = {
                    Text(informationText)
                },
                confirmButton = {
                    Button(
                        onClick = { expanded = false },
                        colors = ButtonDefaults.buttonColors(buttonsColor)
                    ) {
                        Text(stringResource(id = R.string.ok))
                    }
                }
            )
        }
        Switch(
            modifier = Modifier.padding(16.dp),
            checked = checked,
            onCheckedChange = {
                action()
                checked = it
            },

            colors = SwitchDefaults.colors(
                checkedThumbColor = buttonsColor,
                checkedTrackColor = whiteColor,
                uncheckedThumbColor = whiteColor,
                uncheckedTrackColor = buttonsColor
            )
        )

    }

}

