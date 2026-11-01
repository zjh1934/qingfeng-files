# 清风文件

从零新建的 Android 文件管理器，无广告、无会员。

## 当前版本功能

- 浏览手机里的文件和文件夹
- 点文件夹进入，返回键退出上层
- 点文件用系统方式打开
- 长按：删除 / 重命名
- 下拉刷新

## 权限说明

Android 11 以上系统要求「所有文件访问权限」才能浏览手机文件。
首次打开会引导去设置页开启，这是系统硬性要求。

## 怎么构建 APK

用 GitHub Actions 云端构建，不需要本地开发环境：

1. 打开仓库页面，点顶部 **Actions**
2. 左侧点 **Build APK**
3. 右边点 **Run workflow** → 再点 **Run workflow**
4. 等 3～8 分钟，出现绿色 ✅ 即成功
5. 点进任务，页面底部 **Artifacts** 区下载

## 工程结构

```
app/src/main/java/com/qingfeng/files/
├── MainActivity.kt              入口（权限检测与路由）
├── QingfengApp.kt               应用入口
├── base/BaseActivity.kt         基类
├── data/                        数据层
│   ├── FileItem.kt              统一文件模型
│   ├── FileSource.kt            文件来源接口
│   ├── LocalFileSource.kt       本地存储实现
│   └── FileError.kt             错误体系
├── ui/browse/                   浏览功能（MVVM）
│   ├── BrowseActivity.kt
│   ├── BrowseViewModel.kt
│   └── FileAdapter.kt
└── util/FileFormat.kt           格式化工具
```

## 设计来源

参照了现有文件管理器的架构思路（统一数据模型 + 文件来源抽象 +
基类分层），代码全部自己写，用 Kotlin + Coroutines + MVVM。
广告与会员逻辑不存在。
