# 考梗记

朋友之间共同记录梗与回忆的 Android App。Git 仓库名为 `geng`。

每个空间由两位朋友共享，一个账号可以与不同朋友分别建立空间。记录以简短的梗文字为核心，可补充背景及图片。默认仅原作者编辑和删除，也可由原作者开启双方编辑。任一方退出后空间冻结，双方保留只读历史；注销清除原作者内容的规则优先于冻结保留。

## 当前状态

项目处于工程基础阶段。已有 Android Compose 工程、Spring Boot 分层骨架、真实 PostgreSQL 基础表与 RLS、事务入口、构建 Wrapper 与开发检查脚本。当前界面是环境验证页；真实邮箱账号、可变梗记录、私密图片和清除闭环尚未实现。

日常基础开发不需要 Docker。后端按 domain、application、api、infrastructure、bootstrap 分模块并生成一个应用；Android 区分 UI、用例和网络适配，调试页可检查基础服务连接。

```powershell
./tools/Verify-Foundation.ps1
./tools/Start-DevBackend.ps1 -Profile bootstrap
```

第一阶段的实施范围与验收条件见 [实施方案](docs/phase1-implementation.md)。

业务边界见 [模块说明](docs/modules.md)，本轮交付见 [验收表](docs/phase1-acceptance.md)。安装原生 PostgreSQL 与质量工具、执行 `npm ci` 后，可运行 `./tools/Verify-Foundation.ps1 -Full` 进行数据库、构建与工程质量完整检查；不依赖 Docker。

开发配置与验证状态见 [开发环境说明](docs/development.md)。

正式名称为“考梗记”。早期名称“朋友的梗”和“双人梗簿”保留在历史提案及文件名中便于追踪；产品规则以最新需求决策记录为准，旧版 Word 尚未同步全部补充。

## 当前设计基线

- [最新需求决策](docx/需求决策记录_v0.2.md)
- [分层主体架构](docx/双人梗簿_分层主体架构_v0.3.md)
- [技术选型与契约设计](docx/技术选型与API数据库契约设计_v0.1.md)
- [开发阶段与 Git 管理](docx/开发阶段与Git版本管理计划_v0.1.md)
- [OpenAPI 评审草案](contracts/openapi.draft.json)
- [数据库逻辑模型](contracts/database-model.dbml)

## 技术方向

当前建议 Android 采用 Kotlin 与 Jetpack Compose；服务端采用 Java 21、Spring Boot、Spring Security 与 Spring JDBC；数据库采用 PostgreSQL；中国大陆 Docker 部署并开展小范围试用。具体依赖版本在工程初始化时核验锁定。

服务端采用 API、应用用例、领域逻辑与基础设施四层的模块化单体。语音与多人空间为后续方向，不是首版已实现能力。首版采用服务端受控解密，不宣称端到端加密。

## 仓库目录

```text
android/      Android Compose 启动工程
backend/      Spring Boot 分层基础工程
contracts/    接口与数据库模型草案
deploy/       Docker 与开发数据库配置
design/       视觉提案、效果图与设计说明
docs/         开发环境与验证记录
docx/         需求、架构和开发计划
tools/        工具链安装、自检与构建启动脚本
.prd-work/    现有文档与契约生成工具
```

`android/`、`backend/`、`deploy/` 与 `tools/` 已进入工程基础配置；环境验证页和默认拒绝访问不代表完整产品功能已实现。

## 开发约定

使用短期功能分支，经检查后合并 main。需求、接口和实现相关变化保持同步。真实邮箱、回忆、图片、令牌、云密钥、数据库备份和 Android 签名私钥不得提交 Git。

效果图用于讨论布局、风格与交互，不代表功能已经实现，也不自动增加新需求。
