package com.wifitask.ui


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.wifitask.App
import com.wifitask.R
import com.wifitask.adapter.WifiAdapter
import com.wifitask.databinding.ActivityMainBinding
import io.reactivex.disposables.CompositeDisposable
import rx.wifi.android.RxWifi


class MainActivity : AppCompatActivity() {

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1010
    }

    private lateinit var binding: ActivityMainBinding
    private var wifiAdapter: WifiAdapter? = null
    private var scanResultList: ArrayList<ScanResult> = ArrayList()
    private val disposable = CompositeDisposable()
    private val scanningDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        wifiAdapter = WifiAdapter()
        with(binding.rvWiFiList) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = wifiAdapter
        }
        binding.btnConnect.isEnabled = false

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    onLocationPermission()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        MY_PERMISSIONS_REQUEST_LOCATION
                    )
                }
                else -> {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        MY_PERMISSIONS_REQUEST_LOCATION
                    )
                }
            }
            return
        }

        onConnectionListener()

        binding.btnConnect.setOnClickListener {
            onConnectToWifiDevice()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun onLocationPermission() {
        val foreground = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (foreground) {
            val background = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (background) {
                onConnectionListener()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private fun onConnectionListener() {
        RxWifi.states(this).subscribe {
            when (it) {
                WifiManager.WIFI_STATE_DISABLED -> {
                    binding.txtStatus.text = getString(R.string.status_disable)
                    binding.btnConnect.isEnabled = false
                }
                WifiManager.WIFI_STATE_ENABLED -> {
                    binding.txtStatus.text = getString(R.string.status_enable)
                    binding.btnConnect.isEnabled = true
                    onScanWiFi()
                }
            }
        }.let { disposable.add(it) }
    }

    private fun onConnectToWifiDevice() {
        val scanResult = wifiAdapter?.getSelectedItem()
        val password = binding.etPassword.text.toString()
        if (scanResult != null && password.isNotEmpty()) {
            App.instance.wifiClient.connectToWifi(scanResult.SSID, password) {
                runOnUiThread {
                    binding.txtStatus.text = getString(R.string.status_message, it)
                    if (it == "CONNECTED") {
                        Intent(this@MainActivity, DashboardActivity::class.java).apply {
                            putExtra("scanResult", scanResult)
                            putExtra("password", password)
                        }.also { startActivity(it) }
                    }
                }
            }
        } else if (password.isEmpty()) {
            binding.txtStatus.text = getString(R.string.status_password)
        }
    }

    private fun onScanWiFi() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        binding.txtStatus.text = getString(R.string.status_scanning)
        App.instance.wifiClient.scan().subscribe {
            if (!it.isNullOrEmpty() && scanResultList.size != it.size) {
                scanResultList.clear()
                scanResultList.addAll(it)
                wifiAdapter?.setData(scanResultList)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.txtStatus.text = getString(R.string.status_stop_scanning)
                    scanningDisposable.dispose()
                }, 10000)
            }
        }.let { scanningDisposable.add(it) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
                var foreground = false
                var background = false
                for (i in permissions.indices) {
                    if (permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION) {
                        //foreground permission allowed
                        if (grantResults[i] >= 0) {
                            foreground = true
                            Toast.makeText(applicationContext, "Foreground location permission allowed", Toast.LENGTH_SHORT).show()
                            continue
                        } else {
                            Toast.makeText(applicationContext, "Location Permission denied", Toast.LENGTH_SHORT).show()
                            break
                        }
                    }
                    if (permissions[i] == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                        if (grantResults[i] >= 0) {
                            foreground = true
                            background = true
                            Toast.makeText(applicationContext, "Background location location permission allowed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, "Background location location permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (foreground) {
                    if (background) {
                        onConnectionListener()
                    } else {
                        onLocationPermission()
                    }
                }
            }
        } else {
            when (requestCode) {
                MY_PERMISSIONS_REQUEST_LOCATION -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onConnectionListener()
                    } else {
                        binding.txtStatus.text = getString(R.string.please_enable_location)
                        Toast.makeText(
                            this,
                            getString(R.string.please_enable_location),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        scanningDisposable.dispose()
        App.instance.wifiClient.dispose()
    }
}