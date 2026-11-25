# ding_badminton · 某校园羽毛球场地预约助手

一个用 **Kotlin + Jetpack Compose** 编写的 Android 客户端，把某校园场馆预约的整套流程收进手机：
提前保存登录凭证，选好校区 / 日期 / 场地 / 时段，应用会在开放时间（北京时间 **07:00:05**）自动提交，
失败按规则重试，并实时展示当前阶段与结果。

[![最新版本](https://img.shields.io/github/v/release/zdycc/priv-android-badminton?label=release)](https://github.com/zdycc/priv-android-badminton/releases/latest)

> **免责声明**
> 本项目仅用于个人学习与效率实践。请遵守学校场馆预约的相关规定，不要用于代抢、倒卖或其他违规用途。
> 使用本工具产生的一切后果由使用者自行承担。

---

## 功能

**登录与参数**

- 登录凭证（`user-token` 与两个接口参数）保存在本机，一次抓取长期复用。
- 登录页内置抓包指引，三步填写并自动校验登录态。
- 抓包参数不写入代码仓库。

**预约配置**

- 校区、日期、场地号、时段全部点选，自动校验取值范围。
- 支持「今天 / 明天 / 后天」，自动计算 07:00:05 的开放时刻。
- 时段支持 1–2 个，选两个时必须是相邻时段。

**执行与反馈**

- 倒计时结束自动提交，无需人工点击。
- 可重试错误每 60 秒重试，最多 10 次；不可重试错误立即停止并说明原因。
- 运行页实时显示状态、尝试次数、失败原因与下次重试倒计时。
- 结果页展示成功 / 失败、尝试次数和预约信息。
- 任务持久化，重新打开应用可恢复；支持手动停止与取消。

**其他**

- 通过 `AlarmManager.setExactAndAllowWhileIdle` 在开放时刻唤醒应用。
- 纯移动端运行，不依赖电脑、Playwright 或浏览器自动化。

---

## 下载安装

| 项目 | 说明 |
| --- | --- |
| 最新版 APK | [GitHub Releases](https://github.com/zdycc/priv-android-badminton/releases/latest) |
| 系统要求 | Android 7.0（API 24）及以上 |
| 安装 | 首次安装时允许「未知来源应用」即可 |
| 校验 | Release 附带 `SHA256SUMS.txt`，可执行 `shasum -a 256 Badminton-*.apk` |

---

## 使用流程

整个流程分六步：装应用 → 抓参数 → 登录校验 → 配置 → 等待提交 → 看结果。

### 1. 安装与权限

1. 安装 APK 并打开应用。
2. Android 12 及以上，在「设置 → 应用 → 特殊权限 → **闹钟和提醒**」中允许本应用。
3. 在「设置 → 电池」中把应用设为**不受限制 / 允许后台活动**。
4. 开放前 1–2 分钟建议把应用**留在前台**，避免系统限制后台启动界面。

### 2. 获取登录参数

登录需要三个值，全部来自预约系统的真实请求：

| 参数 | 来源 |
| --- | --- |
| `user-token` | `getIndexByStaff` 请求的**请求头** |
| 场地参数 | `getVenueLstByStaff` 请求的**请求体**（完整 JSON） |
| 提交参数 | `submitVenueOrderByStaff` 请求的**请求体**（完整 JSON） |

抓取需要用到 Android 版 **Reqable**，下面按「装工具 → 开始抓包 → 装证书（可选）→ 取值」的顺序说明。

#### 2.1 安装 Reqable（Android）

| 渠道 | 链接 |
| --- | --- |
| Google Play | <https://play.google.com/store/apps/details?id=com.reqable.android> |
| arm64 APK（大多数手机） | <https://app.reqable.com/download?platform=android&arch=arm64&ext=apk> |
| armeabi-v7a APK（老设备） | <https://app.reqable.com/download?platform=android&arch=arm&ext=apk> |

官网：<https://reqable.com/zh-CN/android/>。免费社区版即可满足抓包需求。

> Reqable 是第三方软件，与本项目无关联；请从官网或 Google Play 下载，不要使用第三方渠道的安装包。

#### 2.2 开始抓包

1. 打开 Reqable，首次启动会请求 **VPN 权限**，选择允许。
2. 进入「抓包」页并开始抓包。
3. 在过滤框输入 `getIndexByStaff`、`getVenueLstByStaff`、`submitVenueOrderByStaff`。
4. 用系统浏览器打开预约系统并完成登录，正常操作一遍。
5. 回到 Reqable，在请求列表中查看对应接口的请求头 / 请求体。

#### 2.3 安装 CA 证书（可选）

本项目的三个参数都来自 **HTTP** 接口，**不安装证书也能抓到**；只有需要查看 HTTPS 登录跳转（例如 CAS 登录页）时才需要安装。

1. 在 Reqable 内打开「证书 / 安装证书」，选择 **Android**，按应用内指引导出或保存 CA 证书。
2. 打开系统设置 → **安全**（部分机型为「安全与隐私」）→ **加密与凭据** → **安装证书** → **CA 证书**。
3. 选择 Reqable 导出的证书文件，确认安装（需要锁屏密码 / 指纹验证）。
4. 验证：设置 → 安全 → 加密与凭据 → **信任的凭据** → 切到「**用户**」标签，能看到 Reqable 即可。

不同品牌的路径略有差异，可以直接在设置里搜索「CA 证书」或「安装证书」。

> **注意**
> - Android 7.0 及以上默认只信任系统证书，普通 App 不一定信任用户安装的证书；Chrome 新版本也可能忽略用户证书。
> - 抓包完成后建议删除证书：信任的凭据 → 用户 → Reqable → 删除，避免长期保留降低设备安全性。

#### 2.4 取三个参数

1. `getIndexByStaff` → **请求头** → 复制 `user-token` 的值；
2. `getVenueLstByStaff` → **请求体** → 复制完整 JSON；
3. `submitVenueOrderByStaff` → **请求体** → 复制完整 JSON。

> 登录后不要频繁刷新页面，避免 ticket 失效导致会话不一致。

#### 2.5 抓包常见问题

| 现象 | 处理 |
| --- | --- |
| 无法开始抓包 | VPN 权限被拒绝，到系统设置中允许 Reqable 使用 VPN |
| 请求列表为空 | 确认 Reqable 正在运行、过滤条件正确、浏览器流量确实经过 VPN |
| HTTPS 只显示 CONNECT | 证书未安装或未被信任；本项目主要接口为 HTTP，可先不处理 |
| 找不到「安装证书」 | 在设置中搜索「CA 证书」「加密与凭据」，或查看 Reqable 应用内指引 |

### 3. 登录页填写与校验

登录页分三步：**登录凭证 → 场地接口参数 → 提交接口参数**。

1. 粘贴 `user-token`，点「下一步」；
2. 粘贴场地接口 JSON，点「下一步」；
3. 粘贴提交接口 JSON，点「**保存并校验**」。

校验会调用 `getIndexByStaff` 验证登录态：

- 通过 → 进入参数配置；
- 404 / 校验失败 → 检查接口路径，或重新抓取一份新的会话参数；
- JSON 无效 → 必须粘贴完整请求体，不能只粘 `params` 字符串。

每个输入框右侧的「📋」可一键粘贴剪贴板内容；标题右侧的「⧉」可复制接口关键词。

### 4. 配置预约

1. **选择校区**：点选后进入下一步。
2. **选择日期**：今天 / 明天 / 后天。
3. **选择场地与时段**：场地号需在当前校区范围内；时段 1–2 个，选两个时必须相邻。
4. 确认信息后点「**启动预约任务**」。

### 5. 倒计时与执行

- 应用显示开放时间与倒计时，同时注册系统闹钟；
- 到点自动提交，无需人工点击；
- 运行页显示当前状态、尝试次数、失败原因、下次重试倒计时；
- 可重试失败每 60 秒重试一次，最多 10 次；
- 不可重试失败立即停止并进入结果页；
- 点「停止」可手动结束任务。

### 6. 结果、恢复与取消

- 结果页显示成功 / 失败、尝试次数（x / 10）、失败原因、预约信息；
- 点「确定并返回登录页面」回到首页；
- 已启动的任务会保存，重新打开应用自动恢复到倒计时页；
- 在任务列表中取消预约会**永久删除**该任务；
- 已存在计时任务时，再次选择日期会提示「您已经有一个计时预约了，不可再次选中」。

---

## 业务规则

### 校区与场地

| 校区 | 场馆 | 场地号 | 可选时段 |
| --- | --- | --- | --- |
| pf校区 | 小馆羽毛球场 | 1–6 | 08:00–09:00、09:00–10:00、12:00–13:00、17:00–18:00 |
| mgs校区 | 羽毛球馆 | 1–9 | 08:00–09:00 至 20:00–21:00，共 13 个整点时段 |

### 日期与开放时间

| 预约目标 | 开放时间（北京时间） |
| --- | --- |
| 今天 | 当天 07:00:05 |
| 明天 | 当天 07:00:05 |
| 后天 | 次日 07:00:05 |

### 失败与重试

| 失败类型 | 是否重试 | 说明 |
| --- | --- | --- |
| 时间段已被占用 | 否 | 直接停止 |
| 名额已满 / 余量不足 / 不可预约 | 否 | 直接停止 |
| 参数无效（6010、场地映射等） | 否 | 直接停止 |
| 登录失效 / 未登录 / 登录失败 | 否 | 需重新抓取凭证 |
| 系统繁忙、网络异常、未知错误 | 是 | 每 60 秒一次，最多 10 次 |

---

## 常见问题

| 现象 | 处理 |
| --- | --- |
| 抓不到请求 | 确认 Reqable 正在运行、流量确实经过它，HTTPS 证书已信任 |
| 校验返回 404 | 检查接口路径，或重新抓取一份新会话参数 |
| 提示 JSON 无效 | 粘贴完整请求体（含外层 `{ "params": ... }`），不要只复制值 |
| 到点没有自动提交 | 检查「闹钟和提醒」权限、电池优化、后台限制；开放前把应用留在前台 |
| 提示登录失效 | 重新抓取 `user-token` 与两个接口参数，重新保存并校验 |
| 构建失败 | 确认 JDK 17、`local.properties` 的 `sdk.dir` 正确，首次构建需联网下载依赖 |

---

## 隐私与安全

- 应用不会上传账号、密码或凭证到任何第三方服务器，所有请求只发往预约系统；
- `user-token` 与接口参数保存在本机 DataStore，不会提交到仓库；
- 仓库不包含签名密钥、`local.properties` 或真实登录票据；
- 抓包参数具有时效性，请勿公开包含 `user-token` 的截图或日志。

---

## 开发

### 环境要求

| 项目 | 要求 |
| --- | --- |
| Android | 7.0（API 24）及以上，targetSdk 34 |
| 构建 | Android Studio（Ladybug 及以上）或命令行 Gradle；JDK 17 |
| 抓包（首次） | Android 版 Reqable（见「2.1」） |

### 构建

```bash
git clone https://github.com/zdycc/priv-android-badminton.git
cd priv-android-badminton
```

在项目根目录创建 `local.properties`（不要提交）：

```properties
sdk.dir=/Users/<你>/Library/Android/sdk

# 可选：登录入口地址，CAS ticket 是一次性凭证，仓库里不保存
# login.entryUrl=https://<预约系统>/h5/open?ticket=<你的 ticket>
```

构建：

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # 需要签名配置，见 docs/RELEASING.md
```

也可以用 Android Studio 打开项目直接 Run。

### 项目结构

```text
ding_badminton/
├── app/src/main/
│   ├── java/com/example/ding_badminton/
│   │   ├── alarm/            # 定时闹钟
│   │   ├── data/             # 本地存储、网络、提交链路
│   │   ├── domain/           # 业务规则、调度器、失败分类
│   │   └── ui/               # Compose 界面与 ViewModel
│   ├── res/                  # 资源与主题
│   └── AndroidManifest.xml
├── docs/
│   ├── migration/            # 从原桌面版迁移的设计文档
│   └── RELEASING.md          # 签名与发布说明
├── gradle/                   # 版本目录与 wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

核心模块：

- `domain/usecase/BookingScheduler`：等待开放 → 提交 → 重试的主循环；
- `domain/usecase/BookingTimeCalculator`：按日期偏移计算开放时刻；
- `domain/usecase/FailureClassifier`：把接口返回归类为可重试 / 不可重试；
- `data/submit/PayloadBuilder`：把场地与时段转换为提交结构；
- `ui/BookingViewModel`：串联登录、配置与运行状态。

### 技术栈

Kotlin 1.9 · Jetpack Compose (Material 3) · Navigation Compose · ViewModel · DataStore · OkHttp · AlarmManager

### 发布

APK 通过 GitHub Releases 分发，签名、Secrets 与发布流程见 [docs/RELEASING.md](docs/RELEASING.md)。

---

## License

[MIT](LICENSE)
