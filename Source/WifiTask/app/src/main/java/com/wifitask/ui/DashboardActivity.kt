package com.wifitask.ui

import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wifitask.App
import com.wifitask.R
import com.wifitask.databinding.ActivityDashboardBinding
import com.wifitask.utils.FileManager
import com.wifitask.utils.Session
import io.reactivex.disposables.CompositeDisposable
import rx.wifi.android.RxWifi
import com.wifitask.utils.memoized

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val disposable = CompositeDisposable()
    private val session: Session by lazy { Session(applicationContext) }
    private lateinit var fileManager: FileManager

    private val password: String? by memoized {
        intent?.getStringExtra(
            "password"
        )
    }

    private val scanResult: ScanResult? by memoized {
        intent?.getParcelableExtra(
            "scanResult"
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        fileManager = FileManager().apply { context = this@DashboardActivity }
        binding.btnSettings.setOnClickListener {
            Intent(this@DashboardActivity, SettingsActivity::class.java).also { startActivity(it) }
        }

        binding.btnUpdateBroStatus.setOnClickListener {
           val status = fileManager.getFileFromAssets(fileName = "message.txt")
            if(session.getBroStatus() == 1) {
                binding.txtBroStatus.text = getString(R.string.bro_status_message, status.message)
            } else {
                binding.txtBroStatus.text = getString(R.string.bro_status_battery, status.battery)
            }
        }

        RxWifi.states(this).subscribe {
            when (it) {
                WifiManager.WIFI_STATE_DISABLED -> {
                    binding.txtStatus.text = getString(R.string.status_disable)
                    binding.btnConnect.isEnabled = false
                    binding.btnUpdateBroStatus.isEnabled = false
                }
                WifiManager.WIFI_STATE_ENABLED -> {
                    binding.txtStatus.text = getString(R.string.status_enable)
                    binding.btnConnect.isEnabled = true
                    binding.btnUpdateBroStatus.isEnabled = true
                }
            }
        }.let { disposable.add(it) }

        RxWifi.supplicantStates(this).subscribe {
            when (it.name) {
                "DISCONNECTED",
                "COMPLETED" -> {
                    onCheckSSIDConnected()
                }
                else -> {
                    binding.txtStatus.text = getString(R.string.status_message, it.name)
                }
            }
        }.let { disposable.add(it) }
    }

    private fun onCheckSSIDConnected() {
        scanResult?.let { scanResult ->
            binding.txtSelectedWifi.text = scanResult.SSID
            val isConnected = RxWifi.isConnected(this, scanResult.SSID)
            if (isConnected) {
                binding.txtStatus.text = getString(R.string.status_connected)
                binding.btnConnect.text = getString(R.string.disconnect_CTA)
                binding.btnUpdateBroStatus.isEnabled = true
            } else {
                binding.txtStatus.text = getString(R.string.status_disconnected)
                binding.btnConnect.text = getString(R.string.connect_CTA)
                binding.btnUpdateBroStatus.isEnabled = false
            }

            binding.btnConnect.setOnClickListener {
                onConnectToWifiDevice()
            }
        }
    }

    private fun onConnectToWifiDevice() {
        if (scanResult != null && !password.isNullOrEmpty()) {
            val isConnected = RxWifi.isConnected(this, scanResult!!.SSID)
            if (!isConnected) {
                App.instance.wifiClient.connectToWifi(scanResult!!.SSID, password!!) {
                    runOnUiThread {
                        binding.txtStatus.text = getString(R.string.status_message, it)
                    }
                }
            } else {
                App.instance.wifiClient.disconnectWifi()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        App.instance.wifiClient.dispose()
    }
}