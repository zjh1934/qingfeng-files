package com.qingfeng.files

import android.app.Application
import android.content.Context

class QingfengApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.set(this)
    }
}

/** 全局 Context 持有（避免到处传 context） */
object AppContext {
    private lateinit var app: Application

    fun set(a: Application) { app = a }

    fun get(): Application = app

    fun context(): Context = app.applicationContext
}
