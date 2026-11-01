# 清风文件 —— 重建工程 · 骨架版 v0.1

这是**从零新建**的工程，不是对原包的改造。当前是骨架，用来验证构建链。

---

## 一句话：怎么把它变成 APK

**用 GitHub Actions 云端构建**，你不需要装任何开发环境，全程在手机浏览器上点几下。

---

## 详细步骤（手机上就能做）

### 第 1 步：注册 / 登录 GitHub
- 手机浏览器打开 `github.com`，有账号就登录，没有就注册（免费）

### 第 2 步：新建仓库
1. 点右上角 `+` → `New repository`
2. Repository name 填：**qingfeng-files**
3. 选 **Public**（公开仓库才能免费用 Actions）
4. 勾上 **Add a README file**
5. 点 `Create repository`

### 第 3 步：上传这个工程
1. 进到刚建的仓库页面，点 `Add file` → `Upload files`
2. 把我给你的 **qingfeng-files.zip 解压后的全部文件**拖进去上传
   （注意：`gradle/wrapper` 里的目录也要一起传）
3. 底部点 `Commit changes`

### 第 4 步：点一下构建
1. 仓库页面顶部点 **Actions**
2. 左侧点 **Build APK**
3. 右边点 **Run workflow** → 再点 **Run workflow**
4. 等 3～8 分钟，出现绿色 ✅ 就成功了

### 第 5 步：下载 APK
1. 点那个绿色 ✅ 的任务
2. 页面最下面 **Artifacts** 区，点 **qingfeng-files-apk**
3. 下载下来，解压，装到手机

---

## 如果 Actions 页面提示"未启用"
第一次使用会看到绿色按钮 `I understand my workflows, go ahead and enable them`，点它。

---

## 当前这一版装上去会看到什么
一个写着「清风文件 · 骨架版」的界面。

**这就够了** —— 它证明：工程能编译、能签名、能装、能启动。
确认这一步之后，我再一层一层把真正的功能搬进来。

---

## 工程结构

```
qingfeng-files/
├── settings.gradle          Gradle 仓库与模块配置
├── build.gradle             根构建脚本（AGP / Kotlin 版本）
├── gradle.properties        构建参数
├── gradle/wrapper/          Gradle 版本锁定 8.7
├── .github/workflows/       云端构建流程
└── app/
    ├── build.gradle         应用模块：compileSdk 36 / minSdk 26 / targetSdk 36
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/qingfeng/files/
        │   ├── QingfengApp.kt      应用入口
        │   └── MainActivity.kt     主界面
        └── res/                    图标、布局、主题、字符串
```

## 技术参数

| 项 | 值 |
|---|---|
| 包名 | `com.qingfeng.files` |
| 显示名 | 清风文件 |
| 版本 | 1.0.0-QF / versionCode 1 |
| minSdk | 26（Android 8.0） |
| targetSdk | 36 |
| Kotlin | 2.0.21 |
| AGP | 8.5.2 |
