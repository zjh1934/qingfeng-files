package com.qingfeng.files.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 借鉴点：参照物的基类是分层的，每层只加一个职责（权限 / 工具栏 / 对话框样式）。
 * 我们也这样，这一层只放最通用的东西，不造上帝基类。
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
