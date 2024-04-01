package com.docsysnfc.flowtouch

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCSysScreen
import com.docsysnfc.flowtouch.model.NFCComm
import com.docsysnfc.flowtouch.ui.AppNavigation
import kotlinx.coroutines.launch
import java.util.Locale


private const val TAG = "NFC123"


const val REQUEST_CODE = 1

fun setLocale(context: Context, localeCode: String) {
    val locale = Locale(localeCode)
    Locale.setDefault(locale)
    val resources = context.resources
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
}


class MainActivity : ComponentActivity() {

    val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")


        setLocale(this, "pl")

        hideStatusBar()

        val SDK_INT = Build.VERSION.SDK_INT
        if (SDK_INT > 8) {
            val policy = ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)

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
        val intent = Intent(this, NFCComm::class.java).apply {
            putExtra("ndefMessage", ndefMessage)
        }
        startService(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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
        val intent = Intent(this, NFCComm::class.java)
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

    private fun hideStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
}




