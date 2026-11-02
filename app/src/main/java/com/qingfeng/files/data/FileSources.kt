package com.qingfeng.files.data

/**
 * 文件源工厂（对应参照物的 `ace/zr5.j(String)`）。
 *
 * 参照物按路径里的协议前缀分发实现：
 * gdrive→zf3、onedrive→sy5、dropbox→ab2、mega→de5、box/yandex→r79。
 *
 * 我们目前只有本地；将来加网盘时按前缀分发即可，UI 一行不用改。
 */
object FileSources {
    private val local = LocalFileSource()

    /** 按路径返回合适的文件源 */
    fun forPath(path: String): FileSource = local

    fun local(): FileSource = local
}
