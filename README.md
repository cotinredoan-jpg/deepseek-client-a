# DeepSeek 客户端（Android Demo）

一个最小可用的 Android 手机端 DeepSeek 客户端，使用 Kotlin + Jetpack Compose 编写，通过 OpenAI 兼容协议直连 DeepSeek API（也可换成任意兼容服务）。

## 功能

- 单会话聊天界面，气泡 UI，支持长文本
- 流式输出（SSE，边收边显示）
- API Key / Base URL / Model / System Prompt 可配置，持久化到本地
- 聊天历史本地持久化（DataStore）
- 一键清空历史
- 生成中可随时停止

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- minSdk 24（Android 7.0+）

## 拿到 APK 的两种方式

### 方式 A：用 GitHub Actions 在线构建（推荐，本机零环境）

适合：你不想装 Android Studio，只想要一个能装的 `.apk`。

1. 在 GitHub 新建一个空仓库（公开/私有都行；**勾选 Add README** 以便仓库初始化）。
2. 把 `DeepSeekClient` 目录里的所有文件传上去，二选一：

   **方式 ①：网页拖拽（最简单，不需要 git）**
   - 打开刚建好的仓库 → 点 **Add file → Upload files**
   - 把 `DeepSeekClient` 文件夹里**全部内容**（含 `.github`、`.gitignore`，注意是文件夹"里面"，不是这一层文件夹本身）直接拖进网页
   - 下方写一行 commit message → **Commit changes**
   - ⚠️ `.github` 是隐藏文件夹，Windows 资源管理器需先在"查看"里勾选"隐藏的项目"才看得见。

   **方式 ②：命令行（要装 git）**
   ```bash
   cd DeepSeekClient
   git init
   git add .
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/<仓库名>.git
   git push -u origin main
   ```

3. 推送完成后，打开仓库页面 → **Actions** 标签 → 等几分钟看到 **Build APK** 这次跑变绿。
4. 点进这次运行 → 最下面 **Artifacts** 区域有 `DeepSeekClient-debug-apk` → 下载解压拿到 `DeepSeekClient-debug.apk`。
5. 把 APK 传到手机（微信发自己 / 数据线 / 网盘都行），手机点击安装（首次需要在系统里允许"安装未知来源"）。
6. 打开 App → 右上角齿轮 → 填 API Key（在 https://platform.deepseek.com/ 申请）。

> 这是 **debug 包**，没有签名/混淆，只用于自用没问题，但**不能**上传到应用市场。

### 方式 B：本地用 Android Studio 构建

1. 安装 Android Studio（首次约 2-3GB）。
2. "Open" 打开本目录 → 等 Gradle 同步（首次 10-30 分钟，下依赖）。
3. 菜单 **Build → Build App Bundle(s)/APK(s) → Build APK(s)**，构建完点 *locate* 拿到 APK。
4. 或者直接 USB 连手机 / 开模拟器，点运行也可以。

## 首次使用

打开 App 后点右上角齿轮图标，填写：
- **API Key**：在 https://platform.deepseek.com/ 申请
- **Base URL**：默认 `https://api.deepseek.com`
- **Model**：`deepseek-chat` 或 `deepseek-reasoner`
- **System Prompt**：可留默认

## 也可换成其他服务

只要支持 OpenAI 的 `/v1/chat/completions` 接口与 SSE 流式格式，就能直接用：
- 把 Base URL 改成对应服务（如 `https://api.openai.com`、`https://api.moonshot.cn` 等）
- 把 Model 改成该服务支持的模型名
- 填入对应的 API Key

## 项目结构

```
app/src/main/java/com/example/deepseek/
├── MainActivity.kt        // 入口 Activity + 主题
├── Models.kt              // 数据模型
├── Storage.kt             // DataStore：设置 + 历史持久化
├── Api.kt                 // OkHttp + SSE 调用
├── ChatViewModel.kt       // 状态管理
└── ui/
    ├── ChatScreen.kt      // 聊天主界面
    └── SettingsDialog.kt  // 设置弹窗
```

## 后续可扩展（Demo 没做）

- 多会话/会话列表
- 多套配置切换（账号/服务商预设）
- Markdown 渲染、代码块复制
- 图片/附件上传（多模态模型）
- 导出聊天记录
