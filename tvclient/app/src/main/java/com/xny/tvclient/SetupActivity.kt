package com.xny.tvclient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xny.tvclient.manager.ConfigManager
import com.xny.tvclient.utils.DeviceUtils

/**
 * 首次启动 / 重新配置界面
 *
 * - 展示 URL（必填）：TV 全屏显示的网址，无需后端服务即可运行
 * - 服务器地址（可选）：填写后可接受远程控制指令；留空则纯本地运行
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etDisplayUrl = findViewById<EditText>(R.id.etDisplayUrl)
        val etWsUrl = findViewById<EditText>(R.id.etWsUrl)
        val etDeviceName = findViewById<EditText>(R.id.etDeviceName)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvDeviceInfo = findViewById<TextView>(R.id.tvDeviceInfo)

        // 显示设备信息
        val deviceId = DeviceUtils.getDeviceId(this)
        tvDeviceInfo.text = buildString {
            append("设备ID: $deviceId\n")
            append("型号: ${DeviceUtils.getDeviceModel()}\n")
            append("系统: ${DeviceUtils.getAndroidVersion()}\n")
            append("MAC: ${DeviceUtils.getMacAddress()}\n")
            append("IP: ${DeviceUtils.getIpAddress(this@SetupActivity)}")
        }

        // 恢复已有配置
        etDisplayUrl.setText(ConfigManager.getDisplayUrl(this).let {
            if (it == "about:blank") "" else it
        })
        etWsUrl.setText(ConfigManager.getWsUrl(this))
        etDeviceName.setText(ConfigManager.getDeviceName(this))
        etLocation.setText(ConfigManager.getLocation(this))

        btnSave.setOnClickListener {
            val displayUrl = etDisplayUrl.text.toString().trim()
            val wsUrl = etWsUrl.text.toString().trim()

            // 展示 URL 必填
            if (displayUrl.isBlank()) {
                Toast.makeText(this, "请输入展示网址", Toast.LENGTH_SHORT).show()
                etDisplayUrl.requestFocus()
                return@setOnClickListener
            }
            if (!displayUrl.startsWith("http://") && !displayUrl.startsWith("https://")) {
                Toast.makeText(this, "展示网址格式错误，应以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
                etDisplayUrl.requestFocus()
                return@setOnClickListener
            }

            // 服务器地址可选，填写时校验格式
            if (wsUrl.isNotBlank() && !wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) {
                Toast.makeText(this, "服务器地址格式错误，应以 ws:// 开头（或留空）", Toast.LENGTH_SHORT).show()
                etWsUrl.requestFocus()
                return@setOnClickListener
            }

            ConfigManager.setDisplayUrl(this, displayUrl)
            ConfigManager.setWsUrl(this, wsUrl)
            ConfigManager.setDeviceName(this, etDeviceName.text.toString().trim())
            ConfigManager.setLocation(this, etLocation.text.toString().trim())
            ConfigManager.setSetupDone(this, true)

            val mode = if (wsUrl.isBlank()) "本地模式" else "已连接服务器"
            Toast.makeText(this, "配置已保存（$mode），启动中...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
