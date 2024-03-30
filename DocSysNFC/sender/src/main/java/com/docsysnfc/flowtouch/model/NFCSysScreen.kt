package com.docsysnfc.flowtouch.model

import androidx.annotation.StringRes
import com.docsysnfc.R

enum class NFCSysScreen(@StringRes val title: Int) {
    Home(title = R.string.send),
    Send(title = R.string.send_details),
    Receive(title = R.string.receive),
    Login(title = R.string.login_screen),
    Create(title = R.string.create_screen),
    Recovery(title = R.string.recovery_screen),
    TrustedDevices(title = R.string.trusted_devices),
    Settings(title = R.string.settings),
    ShareKeyScreen(title = R.string.share_key_screen),
}
