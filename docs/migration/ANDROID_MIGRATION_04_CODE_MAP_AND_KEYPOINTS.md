# 原代码映射与关键点（从 badmintonding 到 Android）

## 1. 核心文件映射

## `main.py` -> Android `domain + data`

原职责：

- 配置加载与规则校验
- 开放时间计算
- 预约流程编排
- 成功/失败判定
- 失败分类与重试控制

Android 对应：

- `BookingUseCase`
- `BookingValidator`
- `BookingTimeCalculator`
- `FailureClassifierImpl`
- `RetryExecutor`

## `ui_server.py` -> Android `ViewModel + Scheduler`

原职责：

- 任务启动/停止
- 状态维护（phase、attempt、倒计时）
- 轮询接口给前端

Android 对应：

- `BookingViewModel`
- `StateFlow<BookingUiState>`
- `WorkManager/Coroutine`（按你选型）

## `web/app.js` -> Android `UI`

原职责：

- 向导页面逻辑
- 参数录入
- 按钮交互与轮询显示

Android 对应：

- `Compose Screen` 或 `Fragment`
- `collectAsState` 渲染状态

## 2. 必须迁移的关键算法/规则

1. 开放时间计算：`compute_target_and_open_times`
2. 倒计时等待：`wait_until_open_time`
3. 失败分类：`detect_booking_failure_reason`
4. 重试策略：`run_booking_loop` 中“每分钟重试”

## 3. 原项目关键接口痕迹（用于你后续对接验证）

从现有调试数据可见关键接口包含（目标服务）：

- `getInfoByStaff`
- `getIndexByStaff`
- `getCampusLstByStaff`
- `getVenueLstByStaff`
- 提交接口：
  - `submitVenueOrderByStaff`
  - `submitMulVenueOrderByStaff`

注意：提交接口在原项目中有“前端加密/请求封装复用”逻辑，Android 需专项验证。

## 4. 关键风险与迁移建议

### 风险 K1：加密与 token 机制不一致

现象：

- 请求返回失败/未实际下单

建议：

- 单独做 `crypto + auth` 模块
- 建立“同一参数在旧方案与新方案的一致性对比”

### 风险 K2：成功判定不稳定

原系统不是只看单一文案，而是综合：

- 成功页面 URL/DOM
- 提交请求是否发出
- 页面状态变化

建议：

- Android 不要只看“HTTP 200”
- 需要定义明确成功信号优先级

### 风险 K3：重试导致误触发

建议：

- 维护“任务唯一 ID + 当前 attempt”
- 成功后立即终止重试协程/任务

## 5. Android 页面建议（可替换风格）

你后续会重发风格，这里仅给结构：

1. 登录页
2. 参数配置页（校区/日期/场地/时段）
3. 倒计时与运行页
4. 结果页（成功/失败 + 原因 + 尝试次数）
5. 调试页（可选）

## 6. 开发里程碑建议

### M1（基础骨架）

- 建模 + 状态流 + UI 骨架 + 本地存储

### M2（业务规则）

- 参数校验 + 开放时间 + 倒计时 + 重试框架

### M3（接口打通）

- 登录态注入 + 提交接口 + 失败分类

### M4（稳定性）

- 日志、异常恢复、后台保活策略

## 7. 最终交付验收清单

- 可完成一次登录并持久化
- 可按规则预约并自动重试
- 可稳定区分“不可重试失败”
- 有完整运行状态反馈
- 有可排错日志（可导出）

