# 流程与接口契约（Android 内部实现蓝图）

本文档的“接口”是指你在 Android 项目内部应定义的模块接口契约，便于后续我只看文档就继续开发。

## 1. 端到端流程

## 流程 A：登录

1. 用户点击“开始登录”
2. 执行登录流程（WebView/OAuth/浏览器跳转，按你后续方案）
3. 获取并存储登录态（token/cookie）
4. 校验登录态是否可用
5. 状态更新为 `ready`

## 流程 B：预约任务

1. 用户配置 `BookingConfig`
2. 校验参数合法性
3. 计算 `targetDate` 和 `openTime`
4. 进入 `waiting_open` 倒计时
5. 到点执行一次提交
6. 成功则 `success`，失败则分类：
   - 不可重试 => `blocked`
   - 可重试 => `retrying`（60 秒后重试）

## 2. 领域模型建议

```kotlin
data class BookingConfig(
    val campusName: String,
    val venueName: String,
    val courtNumber: Int,
    val timeSlots: List<String>, // 1..2
    val targetDayOffset: Int,    // 0/1/2
    val timeoutMs: Int = 8000
)

data class BookingOutcome(
    val success: Boolean,
    val reason: String,
    val retryable: Boolean
)

enum class BookingPhase {
    IDLE, LOGIN, READY, WAITING_OPEN, RETRYING, SUCCESS, BLOCKED, STOPPED
}
```

## 3. 仓储接口契约（建议）

```kotlin
interface AuthRepository {
    suspend fun hasLoginState(): Boolean
    suspend fun saveLoginState(raw: String)
    suspend fun clearLoginState()
    suspend fun getLoginState(): String?
}

interface BookingRemoteDataSource {
    suspend fun fetchCampusList(): Result<List<String>>
    suspend fun fetchVenueList(campusId: String): Result<List<String>>
    suspend fun submitOrder(payload: SubmitPayload): Result<SubmitResult>
}

interface BookingScheduler {
    suspend fun start(config: BookingConfig)
    suspend fun stop()
    val state: kotlinx.coroutines.flow.StateFlow<BookingUiState>
}
```

## 4. 状态对象契约（UI 层）

```kotlin
data class BookingUiState(
    val running: Boolean = false,
    val phase: BookingPhase = BookingPhase.IDLE,
    val attempts: Int = 0,
    val lastReason: String = "",
    val nextAttemptInSec: Int? = null,
    val openInSec: Int? = null,
    val openTimeStr: String = "",
    val targetDateStr: String = "",
    val config: BookingConfig? = null
)
```

## 5. 提交链路关键接口（高风险）

Android 侧你必须预留以下职责点：

1. `PayloadBuilder`：把选中的场地/时段转换为提交结构
2. `TokenProvider`：注入认证 token/cookie
3. `CryptoSigner`：完成加密/编码（若目标接口要求）
4. `SubmitApi`：真正发起订单提交

建议接口：

```kotlin
interface CryptoSigner {
    fun encryptForSubmit(plainJson: String): String
}

interface SubmitApi {
    suspend fun submitVenueOrder(encryptedParams: String, userToken: String): Result<String>
}
```

## 6. 失败分类器契约（必须实现）

```kotlin
interface FailureClassifier {
    fun classify(rawMessage: String, rawBody: String? = null): BookingOutcome
}
```

规则映射：

- 命中“已被占用” => `retryable=false`
- 命中“登录失效/请登录” => `retryable=false`
- 其他 => `retryable=true`

## 7. 重试执行器建议

```kotlin
interface RetryPolicy {
    val intervalSec: Int get() = 60
    fun shouldRetry(outcome: BookingOutcome): Boolean = !outcome.success && outcome.retryable
}
```

## 8. 日志与调试落地建议

建议在 Android 项目内保存：

- 最近 N 次请求摘要（url/method/status/time）
- 每次失败原因与分类结果
- 提交前后的关键 payload（脱敏后）

以便替代原项目 `debug/*.json` 的排查能力。

