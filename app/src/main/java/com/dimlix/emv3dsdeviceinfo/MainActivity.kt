package com.dimlix.emv3dsdeviceinfo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.dimlix.emvdeviceinfo.DeviceInfoUtil

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.action_button).setOnClickListener {
            findViewById<TextView>(R.id.data_textview).text = DeviceInfoUtil.getDeviceInfo(this)
        }
    }
}