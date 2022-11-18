package com.liyobor.android_wifi_direct

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        //for other device how are able to connect with Ethernet
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        //for check internet over Bluetooth
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }
}

fun getCurrentTime(): String {

    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    return current.format(formatter)

}

fun setUUID(context: Context, uuid: String) {
    val pref = context.getSharedPreferences("User", Context.MODE_PRIVATE)
    val editor = pref.edit()
    editor.putString("uuid", uuid).apply()
}

fun getUUID(context: Context): String {
    val pref = context.getSharedPreferences("User", Context.MODE_PRIVATE)
    val uuidString = pref.getString("uuid", "NULL") ?: "NULL"
    return if (uuidString == "NULL") {
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        setUUID(context, uuid)
        uuid
    } else {
        uuidString
    }
}