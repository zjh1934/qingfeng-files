package com.qingfeng.files.data

import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * APK 解析器。
 *
 * 借鉴点（实测参照物）：
 * - 显示 **Sign MD5**（`ajp`）—— 签名指纹，判断来源可信度
 * - 区分 Version 与 **Target Version**（API 级别）
 * - 权限列表 + **敏感权限**标注（`db`/`bw`/`c7`）
 * - 已安装应用额外显示首次/最后安装时间（`cq`/`cz`）
 *
 * 实现：Android 10+ 用 `PackageManager.getPackageArchiveInfo`，
 * 签名指纹从 APK 里的签名文件算。
 */
class ApkParser {

    fun parse(file: File): ApkInfo? {
        val pm = AppContext.get().packageManager
        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(
                    (PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES).toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
            )
        } ?: return null

        info.applicationInfo?.let {
            it.sourceDir = file.absolutePath
            it.publicSourceDir = file.absolutePath
        }

        val pkg = info.packageName
        val label = info.applicationInfo?.let {
            pm.getApplicationLabel(it).toString()
        } ?: pkg

        val perms = info.requestedPermissions?.toList() ?: emptyList()

        val signatures = readSignatureBytes(file)
        return ApkInfo(
            filePath = file.absolutePath,
            appName = label,
            packageName = pkg,
            versionName = info.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            targetSdk = info.applicationInfo?.targetSdkVersion,
            minSdk = if (Build.VERSION.SDK_INT >= 24) info.applicationInfo?.minSdkVersion else null,
            size = file.length(),
            permissions = perms,
            signMd5 = signatures?.let { md5(it) },
            signSha1 = signatures?.let { sha1(it) },
            signSha256 = signatures?.let { sha256(it) }
        )
    }

    /** 从 APK 内签名文件取字节，用于算指纹 */
    private fun readSignatureBytes(file: File): ByteArray? {
        return try {
            ZipFile(file).use { zip ->
                val entries = zip.entries().toList()
                val target = entries.firstOrNull {
                    it.name.startsWith("META-INF/") &&
                        (it.name.endsWith(".RSA") || it.name.endsWith(".DSA") ||
                            it.name.endsWith(".EC"))
                } ?: return null
                zip.getInputStream(target).readBytes()
            }
        } catch (e: Exception) { null }
    }

    private fun md5(b: ByteArray): String = digest("MD5", b)
    private fun sha1(b: ByteArray): String = digest("SHA-1", b)
    private fun sha256(b: ByteArray): String = digest("SHA-256", b)

    private fun digest(alg: String, b: ByteArray): String =
        MessageDigest.getInstance(alg).digest(b)
            .joinToString(":") { "%02X".format(it) }
}
