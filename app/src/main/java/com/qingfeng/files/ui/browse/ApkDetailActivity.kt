package com.qingfeng.files.ui.browse

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qingfeng.files.R
import com.qingfeng.files.base.BaseActivity
import com.qingfeng.files.data.ApkInfo
import com.qingfeng.files.data.ApkParser
import com.qingfeng.files.data.SensitivePermissions
import com.qingfeng.files.util.FileFormat
import java.io.File

/**
 * APK 详情页。
 *
 * 字段顺序严格照参照物 `res/layout/bg.xml`：
 * 应用名 → Version → Size → Package Name → **Target Version** → **Sign MD5**
 * → Permission（按钮） → Uninstall（按钮）
 */
class ApkDetailActivity : BaseActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apk_detail)
        container = findViewById(R.id.apkInfoList)

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path == null) { finish(); return }

        val file = File(path)
        val info = ApkParser().parse(file)
        if (info == null) {
            Toast.makeText(this, "解析失败，可能不是有效的 APK", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        render(info)
    }

    private fun render(info: ApkInfo) {
        findViewById<TextView>(R.id.tvApkName).text = info.appName
        container.removeAllViews()

        row("Version", info.versionName ?: "未知")
        row("Version Code", info.versionCode.toString())
        row("Size", FileFormat.size(info.size))
        row("Package Name", info.packageName)
        row("Target Version", info.targetSdk?.let { "API $it" } ?: "未知")
        info.minSdk?.let { row("Min Version", "API $it") }
        row("Sign MD5", info.signMd5 ?: "未取到")
        info.signSha1?.let { row("Sign SHA-1", it) }

        // 敏感权限（参照物 db/bw/c7）
        val sensitive = SensitivePermissions.filter(info.permissions)
        if (sensitive.isNotEmpty()) {
            val btn = buttonRow("敏感权限（${sensitive.size}）", "查看") {
                MaterialAlertDialogBuilder(this)
                    .setTitle("敏感权限 ${sensitive.size} 项")
                    .setMessage(sensitive.joinToString("\n") { "• " + SensitivePermissions.describe(it) })
                    .setPositiveButton("关闭", null)
                    .show()
            }
            container.addView(btn)
        }

        container.addView(buttonRow("权限（${info.permissions.size}）", "查看") {
            MaterialAlertDialogBuilder(this)
                .setTitle("申请的权限 ${info.permissions.size} 项")
                .setMessage(info.permissions.joinToString("\n") { "• " + it.substringAfterLast('.') })
                .setPositiveButton("关闭", null)
                .show()
        })

        container.addView(buttonRow("安装", "安装") { installApk(info.filePath) })
    }

    private fun row(label: String, value: String) {
        val v = layoutInflater.inflate(R.layout.item_info_row, container, false)
        v.findViewById<TextView>(R.id.tvLabel).text = label
        v.findViewById<TextView>(R.id.tvValue).text = value
        container.addView(v)
    }

    private fun buttonRow(label: String, btn: String, onClick: () -> Unit): View {
        val v = layoutInflater.inflate(R.layout.item_info_row, container, false)
        v.findViewById<TextView>(R.id.tvLabel).text = label
        val value = v.findViewById<TextView>(R.id.tvValue)
        value.text = btn
        value.setTextColor(resources.getColor(R.color.accent, theme))
        v.setOnClickListener { onClick() }
        container.addView(v)
        return v
    }

    private fun installApk(path: String) {
        try {
            val file = File(path)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动安装：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_PATH = "path"
    }
}
