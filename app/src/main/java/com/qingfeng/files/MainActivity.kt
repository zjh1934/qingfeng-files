package com.qingfeng.files

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import com.qingfeng.files.base.BaseActivity
import com.qingfeng.files.ui.browse.BrowseActivity

/**
 * 入口页：只做一件事 —— 确认有文件访问权限，然后进浏览页。
 *
 * 借鉴点：参照物用「闪屏/路由页」承担初始化与跳转，主页只管展示。
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.tvStatus)
        val button = findViewById<Button>(R.id.btnAction)

        refresh(statusText, button)

        button.setOnClickListener {
            if (hasStorageAccess()) {
                enterBrowse()
            } else {
                requestStorageAccess()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.tvStatus)
        val button = findViewById<Button>(R.id.btnAction)
        refresh(statusText, button)
        // 授权回来后自动进入
        if (hasStorageAccess()) {
            enterBrowse()
        }
    }

    private fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun refresh(status: TextView, button: Button) {
        if (hasStorageAccess()) {
            status.text = "已获得文件访问权限"
            button.text = "进入清风文件"
        } else {
            status.text = "需要「所有文件访问权限」才能浏览手机里的文件。\n" +
                    "这是 Android 11 以上系统的要求，点下面的按钮去开启。"
            button.text = "去开启权限"
        }
    }

    private fun enterBrowse() {
        startActivity(Intent(this, BrowseActivity::class.java))
        finish()
    }
}
