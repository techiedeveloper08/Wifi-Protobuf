package com.wifitask.utils

import android.content.Context
import android.content.SharedPreferences
import com.wifitask.utils.Session.Key.APP_BRO_STATUS

class Session(context: Context) {

    private val appPrefName = "app_wifi"
    private val privateMode = 0
    private val appPref: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        appPref = context.getSharedPreferences(appPrefName, privateMode)
        editor = appPref.edit()
    }

    fun setBroStatus(broStatus: Int) {
        editor.putInt(APP_BRO_STATUS, broStatus)
        editor.apply()
    }

    fun getBroStatus(): Int {
        return appPref.getInt(APP_BRO_STATUS, 1)
    }

    object Key {
        internal const val APP_BRO_STATUS = "app_bro_status"
    }
}