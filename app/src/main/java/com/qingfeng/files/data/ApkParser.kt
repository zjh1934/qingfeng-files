package com.qingfeng.files.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.qingfeng.files.AppContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * APK 解析器。
 *
 * 借鉴点（实测参照物 `res/layout/bg.xml`）：
 * - 显示 **Sign MD5**（`ajp`）—— 签名指纹，判断来源可信度
 * - 区分 Version 与 **Target Version**（API 级别）
 * - 权限列表 + **敏感权限**标注（`db`/`bw`/`c7`）
 *
 * 实现：用 `PackageManager.getPackageArchiveInfo()` 读未安装的 APK。
 */
class ApkParser {

    fun parse(file: File): ApkInfo? {
        val pm: PackageManager = AppContext.get().packageManager
        val info: PackageInfo = readArchiveInfo(pm, file) ?: return null

        // 让系统能找到 APK 里的资源（否则取不到应用名）
        val appInfo = info.applicationInfo
        if (appInfo != null) {
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
        }

        val pkg: String = info.packageName ?: return null
        val label: String = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg

        val perms: List<String> = info.requestedPermissions?.toList() ?: emptyList()

        val sig = readSignatureBytes(file)
        return ApkInfo(
            filePath = file.absolutePath,
            appName = label,
            packageName = pkg,
            versionName = info.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            },
            targetSdk = appInfo?.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo?.minSdkVersion
            } else null,
            size = file.length(),
            permissions = perms,
            signMd5 = sig?.let { digest("MD5", it) },
            signSha1 = sig?.let { digest("SHA-1", it) },
            signSha256 = sig?.let { digest("SHA-256", it) }
        )
    }

    @Suppress("DEPRECATION")
    private fun readArchiveInfo(pm: PackageManager, file: File): PackageInfo? {
        val flags = PackageManager.GET_PERMISSIONS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            pm.getPackageArchiveInfo(file.absolutePath, flags)
        }
    }

    /** 从 APK 内签名文件取字节，用于算指纹 */
    private fun readSignatureBytes(file: File): ByteArray? {
        return try {
            ZipFile(file).use { zip ->
                val target = zip.entries().toList().firstOrNull {
                    it.name.startsWith("META-INF/") &&
                        (it.name.endsWith(".RSA") || it.name.endsWith(".DSA") ||
                            it.name.endsWith(".EC"))
                } ?: return null
                zip.getInputStream(target).readBytes()
            }
        } catch (e: Exception) { null }
    }

    private fun digest(alg: String, b: ByteArray): String =
        MessageDigest.getInstance(alg).digest(b)
            .joinToString(":") { "%02X".format(it) }
}
