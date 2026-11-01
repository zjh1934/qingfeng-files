package com.qingfeng.files

import android.app.Application

/**
 * 应用入口。
 * 目前无全局初始化需求，后续如有（主题、崩溃收集、数据库）放这里。
 */
class QingfengApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
