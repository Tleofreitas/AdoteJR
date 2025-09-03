package com.example.adotejr

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.work.Configuration

class MeuApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() {
            val builder = Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setDefaultProcessName(packageName)
            }

            return builder.build()
        }
}