# 考梗记开发环境

配置日期：2026年11月6日。当前阶段仅建立构建和本地运行基础，不包含注册、空间、梗或图片等已完成功能。

## 日常开发不依赖 Docker

用户已决定暂时搁置 Docker 排障。本机已配置项目专用原生 PostgreSQL，完整验证使用 local 数据库模式；bootstrap 仍可快速验证无数据库的基础进程。环境自检默认不连接 Docker，需要时使用 `Check-DevEnvironment.ps1 -IncludeDocker`。

```powershell
./tools/Verify-Foundation.ps1
./tools/Start-DevBackend.ps1 -Profile bootstrap
```

第一条命令依次运行后端构建测试与 Android 单元测试、APK 和 lint；第二条启动基础进程。详见 [第一阶段方案](phase1-implementation.md) 与 [后端模块说明](../backend/README.md)。

完整基础检查首次先准备本地数据库和质量工具：

```powershell
npm ci
./tools/Install-Postgres.ps1
./tools/Install-QualityTools.ps1
./tools/Verify-Foundation.ps1 -Full
./tools/Start-DevBackend.ps1 -Profile local
```

Full 包括真实 PG 迁移、专用集成库、事务与 RLS 验证、Android 构建、OpenAPI、格式、Actions 语法、秘密扫描与漏洞审计；需联网查询漏洞库，不以网络失败冒充零漏洞。扫描报告与完整日志保存在 `.dev`，不提交。

原生数据库位于 `.dev/postgres/data`，仅监听本机 55432，不安装 Windows 服务。启动 `Start-LocalDatabase.ps1` 保留已有密码与数据；停止 `Stop-LocalDatabase.ps1` 保留数据并移除数据库使用的工作区盘符别名。中文路径时通常使用 P:；运行期间须保留该别名，不要手动删除。

管理员、迁移和应用角色均独立。实际应用只有已授予的 SELECT/RLS 访问，ops 无读取资格，产品写权限暂未开放。测试使用独立 `geng_foundation_it` 数据库；管理员密码只用于初始化和测试，不注入日常后端进程。

## 已锁定的基础版本

| 组件 | 版本或设置 |
| --- | --- |
| JDK | Eclipse Temurin 21.0.13+9，下载按官方 SHA256 校验 |
| Maven | 3.9.16，官方 Wrapper 3.3.4，分发包带 SHA256 校验 |
| Spring Boot | 4.1.2 |
| Gradle | 8.14.6，Wrapper 分发包带 SHA256 校验 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin 与 Compose 编译插件 | 2.2.21 |
| Compose BOM | 2025.10.01 |
| Android 平台 | compile/target SDK 36，Build Tools 35.0.0 |
| Android 命令行工具 | 固定版 19.0；新版原生 CLI 在本机索引写入失败，不作为当前项目入口 |
| ADB | Platform Tools 37.0.1 |
| 开发数据库 | PostgreSQL 17.11 原生便携版已验证；容器仍为后续部署方式 |

Android 当前包名 `dev.geng.kaogengji`、最低 API 26 和版本 `0.1.0-dev` 是工程起步配置，发布前需结合设备范围与包名规划确认，不表示新增产品兼容承诺。

## 安装与环境激活

在仓库根目录使用 PowerShell 7：

```powershell
./tools/Install-Toolchain.ps1
. ./tools/Enter-DevEnvironment.ps1
./tools/Install-AndroidPackages.ps1
./tools/Initialize-DevEnvironment.ps1
./tools/Check-DevEnvironment.ps1
```

安装从 Adoptium、Apache Maven 与 Google 官方源获取工具并校验。SDK 许可在安装时按 Google 提示处理。SDK 安装结束后再启动 Gradle，避免两个安装器同时写入包目录。

JDK、Maven 与 Maven 缓存位于 `.tools/`。Gradle 缓存默认放在 `.tools/gradle-drive/home`。Windows 构建脚本遇到中文工作区路径时，临时使用空闲盘符作为工作区的英文路径别名，并在构建结束后移除；这不改变磁盘分区，用于避免 Java 测试 worker 的参数文件路径编码问题；Android SDK 默认在 `%LOCALAPPDATA%/Android/Sdk`。`dev.local.json` 保存本机路径，`android/local.properties` 由激活脚本生成，二者不提交 Git。环境激活只影响当前终端，不修改系统级 PATH。缓存换位置时不直接复用转换产物，避免其旧路径元数据导致失败。

本机初次 SDK 平台安装器在 Windows 文件复制阶段长时间停留，已从安装器校验解压后的官方平台源进行恢复。固定版本安装脚本与 Wrapper 仍保留标准入口。SDK 包完整性和 APK 构建需以实际验证结果为准。

## 后端构建与最小启动

```powershell
./tools/Invoke-Backend.ps1 verify
./tools/Start-DevBackend.ps1 -Profile bootstrap
```

bootstrap 是无数据库环境验证模式，默认只监听 `127.0.0.1:8080`。访问 `/actuator/health` 返回最少健康状态；其他入口默认拒绝，尚未实现邮箱登录或业务访问。它不能被当成数据库和产品已可用。

本地验证接口可用 `http://127.0.0.1:8080/actuator/health`。停止前台进程使用 Ctrl+C。健康检查不暴露数据库密码或完整配置，关闭默认生成密码登录，不提供开发后门账号。

