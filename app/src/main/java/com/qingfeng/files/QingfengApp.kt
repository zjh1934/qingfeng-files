package com.qingfeng.files

import android.app.Application

/**
 * 应用入口。骨架阶段仅做占位，
 * 后续原项目 App 类的初始化逻辑（attachBaseContext / 多 dex / SDK 初始化）会搬到这里。
 */
class QingfengApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
