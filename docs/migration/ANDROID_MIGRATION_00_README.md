# badmintonding 纯移动端迁移文档总览

这套文档用于把当前 `badmintonding` 项目完整迁移为“纯移动端 Android App”。

目标：

- 不再依赖当前项目源码目录进行二次分析
- 新建 Android 项目后，只通过本目录的 Markdown 文档即可理解原系统
- 可作为实现清单、接口约定、流程规范与验收标准

## 文档清单

1. `ANDROID_MIGRATION_01_SYSTEM_OVERVIEW.md`
   - 项目全貌、核心能力、功能边界、重要状态机

2. `ANDROID_MIGRATION_02_FEATURE_SPEC.md`
   - 用户功能、业务规则、异常与重试策略、验收点

3. `ANDROID_MIGRATION_03_FLOW_AND_API_CONTRACT.md`
   - 端到端流程、内部接口契约（App 内部模块接口）、数据结构建议

4. `ANDROID_MIGRATION_04_CODE_MAP_AND_KEYPOINTS.md`
   - 原项目关键代码映射（Python/Web 到 Android）
   - 关键实现细节与易踩坑

## 推荐使用方式

在新 Android 项目中建立 `docs/migration/`，把本目录四个文档拷贝进去。

建议实现顺序：

1. 先读 `01`，统一架构认知  
2. 再读 `02`，确定功能范围和验收标准  
3. 按 `03` 先搭接口与状态流  
4. 开发时对照 `04` 逐个迁移关键能力

## 迁移目标说明（纯移动端）

本次迁移定义的“纯移动端”含义：

- Android 客户端独立运行
- 不依赖当前 `ui_server.py + web/` 的本地控制面板
- 不要求保留 Playwright 浏览器自动点击实现
- 需要复刻业务行为：登录、选参数、等待开放、预约提交、失败重试、结果反馈

## 重要提醒

原项目里存在“网页前端加密/鉴权”的提交逻辑探测与兜底。迁移到 Android 时，必须把“提交请求链路的加密与 token 机制”作为专项验证项，否则会出现“看起来流程正常但无法下单”的问题。