后端测试会验证纯 Java 权限策略、跨空间上下文、模块依赖，并启动实际本地 HTTP 服务检查安全响应、脱敏错误及契约必填字段。它们不需要真实用户、数据库或云凭据。

后端已划分为 domain、application、api、infrastructure、bootstrap。唯一启动产物位于 `backend/bootstrap/target/geng-backend-0.1.0-SNAPSHOT.jar`，构建脚本仍从聚合工程统一构建。

## Android 构建

```powershell
./tools/Invoke-Android.ps1 testDebugUnitTest assembleDebug lintDebug
```

APK 输出为 `android/app/build/outputs/apk/debug/app-debug.apk`，属于调试产物，不是签名发布包。调试页包含基础连接诊断，图片效果图尚未实现为实际业务页面。

Android 内部区分 app 装配、ui、application、data 和 core；页面通过 ViewModel 调用诊断用例，HTTP 连接由 data 层在 IO 调度器执行。单元测试的本机 HTTP 服务器不属于正式业务存储，也不代表真机联调已通过。

调试包默认连接模拟器宿主地址 `http://10.0.2.2:8080`；网络安全配置仅对 localhost、127.0.0.1 和该模拟器地址开放调试 HTTP，客户端拒绝凭据 URL、查询参数和自动重定向。发布配置要求 HTTPS，当前发布域名为保留的 `.invalid` 占位值，尚未配置真实线上服务。开发诊断不出现在发布用户流程。

如果使用 Android Studio，打开 `android/`，选择 JDK 21 作为 Gradle JDK，使用同一 SDK。当前没有安装 Android Studio、模拟器或为真实设备授权 USB 调试；命令行构建与模拟器/真机运行是不同验收项。

现有仓库路径含中文，Android 工程设置 `android.overridePathCheck=true`；SDK 自身放在 ASCII 路径。当前无 NDK 或原生编译，今后加入原生组件需重新验证路径兼容。

## Docker 与数据库

以下作为后续可选基础设施说明，不阻塞当前基础开发。本机 Docker Desktop 已安装，初次启动日志报告 `WslVirtualMachinePlatformNotEnabled`。用户授权后，管理员检查确认 VirtualMachinePlatform 与 Microsoft-Windows-Subsystem-Linux 均为 Enabled，系统未报告需要重启。功能状态不代替容器引擎验证，数据库容器仍需实际启动验收。

确认允许系统变更后，在管理员 PowerShell 中运行：

```powershell
./tools/Enable-DockerPrerequisites.ps1
```

脚本只启用必要 Windows 功能，不自动重启。若 Windows 要求重启，保存工作后自行重启，再启动 Docker Desktop。脚本不能代替 BIOS 中的硬件虚拟化设置；只有实际启动检查能证明容器可用。

本机管理员检查虽未返回重启标记，但随后 WSL 仍报告虚拟化未就绪，Docker 启动异常退出。下一步先由用户自行重启后重试；不能把 feature Enabled 等同于 WSL2 已能运行，也不能据此直接断定固件设置关闭。

Docker 引擎可用后：

```powershell
./tools/Start-DevInfrastructure.ps1
./tools/Invoke-DatabaseMigration.ps1
./tools/Start-DevBackend.ps1 -Profile local
```

数据库仅映射本机 `127.0.0.1:55432`。管理员、迁移与应用运行账号分开；应用角色不具备超级用户、角色创建或绕过 RLS 权限。当前已执行 V1 模块 schema 与 V2 四张核心基础表、只读 RLS；完整业务写入和生命周期策略仍随功能阶段实现。

后续也可使用本机原生 PostgreSQL 或专用测试 PostgreSQL，按相同角色与 schema 契约配置 `.dev/local.env`；Docker 不是数据库连接的必需条件。本机已安装并验证项目专用原生 PostgreSQL，步骤见本文开头。

密码随机生成到 `.dev/local.env`，不会打印；配置模板 `.env.example` 不包含真实秘密。已有本地密码不会被重复初始化覆盖。初始化角色脚本只在全新数据库卷首次初始化时执行，修改环境文件不会自动修改已有数据库密码。

后端运行脚本只注入应用数据库凭据，迁移脚本在独立调用中使用迁移凭据。不要把整个 `.dev/local.env` 注入正式业务容器。

停止本项目开发容器而保留数据：

```powershell
docker compose --env-file .dev/local.env -f deploy/compose.dev.yml stop
```

不使用带 `-v` 的删除命令作为日常停止方式。Compose 单机环境不等于生产高可用，尚未配置云资源、正式邮箱、OSS、KMS 或生产秘密。

## CI 与版本管理

`.github/workflows/ci.yml` 提供后端构建测试和 Android 构建 lint 两个任务，Action 使用固定提交且只读仓库权限。该配置尚未推送执行，不声称 GitHub CI 已通过。

当前使用功能分支开发；没有自动提交、推送、创建 PR、修改远程保护规则或发布 APK。`.gitignore` 排除工具包、缓存、真实本地配置和构建产物，Wrapper 和迁移源文件应提交。

## 环境验收与剩余项

实际执行结果单独记录于 `docs/environment-status.md`。至少分别核验后端构建、打包启动、Android APK 与 lint、容器数据库和迁移；不能用其中一项成功代表全部就绪。

还需要产品后续决策的容量、删除时限和邀请期限等，不在本次环境配置中编造默认业务需求。
