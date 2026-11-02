package com.qingfeng.files.data

/**
 * APK 信息模型。
 *
 * 借鉴点：实测参照物 APK 详情页（`res/layout/bg.xml`）的字段顺序：
 *   1. 应用名（app_name_text）
 *   2. Version            ← property_version
 *   3. 新版本（row_new_version，可隐藏）
 *   4. Size               ← property_size
 *   5. Package Name       ← property_pname_text
 *   6. Target Version     ← property_target_version_text（API 级别）
 *   7. **Sign MD5**       ← property_sign_md5
 *   8. Permission（按钮） ← permissions
 *   9. Uninstall（按钮）  ← uninstall
 *
 * 另有：`cq`="First installed time: %s"、`cz`="Last installed time: %s"
 * （已安装应用才有）
 *
 * ⭐ 最有价值的是 **Sign MD5** —— 用签名指纹判断来源是否可信，
 * 手搓绝对想不到要显示这个。
 */
data class ApkInfo(
    val filePath: String,
    val appName: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val targetSdk: Int?,
    val minSdk: Int?,
    val size: Long,
    val permissions: List<String>,
    val signMd5: String?,
    val signSha1: String?,
    val signSha256: String?,
    val firstInstallTime: Long? = null,
    val lastUpdateTime: Long? = null,
    /** 是否是系统应用（已安装时） */
    val isSystemApp: Boolean = false
)

/** 敏感权限（参照物 `db`="Sensitive Permissions"、`bw` 文案） */
object SensitivePermissions {
    private val SENSITIVE = setOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.GET_ACCOUNTS,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.WRITE_CALL_LOG,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.REQUEST_DELETE_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    fun filter(perms: List<String>): List<String> = perms.filter { it in SENSITIVE }

    /** 简单描述（不引额外依赖） */
    fun describe(p: String): String = when {
        p.contains("SMS") -> "短信（可读取或发送短信）"
        p.contains("CONTACTS") -> "联系人"
        p.contains("ACCOUNTS") -> "账号"
        p.contains("LOCATION") -> "位置"
        p.contains("RECORD_AUDIO") -> "录音"
        p.contains("CAMERA") -> "相机"
        p.contains("CALL_LOG") -> "通话记录"
        p.contains("CALL_PHONE") -> "直接拨打电话"
        p.contains("READ_PHONE_STATE") -> "设备识别信息"
        p.contains("STORAGE") -> "存储空间（可读写文件）"
        p.contains("QUERY_ALL_PACKAGES") -> "读取全部已安装应用列表"
        p.contains("USAGE_STATS") -> "读取你的应用使用情况"
        p.contains("ACCESSIBILITY") -> "无障碍服务（可读取屏幕内容）"
        p.contains("DELETE_PACKAGES") -> "卸载其他应用"
        p.contains("SYSTEM_ALERT_WINDOW") -> "悬浮窗（可覆盖其他应用）"
        else -> p.substringAfterLast('.')
    }
}
