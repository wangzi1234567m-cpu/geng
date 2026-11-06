# 第一阶段第二轮验收

本轮目标：补齐可正常开发的本地工程基础与业务模块边界。Docker 本机运行继续按用户要求暂缓。

| 条目 | 本轮实现 / 证据 |
| --- | --- |
| 分层与解耦 | 五个 Maven 编译模块，一个部署进程；ArchUnit 校验框架隔离、适配器边界和无循环依赖 |
| 业务模块 | identity、space、entry、media、lifecycle，详见 modules.md；不把接口边界当作业务功能完成 |
| 数据库 | PostgreSQL 17.11 真实实例、三个分权账号、五个 schema；V1 保持不变，V2 为四张核心基础表及只读 RLS |
| 迁移 | 开发空库与专用集成空库迁移成功；重复 migrate 不增加版本；validate 通过；摘要清单进入版本控制 |
| 事务与权限 | 六项真实 PG 集成测试覆盖三用户跨空间隔离、无 actor 默认拒绝、连接池身份清理、只读禁写、回滚、版本状态读取、两人容量及角色权限；普通业务表未授予写入 |
| 后端 | 18 项原有测试通过，加上 6 项 PG 测试；默认拒绝业务访问，统一错误、请求编号与异常脱敏 |
| Android | 依赖锁文件纳入版本控制；单元测试、APK 与 lint 的最终结果见 environment-status.md |
| 契约与格式 | Redocly 校验 32 个设计操作、引用、唯一 ID、结构和错误契约；Prettier 检查工程 JSON/YAML；迁移摘要检查 |
| 秘密检查 | Gitleaks 检查可提交文件快照及已有历史，无发现；不扫描后上传真实配置，真实配置始终被排除 |
| 依赖审计 | Maven SBOM 110、Android 锁文件 125、npm 锁文件 2 项依赖被 OSV 实际解析，未发现已知漏洞；npm audit 零发现。这不是未来始终无漏洞的保证 |
| CI 配置 | actionlint 验证通过；三个任务 quality、backend、android，包含真实 PG 与镜像构建；远程尚未执行 |
| 本地重启与干净检出 | PostgreSQL 实际重启保留提交数据与迁移；独立克隆实现提交 86186fe，无编译产物的后端与 Android 构建、测试、lint 均通过，构建后工作树无已跟踪变更 |
| Git 交付 | 本地实现提交 86186fe；隔离克隆中完成合并演练，结果树一致，源 main 保持原样；功能分支 codex/foundation-modules，远程公开上传范围尚需确认 |

## 交付边界

第一阶段基础模块包含可执行工程、访问策略、事务入口与真实存储验证，不包含阶段 2 的邮箱登录闭环、正式产品页面、正文加密与 KMS、私密图片处理、冻结并发写入或永久清除流程。只有已验证的能力计入本轮结果。

远程 CI、仓库保护设置与本机 Docker 镜像运行在实际执行前不标记完成。公开仓库上传涉及此前本地设计资料的公开范围，因此需在可审查提交就绪后确认；本地实现与验证先完成。

## 复现入口

```powershell
npm ci
./tools/Install-Postgres.ps1
./tools/Install-QualityTools.ps1
./tools/Verify-Foundation.ps1 -Full
./tools/Start-DevBackend.ps1 -Profile local
```

Full 保留本地 PG 供开发使用。停止时执行 `./tools/Stop-LocalDatabase.ps1`，它保留数据并移除本次使用的数据库盘符别名。

工具下载均固定版本与校验值；本机配置和详细扫描、测试日志位于被 Git 忽略的位置，不上传云仓库。PostgreSQL 校验值来源：[EDB 官方安装器仓库维护者回复](https://github.com/EnterpriseDB/edb-installers/issues/682)。
