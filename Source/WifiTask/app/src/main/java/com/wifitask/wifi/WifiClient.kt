package com.wifitask.wifi

import android.Manifest.permission.*
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import rx.wifi.android.RxWifi
import rx2.receiver.android.RxReceiver


class WifiClient {

    private val TAG = "WifiClient"
    lateinit var context: Context
    lateinit var wifiManager: WifiManager
    lateinit var networkCallback: ConnectivityManager.NetworkCallback
    lateinit var connectivityManager: ConnectivityManager
    private val disposable = CompositeDisposable()

    fun isWifiEnabled(): Boolean {
        wifiManager = context.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    @NonNull
    @RequiresPermission(anyOf = [ACCESS_WIFI_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    @CheckReturnValue
    fun scan(): Observable<List<ScanResult>> {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        return RxReceiver.receives(context, intentFilter)
            .filter { intent ->
                intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false
                )
            }
            .map { wifiManager.scanResults }
            .startWith(wifiManager.scanResults)
    }

    fun disconnectWifi() {
        if (context.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.e(
                TAG,
                "Missing <ACCESS_FINE_LOCATION> permission. Required to obtain the Wifi SSID."
            )
            return
        }

        wifiManager = context.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectivityManager?.run {
                unregisterNetworkCallback(networkCallback)
            }
        } else {
            wifiManager.disconnect()
        }
    }

    fun connectToWifi(ssid: String, password: String, onCallBack: (it: String) -> Unit) {
        wifiManager = context.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            RxWifi.connects(context, ssid, password).subscribe(
                {
                    onCallBack("CONNECTED")
                }, {
                    onCallBack("ERROR - onUnavailable")
                }
            ).let { disposable.add(it) }
        } else {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    createNetworkRoute(network, connectivityManager)
                    onCallBack("CONNECTED")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    onCallBack("ERROR - onLost")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    onCallBack("ERROR - onUnavailable")
                }
            }
            connectivityManager.requestNetwork(request, networkCallback)
        }
    }

    private fun createNetworkRoute(network: Network, connectivityManager: ConnectivityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(network)
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network)
        }
    }

    fun dispose() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        disposable.dispose()
    }
}