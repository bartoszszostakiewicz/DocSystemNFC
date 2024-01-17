package com.P2PNFC

class NativeLib {

    /**
     * A native method that is implemented by the 'P2PNFC' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'P2PNFC' library on application startup.
        init {
            System.loadLibrary("P2PNFC")
        }
    }
}