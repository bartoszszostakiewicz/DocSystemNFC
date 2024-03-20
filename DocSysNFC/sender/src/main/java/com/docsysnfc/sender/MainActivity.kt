package com.docsysnfc.sender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.docsysnfc.sender.model.NFCSysScreen
import com.docsysnfc.sender.model.NFCtest
import com.docsysnfc.sender.ui.AppNavigation
import kotlinx.coroutines.launch


private const val TAG = "NFC123"


val REQUEST_CODE = 1

class MainActivity : ComponentActivity() {

    val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")

        val SDK_INT = Build.VERSION.SDK_INT
        if (SDK_INT > 8) {
            val policy = ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            //your codes here
        }

        val destinationName = intent.getStringExtra("destinationId")
        val destScreen = destinationName?.let { NFCSysScreen.valueOf(it) }

        if (destScreen != null) {
            viewModel.setNavigationDestination(destScreen)
        }





        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
        }



        lifecycleScope.launch {
            viewModel.activeURL.collect { ndefMessage ->
                ndefMessage.let {
                    startNFCService(it)
                    Log.d("NFC1234", "Payload:  $ndefMessage")
                }
            }
        }

        setContent {
            AppNavigation(viewModel = viewModel , context = this@MainActivity)
        }
    }

    private fun startNFCService(ndefMessage: String) {
        val intent = Intent(this, NFCtest::class.java).apply {
            putExtra("ndefMessage", ndefMessage)
        }
        startService(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        stopNFCService()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        viewModel.setActivityVisibility(true)
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putBoolean("isActivityVisible", true)
            apply()
        }

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
        viewModel.setActivityVisibility(false)
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putBoolean("isActivityVisible", false)
            apply()
        }

    }

    private fun stopNFCService() {
        val intent = Intent(this, NFCtest::class.java)
        stopService(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                    if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        // Uprawnienia zostały przyznane. Możesz kontynuować zapis do katalogu Downloads.
                    } else {
                        // Uprawnienia zostały odrzucone. Musisz to obsłużyć.
                        Toast.makeText(
                            this,
                            "Uprawnienia zapisu zostały odrzucone.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }
            }
            else -> {
                // Inne przypadki, które mogłeś poprosić o uprawnienia.
                return
            }
            // Sprawdź inne uprawnienia, które mogłeś poprosić.
        }
    }
}




