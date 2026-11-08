# 考梗记后端基础架构

本目录是一个多模块 Maven 工程，最终只部署 bootstrap 生成的一个 Spring Boot 应用。模块边界不等于微服务。

| 模块 | 内容与依赖 |
| --- | --- |
| domain | 纯 Java 访问规则、不可变权限快照及领域错误，不引用框架 |
| application | 权限上下文端口和协调组件，只依赖 domain |
| api | HTTP 错误映射、请求编号和 Web 适配，只引用核心与 Web 技术 |
| infrastructure | JDBC 权限元数据读取、事务局部身份与读写边界、Flyway 基础表和 RLS；未授权产品写入 |
| bootstrap | 启动、Spring 装配、安全和运行 profile，生成唯一可执行 JAR |

从仓库根目录运行：

```powershell
./tools/Invoke-Backend.ps1 verify
./tools/Start-DevBackend.ps1 -Profile bootstrap
```

运行产物为 `bootstrap/target/geng-backend-0.1.0-SNAPSHOT.jar`。不要使用重构前根 target 中的旧产物。

bootstrap 不连接数据库，也不提供业务假数据；仅最小健康端点开放，业务入口默认拒绝。local 模式连接真实 PostgreSQL，迁移另行执行。两种模式均不自动为应用注入数据库管理员或迁移密码。

## 扩展约束

新增业务用例先确定领域规则和核心端口，基础设施实现端口，API 调用应用层，bootstrap 装配。不得把 SQL、云 SDK 或 HTTP 对象传入核心。未有实际可变能力前不增加无意义接口。

EntryAuthorization 当前只作权限协调，不实现事务或持久化。未来写入命令必须在统一真实数据库事务中重新读取权威空间、记录状态并校验，不能把一次先行授权当成永久写权限。账号清除是单独受控用例，不在普通操作枚举中增加绕过冻结的后门。

local 配置已装配 ActorTransactions 与 JdbcEntryAccessReader；数据库读取和授权可以在同一实际事务验证。框架不进入 application/domain；禁止在已有事务中另开 actor 边界，防止嵌套用例变更身份或升级只读事务。当前应用表无产品 DML 授权，写事务行为通过专用集成测试表验证。

业务模块、已实现与待接入内容见 [模块说明](../docs/modules.md)。完整本地检查入口为 `../tools/Verify-Foundation.ps1 -Full`。

错误使用统一 Problem Details。请求编号由服务端生成并贯穿响应和 MDC；原始请求正文、绑定错误值、异常消息及堆栈不作为默认错误或日志内容。默认不记录请求参数或有效凭据。

领域权限、应用范围约束、HTTP 脱敏及架构依赖测试随 verify 执行。架构检查不替代数据库 RLS、锁与恢复验收；当前不宣称这些能力已经实现。
