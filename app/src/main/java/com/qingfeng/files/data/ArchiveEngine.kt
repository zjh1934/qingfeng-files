package com.qingfeng.files.data

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 压缩 / 解压引擎。
 *
 * 借鉴点（实测参照物）：
 * - 压缩级别 4 档：Store / Fast / Standard / Best → Deflater 0/1/6/9
 * - 长任务**可中断**（`ig`="确定要停止压缩吗？"）
 * - 有实时进度（`zu`="正在压缩 %1$s.%2$s"）
 * - 支持密码（`ah`=Encrypt、`l2`="Password:"），但 ZIP 传统加密不安全，
 *   我们**不做**加密（也不做解密），避免给用户虚假的安全感
 *
 * 只做 zip：JDK 原生支持，无额外依赖。
 */
class ArchiveEngine {

    @Volatile
    private var cancelled = false

    fun cancel() { cancelled = true }

    /**
     * 压缩。
     * @param files 要压的文件/目录
     * @param zipFile 输出的 .zip
     * @param level 压缩级别
     * @param onProgress (当前文件名, 已完成, 总数) → 返回 false 表示取消
     */
    fun compress(
        files: List<File>,
        zipFile: File,
        level: CompressLevel = CompressLevel.STANDARD,
        onProgress: (String, Int, Int) -> Boolean
    ): Result<Int> {
        cancelled = false
        return try {
            val all = ArrayList<Pair<File, String>>()
            files.forEach { f -> collect(f, f.parentFile, all) }
            var done = 0
            zipFile.parentFile?.mkdirs()
            ZipOutputStream(
                BufferedOutputStream(FileOutputStream(zipFile))
            ).use { zos ->
                zos.setLevel(level.deflaterLevel)
                // 借鉴：级别 0 = 仅存储，不走 Deflater
                zos.setMethod(if (level == CompressLevel.STORE) ZipEntry.STORED else ZipEntry.DEFLATED)
                for ((f, entryName) in all) {
                    if (cancelled) return Result.failure(Exception("已取消"))
                    if (!onProgress(f.name, done, all.size)) {
                        cancelled = true
                        return Result.failure(Exception("已取消"))
                    }
                    if (f.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryName/"))
                        zos.closeEntry()
                    } else {
                        val entry = ZipEntry(entryName)
                        if (level == CompressLevel.STORE) {
                            // STORED 必须预先提供 size 和 crc
                            entry.size = f.length()
                            entry.crc = crc32Of(f)
                        }
                        zos.putNextEntry(entry)
                        BufferedInputStream(FileInputStream(f)).use { it.copyTo(zos, BUF) }
                        zos.closeEntry()
                    }
                    done++
                }
                onProgress("完成", all.size, all.size)
            }
            Result.success(all.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解压。
     * @param intoSubDir true = 解压到以包名命名的子目录（参照物 "Extract to" 的默认行为）
     */
    fun extract(
        zipFile: File,
        destDir: File,
        intoSubDir: Boolean = true,
        onProgress: (String, Int, Int) -> Boolean
    ): Result<Int> {
        cancelled = false
        return try {
            val root = if (intoSubDir) {
                File(destDir, zipFile.nameWithoutExtension).also { it.mkdirs() }
            } else destDir.also { it.mkdirs() }

            // 先数条目，才能报进度
            val total = countEntries(zipFile)
            var done = 0
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (cancelled) return Result.failure(Exception("已取消"))
                    if (!onProgress(entry.name, done, total)) {
                        cancelled = true
                        return Result.failure(Exception("已取消"))
                    }
                    val out = safeDest(root, entry.name)
                    if (entry.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { zis.copyTo(it, BUF) }
                    }
                    zis.closeEntry()
                    done++
                }
                onProgress("完成", total, total)
            }
            Result.success(done)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 列出压缩包内条目（用于"浏览压缩包内容"） */
    fun listEntries(zipFile: File): List<Pair<String, Long>> {
        val out = ArrayList<Pair<String, Long>>()
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                out.add(e.name to e.size)
                zis.closeEntry()
            }
        }
        return out
    }

    // ---------- 内部 ----------

    /** 收集文件，entryName 用相对路径 */
    private fun collect(file: File, base: File?, out: MutableList<Pair<File, String>>) {
        val name = if (base != null) {
            file.absolutePath.removePrefix(base.absolutePath).trimStart('/')
        } else file.name
        if (name.isEmpty()) return
        out.add(file to name)
        if (file.isDirectory) {
            file.listFiles()?.forEach { collect(it, base, out) }
        }
    }

    /** 防 Zip Slip 路径穿越攻击 */
    private fun safeDest(root: File, entryName: String): File {
        val target = File(root, entryName).canonicalFile
        if (!target.path.startsWith(root.canonicalFile.path + File.separator) &&
            target.path != root.canonicalFile.path
        ) throw SecurityException("非法的压缩包条目路径：$entryName")
        return target
    }

    private fun crc32Of(f: File): Long {
        val crc = java.util.zip.CRC32()
        BufferedInputStream(FileInputStream(f)).use { input ->
            val buf = ByteArray(BUF)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                crc.update(buf, 0, n)
            }
        }
        return crc.value
    }

    private fun countEntries(zipFile: File): Int {
        var n = 0
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            while (zis.nextEntry != null) { n++; zis.closeEntry() }
        }
        return n
    }

    companion object { private const val BUF = 64 * 1024 }
}
