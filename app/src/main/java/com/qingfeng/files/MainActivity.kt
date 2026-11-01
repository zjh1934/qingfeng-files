package com.qingfeng.files

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 清风文件 —— 骨架入口
 *
 * 这是重建工程的第一块砖。当前只做一件事：证明整条构建链是通的。
 * 后续会按依赖层次逐层搬入原项目的模块：
 *   compress / chromecast / utils / common / base
 *     -> apk / access / ui / page
 *       -> fileprovider <-> fileexplorer
 *         -> feature
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
