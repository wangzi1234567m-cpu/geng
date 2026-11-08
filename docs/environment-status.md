# 开发环境与第一阶段验证记录

验证日期：2026年11月6日。分支：codex/foundation-modules。此记录描述本机结果，不代表产品业务功能已经实现。

## 已通过

| 项目 | 结果 |
| --- | --- |
| JDK / Maven | Temurin 21.0.13+9 / Maven 3.9.16，项目工具链与 Maven Wrapper 可构建 |
| Gradle | 8.14.6，分发包 SHA256 已固定，Wrapper 已更新 |
| Android SDK | Command-line Tools 19.0、API 36 revision 2、Build Tools 35.0.0、ADB 37.0.1 |
| 后端构建 | Maven reactor clean verify、verify 成功，五个编译模块生成一个可执行 JAR |
| 后端测试 | 18 项原有测试 + 6 项真实 PostgreSQL 集成测试通过，0 失败、0 错误、0 跳过；含三用户空间隔离、事务与连接池身份、双人约束及分权角色 |
| 实际 JAR 启动 | bootstrap 与 local 均经过打包运行；local 实际连接 PostgreSQL 后健康接口 200 UP、未认证业务 401，头与错误请求编号一致；验证后端已停止 |
| Android 单元测试 | 8 项通过，0 失败、0 错误、0 跳过；覆盖端点限制、健康 JSON、错误内容、响应体上限和禁止重定向 |
| Android APK / lint | testDebugUnitTest assembleDebug lintDebug --offline 成功；lint 0 errors、5 warnings，均为固定依赖存在新版本的提示；未设置错误基线或关闭检查 |
| 脚本与配置 | PowerShell 脚本语法通过；git diff --check 无空白错误。历史 Compose config --quiet 通过，不等于容器运行通过 |
| 本地秘密 | 本地密码随机生成、不打印、复跑不覆盖；本地目录 DACL 已限制；配置、工具、缓存和构建产物被 Git 忽略 |

后端测试结果位于各模块 `target/surefire-reports/`；运行 JAR 为 `backend/bootstrap/target/geng-backend-0.1.0-SNAPSHOT.jar`。

Android 备份排除规则覆盖旧版备份与 Android 12 之后的云备份、设备迁移配置，实际设备行为仍需真机验证。当前图标为开发占位矢量图，不是已确认 Logo。

## 当前开发方式

按用户安排不依赖 Docker。已安装并验证项目专用 PostgreSQL 17.11，Start-LocalDatabase 启动，Stop-LocalDatabase 停止且保留数据；无数据库 bootstrap 模式仍可使用。没有 H2 或虚假持久化。

Windows 中文 Gradle 缓存路径曾导致 Java 测试 worker 无法加载；外部缓存目录又遇到重命名问题。本机改为仓库内 `.tools/gradle-drive/home`，Android 构建脚本临时为整个工作区使用空闲盘符路径别名，完成后清理。未关闭安全软件或跳过检查；`dev.local.json` 的机器路径不进入 Git。

## 第二轮补齐结果与剩余外部验收

- **真实数据库已验证**：PostgreSQL 17.11 官方 ZIP 的 SHA256 通过；不安装系统服务，数据受 `.dev` 私密 ACL 保护，监听 127.0.0.1:55432。原 V1 保持不变，新增 V2 四张核心基础表、双人容量约束与只读 RLS；开发空库与专用集成空库迁移、重复迁移及 validate 均通过。
- **事务和隔离已验证**：独立应用、迁移、管理员角色；同连接事务局部 actor，提交及回滚均清除；真实只读禁写、回滚、嵌套身份拒绝、三账号跨空间隔离、冻结策略和容量约束测试通过。产品写权限尚未开放。
- **数据库重启已验证**：专属测试表中的提交数据、角色和迁移 v2 在实际停止、重启后保留，测试表已清除。
- **工程检查已验证**：`Verify-Foundation.ps1 -Full` 整套通过；Gitleaks 工作树快照与历史无发现；OpenAPI 32 个设计操作、Prettier 和 actionlint 通过；OSV 实际解析后端 110、Android 125、npm 2 项依赖，无已知漏洞报告；npm audit 零发现。
- **Git 与干净检出已验证**：实现提交 `86186fe` 位于 `codex/foundation-modules`。独立克隆后在隔离分支完成无冲突合并，合并树与实现提交一致，源仓库 main 未改动；干净检出后端 verify、Android 8 项单元测试、APK、lint 全部通过，构建后无已跟踪文件变更。复用已验证工具及依赖缓存，没有复用编译产物。
- **远程 CI 已通过**。功能分支已按授权公开上传，质量、后端和 Android 三项通过。 [草稿 PR #1](https://github.com/wangzi1234567m-cpu/geng/pull/1) · [验收运行](https://github.com/wangzi1234567m-cpu/geng/actions/runs/42442037991)
- **容器已在 CI 验收**。Compose 构建并启动应用，重建 PostgreSQL 容器后迁移记录保留，应用恢复健康。本机 Docker 排障继续暂缓。
- **分支保护待授权**。只读检查显示 `main` 未受保护。规则模板位于 `.github/rulesets/main.json`。
- **后续产品阶段**：Android Studio/模拟器/真机、真实邮箱账号、正文加密/KMS、对象存储、正式业务页面、冻结并发和永久清除闭环仍未交付；它们属于阶段 2 及之后。

实际业务边界见 `modules.md`，本轮验收表见 `phase1-acceptance.md`。不将本地架构完成等同于首版产品可试用。

## 使用入口

```powershell
./tools/Check-DevEnvironment.ps1
./tools/Verify-Foundation.ps1 -Full
./tools/Start-DevBackend.ps1 -Profile local
```

Android 输出为 `android/app/build/outputs/apk/debug/app-debug.apk`，属于调试包。完整说明见 [development.md](development.md)。
