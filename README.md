# ding_badminton

**某校园羽毛球场地预约助手** · Android

提前保存登录凭证，选好校区、日期、场地和时段，应用会在开放时间（北京时间 07:00:05）自动提交，失败按规则重试。

[下载最新版](https://github.com/zdycc/priv-android-badminton/releases/latest) ·
[发布记录](https://github.com/zdycc/priv-android-badminton/releases) ·
[MIT License](LICENSE)

> 仅用于个人学习与效率实践。请遵守学校场馆预约规定，不要用于代抢、倒卖等用途，使用后果由使用者自行承担。

## 功能

- **登录一次，长期复用**：`user-token` 与两个接口参数保存在本机，不写入代码仓库。
- **点选式配置**：校区、日期、场地号、时段全部可视化选择，自动校验范围。
- **到点自动提交**：按「今天 / 明天 / 后天」计算开放时刻，倒计时结束自动发起预约。
- **失败自动重试**：可重试错误每 60 秒重试，最多 10 次；不可重试错误立即停止并说明原因。
- **状态全程可见**：等待开放、提交、重试、成功 / 失败都有明确展示。
- **任务可恢复**：已启动的任务会保存，重新打开应用自动恢复。

## 下载安装

| 项目 | 说明 |
| --- | --- |
| 最新版本 | [GitHub Releases](https://github.com/zdycc/priv-android-badminton/releases/latest) |
| 系统要求 | Android 7.0（API 24）及以上 |
| 安装方式 | 下载 APK，允许「未知来源应用」后安装 |
| 完整性校验 | Release 附带 `SHA256SUMS.txt`，可执行 `shasum -a 256 Badminton-*.apk` |

## 快速开始

1. **装好应用**：安装 APK，允许通知、精确闹钟和后台运行（见「权限」）。
2. **准备登录参数**：用 Reqable 抓取 `user-token` 和两个接口 JSON（见「登录参数」）。
3. **开始预约**：登录校验 → 选择校区、日期、场地、时段 → 启动任务 → 等待结果。

## 登录参数

登录页需要三个值，全部来自预约系统的真实请求：

| 参数 | 位置 |
| --- | --- |
| `user-token` | `getIndexByStaff` 请求头 |
| 场地参数 | `getVenueLstByStaff` 请求体（完整 JSON） |
| 提交参数 | `submitVenueOrderByStaff` 请求体（完整 JSON） |

### 安装 Reqable（Android）

| 渠道 | 链接 |
| --- | --- |
| Google Play | [Reqable](https://play.google.com/store/apps/details?id=com.reqable.android) |
| arm64 APK（多数手机） | [下载](https://app.reqable.com/download?platform=android&arch=arm64&ext=apk) |
| armeabi-v7a APK（老设备） | [下载](https://app.reqable.com/download?platform=android&arch=arm&ext=apk) |

官网：[reqable.com/zh-CN/android](https://reqable.com/zh-CN/android/)。免费社区版即可满足抓包需求。

> Reqable 是第三方软件，与本项目无关；请从官网或 Google Play 下载。

### 抓取参数

1. 打开 Reqable，允许 **VPN 权限**，进入「抓包」页开始抓包。
2. 过滤框输入 `getIndexByStaff`、`getVenueLstByStaff`、`submitVenueOrderByStaff`。
3. 用系统浏览器打开预约系统并完成登录，正常操作一遍。
4. 回到 Reqable，分别从请求头和请求体中复制三个参数。

> 登录后不要频繁刷新页面，避免 ticket 失效导致会话不一致。

<details>
<summary><b>安装 CA 证书（可选）</b></summary>

本项目的三个参数都来自 **HTTP** 接口，不装证书也能抓取；只有需要查看 HTTPS 登录跳转时才需要。

1. Reqable →「证书 / 安装证书」→ Android → 导出或保存 CA 证书。
2. 系统设置 → 安全（或「安全与隐私」）→ 加密与凭据 → 安装证书 → **CA 证书**。
3. 选择证书文件并确认安装（需要锁屏密码或指纹）。
4. 验证：信任的凭据 →「用户」标签中能看到 Reqable。

注意：Android 7.0 及以上默认只信任系统证书，部分应用和 Chrome 新版本可能不信任用户证书。抓包结束后建议删除证书（信任的凭据 → 用户 → Reqable → 删除）。

</details>

## 使用

### 登录校验

1. 粘贴 `user-token`，点「下一步」；
2. 粘贴场地接口 JSON，点「下一步」；
3. 粘贴提交接口 JSON，点「保存并校验」。

校验通过后进入参数配置；返回 404 时检查接口路径或重新抓取会话参数；提示 JSON 无效时需粘贴完整请求体。

### 配置预约

1. 选择校区；
2. 选择日期（今天 / 明天 / 后天）；
3. 选择场地号与时段（1–2 个，选两个时必须相邻）；
4. 确认后点「启动预约任务」。

### 等待与结果

- 倒计时期间可查看开放时间与剩余秒数；
- 到点自动提交，无需人工点击；
- 运行页显示状态、尝试次数、失败原因和下次重试倒计时；
- 可重试失败每 60 秒重试一次，最多 10 次；
- 不可重试失败立即停止并进入结果页；
- 结果页显示成功 / 失败、尝试次数和预约信息。

任务启动后会保存，重新打开应用自动恢复；取消任务会永久删除，请谨慎操作。

### 权限

| 权限 | 用途 |
| --- | --- |
| 闹钟和提醒 | 在 07:00:05 精确唤醒（Android 12+） |
| 后台运行 / 电池优化 | 避免到点时被系统冻结 |
| 通知 | 展示运行状态 |

建议在开放前 1–2 分钟把应用留在前台。

## 规则

| 校区 | 场馆 | 场地号 | 可选时段 |
| --- | --- | --- | --- |
| pf校区 | 小馆羽毛球场 | 1–6 | 08–09、09–10、12–13、17–18 |
| mgs校区 | 羽毛球馆 | 1–9 | 08–09 至 20–21，共 13 个整点时段 |

| 预约目标 | 开放时间（北京时间） |
| --- | --- |
| 今天 / 明天 | 当天 07:00:05 |
| 后天 | 次日 07:00:05 |

| 失败类型 | 是否重试 |
| --- | --- |
| 时段已被占用、名额已满、参数无效、登录失效 | 否，直接停止 |
| 系统繁忙、网络异常、未知错误 | 是，每 60 秒一次，最多 10 次 |

## 常见问题

| 现象 | 处理 |
| --- | --- |
| 抓不到请求 | 确认 Reqable 正在运行、流量经过 VPN、过滤条件正确 |
| 校验返回 404 | 检查接口路径，或重新抓取一份新会话参数 |
| 提示 JSON 无效 | 粘贴完整请求体，不能只复制 `params` 的值 |
| 到点没有提交 | 检查闹钟权限、电池优化和后台限制，开放前保持应用在前台 |
| 提示登录失效 | 重新抓取 `user-token` 和两个接口参数 |

## 隐私与安全

- 所有请求只发往预约系统，不上传账号、密码或凭证到第三方。
- 凭证保存在本机 DataStore，仓库不包含任何真实票据。
- 抓包参数具有时效性，请勿公开包含 `user-token` 的截图或日志。

<details>
<summary><b>开发与构建</b></summary>

### 环境

Android 7.0（API 24）及以上，targetSdk 34；Android Studio（Ladybug 及以上）或命令行 Gradle；JDK 17。

### 构建

```bash
git clone https://github.com/zdycc/priv-android-badminton.git
cd priv-android-badminton
```

在项目根目录创建 `local.properties`：

```properties
sdk.dir=/Users/<你>/Library/Android/sdk

# 可选：登录入口地址，CAS ticket 是一次性凭证，仓库里不保存
# login.entryUrl=https://<预约系统>/h5/open?ticket=<你的 ticket>
```

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # 需要签名配置，见 docs/RELEASING.md
```

### 项目结构

```text
ding_badminton/
├── app/src/main/
│   ├── java/com/example/ding_badminton/
│   │   ├── alarm/        # 定时闹钟
│   │   ├── data/         # 本地存储、网络、提交链路
│   │   ├── domain/       # 业务规则、调度器、失败分类
│   │   └── ui/           # Compose 界面与 ViewModel
│   ├── res/
│   └── AndroidManifest.xml
├── docs/
│   ├── migration/        # 迁移设计文档
│   └── RELEASING.md      # 签名与发布
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

### 技术栈

Kotlin 1.9 · Jetpack Compose (Material 3) · Navigation Compose · ViewModel · DataStore · OkHttp · AlarmManager

### 发布

APK 通过 GitHub Releases 分发，签名与 Secrets 配置见 [docs/RELEASING.md](docs/RELEASING.md)。

</details>

## License

[MIT](LICENSE)
