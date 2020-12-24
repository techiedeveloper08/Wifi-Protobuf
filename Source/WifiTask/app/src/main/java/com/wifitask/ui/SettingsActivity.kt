package com.wifitask.ui

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wifitask.databinding.ActivitySettingsBinding
import com.wifitask.utils.Session


class SettingsActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val session: Session  by lazy { Session(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.btnSelectOtherWiFi.setOnClickListener {
            Intent(this@SettingsActivity, MainActivity::class.java).also { startActivity(it) }.also { finishAffinity() }
        }

        binding.buttonGroupBroStatus.setPosition(session.getBroStatus(), true)
        binding.buttonGroupBroStatus.setOnPositionChangedListener {
            session.setBroStatus(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> finish()
        }
        return true
    }
}