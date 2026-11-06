# 双人梗簿技术选型与 API 数据库契约设计

2026年11月6日｜v0.1｜技术评审稿，尚非生产配置或已执行迁移

依据《需求决策记录_v0.2.md》与《双人梗簿_分层主体架构_v0.3.md》。本轮新增确认：账号注销时冻结相关活动空间；首版仅邀请已注册并验证邮箱的用户。以下技术选型、接口形式、字段与实现约束是设计建议，不假称已获产品确认。已有 Word PRD 尚未同步这些变更。

## 1 首选技术组合

在没有既有团队语言、存量代码及云厂商约束的前提下，首选以下组合；如团队已熟练掌握其他成熟栈，应把人员熟悉度计入决策，不因本建议机械换栈。

| 部分 | 首选 | 本产品的选择依据 |
| --- | --- | --- |
| Android | Kotlin 与 Jetpack Compose | 原生 Android 首发，适合后续媒体及设备权限能力；Compose 为 Android 官方现代界面工具 [S1] |
| 服务端 | Java 21 LTS、Spring Boot 4.x 稳定兼容版本、Spring MVC、Spring Security | 用于分层模块化应用，集中认证和事务；不采用 Spring Cloud 或微服务集群 [S2] |
| 数据访问 | Spring JDBC 的 JdbcClient、参数化 SQL、Flyway 迁移 | 本产品需要清楚表达权限过滤、版本和冻结边界；避免将 ORM 实体当作 API 返回模型 [S3] |
| 数据库 | PostgreSQL 17，采用部署时受支持的安全修订版本 | 关系约束、事务、行锁与 RLS 匹配成员隔离和一致性需求；17 仍在官方支持期 [S4][S5] |
| 对外接口 | HTTPS、REST、JSON、OpenAPI 3.1.1 | 显式描述请求响应、生成客户端并检查兼容性；3.1.1 为本项目契约基线，不宣称是最新版本 [S6] |
| 错误格式 | RFC 9457 Problem Details | 使用真实 HTTP 状态与稳定业务错误码 [S7] |
| 会话 | 不透明随机访问令牌与可轮换刷新令牌，服务端仅存摘要 | 便于退出登录、密码重置和注销时撤销，不依赖长期离线有效的自包含令牌 |
| 密码 | Spring Security 接入 Argon2id 实现 | 密码单向哈希，不可逆加密；参数结合 OWASP 建议与实例压测确定 [S8] |
| 部署 | Linux、Docker Compose、反向代理、独立后台任务进程 | 适合小范围试用；不将单机 Compose 误称为高可用平台 [S9] |
| 云资源 | 首选评估同一大陆地域的阿里云 ECS、托管 PostgreSQL、私有 OSS、KMS 与邮件推送 | 便于统一地域与权限管理；具体地域、价格、数据库版本可用性及邮件资格须采购前核实 [S10][S11] |

Java/Spring 的部署和工程配置成本通常比一个简单脚本服务高；这里是为了长期分层、明确事务及安全治理作出的推荐，并非声称它天然比其他语言更安全。

可选方案：FastAPI 与 SQLAlchemy 适合已有 Python 经验并重视早期迭代的团队；NestJS 适合已有 TypeScript 团队。当前没有这些团队约束，因此不将其列为首选。SQLite、单张 JSON 文档表或对象存储不作为本产品权威关系状态的首选。首版不需要 Redis、Kafka、Elasticsearch、GraphQL、gRPC 或 Kubernetes。

补丁版本、依赖锁定、镜像摘要及 Spring Boot、springdoc 与 Flyway 的兼容组合在项目初始化时验证固定。springdoc 3.x 对应 Spring Boot 4.x 的兼容线应按项目官方说明选用 [S12]，不直接使用 latest 镜像。

## 2 分层与契约的对应

API 层接收独立请求模型；应用用例协调操作及事务；领域层定义权限、作者归属、共同编辑和冻结规则；基础设施实现数据库、媒体、邮件和 KMS 端口。客户端模型、数据库模型和领域模型分开，不能因为某列新增就自动对外暴露。

对外以 OpenAPI 文件为契约源。服务端实现必须符合该契约；运行时生成的文档用于比对，不能反过来让实现意外改变契约。框架自动校验只是第一层，不能代替业务授权。

## 3 API 统一格式

### 3.1 命名与基本约定

- 路径前缀 `/api/v1`，使用复数资源名；本稿将梗记录命名为 `entries`。
- JSON 字段使用 camelCase；数据库列使用 snake_case，两者显式映射。
- 资源 ID 使用服务端生成的随机 UUID，作为字符串；用户可分享自己的用户 ID，但知道 ID 不代表拥有访问权。
- 时间统一为 UTC 的 RFC 3339 字符串，Android 转为用户本地时间展示。
- 成功响应直接返回资源对象或 `items` 列表，不额外包一层永远为真的 success/code；错误不返回 HTTP 200。
- JSON 请求为 `application/json`，错误为 `application/problem+json`；媒体正文走独立受保护的二进制接口，不使用 Base64 嵌入普通 JSON。
- 私密响应禁止公共缓存；日志不记录请求正文、Authorization、密码、验证凭据或图片内容。

### 3.2 成功响应与分页

示例中的梗文字与背景只是契约示例，不是实际用户资料。字段必填性中，本稿建议 phrase 为非空梗文字，context 可省略并规范为空字符串，图片可为空。是否允许只有图片需另行确认；这一建议不冒充已确认业务要求。

```json
{
  "id": "9f97c865-0f5f-44aa-9a57-5379ef9f2e59",
  "spaceId": "61acef42-263c-4f70-a2aa-b094b3bd8011",
  "authorId": "18f0156f-15e2-4f85-8901-1f882c597e84",
  "phrase": "我来早了",
  "context": "用于记录这句话当时发生的背景",
  "editMode": "AUTHOR_ONLY",
  "version": 1,
  "assets": [],
  "createdAt": "2026-11-06T08:00:00Z",
  "updatedAt": "2026-11-06T08:00:00Z"
}
```

列表采用游标分页，响应为 `items` 与可为空的 `nextCursor`。本稿建议按服务器 createdAt 与 ID 稳定倒序，不按尚未确认的事件发生时间排序。游标绑定账号、空间、排序与过滤上下文并校验完整性，不能成为绕过授权的凭据。分页默认值和上限属于后续可配置技术参数，当前不写未经确认的业务容量限制。

### 3.3 错误契约

```json
{
  "type": "urn:gengbook:problem:entry-version-conflict",
  "title": "记录已被更新",
  "status": 409,
  "code": "ENTRY_VERSION_CONFLICT",
  "detail": "请重新读取记录后再决定如何保存。",
  "requestId": "server-generated-request-id"
}
```

`code` 用于客户端分支，title/detail 用于展示，不让客户端解析中文判断错误。字段校验错误可以带不含敏感原值的 `errors`。无权限资源统一按 404 处理以减少存在性泄露；已确认是空间成员但违反编辑模式可返回 403。未登录为 401，状态或版本冲突为 409，字段不合法为 422，不支持的媒体为 415，超出体积限制为 413，限流为 429，依赖故障为 503。

本稿采用显式 expectedVersion 与 409，不同时引入 ETag/If-Match，避免两套并发版本规则混用。若未来改用标准条件请求，需要整体调整契约，不能只把状态码改为 412。

### 3.4 版本与重试

修改正文和附件时使用 PUT，完整提交可修改字段，省略的可选 context 规范为空，assetIds 必须提交完整保留列表；不采用含义模糊的 null 或任意 JSON Patch。编辑权限使用独立操作，仅原作者可调用。

请求体提交 expectedVersion；永久删除使用同名查询参数，避免依赖 DELETE 请求体。成功修改递增 version。版本校验与权限和冻结校验位于同一事务，不能先比较再无锁覆盖。版本号用于并发控制，不代表提供历史版本功能。

创建空间、邀请、创建记录、上传初始化、完成上传、退出及删除申请等需要可安全重试的操作携带 `Idempotency-Key`。记录用户、操作、目标及规范请求摘要；相同键不同内容返回 409，同键同内容返回最小结果，不再重复执行。幂等结果不缓存完整私密正文，且不能在账号已注销或资源已删除后重放旧正文。保存期限与重试窗口需在实施时统一配置并写入运行契约。

## 4 资源与接口范围

所有接口除注册、邮箱验证、登录、刷新及找回外默认需要有效会话；每个资源仍需单独授权。密码找回、会话轮换与二次验证是支持邮箱密码安全生命周期的技术建议，尚未作为新增产品界面确认。

| 类别 | 主要接口 | 语义 |
| --- | --- | --- |
| 注册验证 | POST /auth/registrations、POST /auth/email-verifications、POST /auth/email-verifications/resend | 注册待验证身份，验证邮箱归属；外部响应尽量不暴露邮箱是否注册 |
| 会话 | POST /auth/sessions、POST /auth/sessions/refresh、DELETE /auth/sessions/current | 登录、刷新轮换、撤销当前会话 |
| 找回与敏感确认 | POST /auth/password-reset-requests、POST /auth/password-resets、POST /auth/reauthentications | 一次性找回凭据、密码更新、注销前近期身份确认 |
| 自己 | GET /me | 自己的用户 ID 与必要账号状态，不提供公共用户目录 |
| 空间 | POST /spaces、GET /spaces、GET /spaces/{spaceId} | 创建、浏览自己的空间，查看空间状态 |
| 邀请 | POST /spaces/{spaceId}/invitations、GET /me/invitations | 按邮箱或用户 ID 发起，查看自己实际收到的邀请 |
| 邀请处理 | POST /invitations/{invitationId}/accept、/reject、/revoke | 接受、拒绝、撤销；接收者身份须准确匹配 |
| 退出 | POST /spaces/{spaceId}/exit | 任一方退出后整体冻结，双方保留读取资格 |
| 梗记录 | GET/POST /spaces/{spaceId}/entries、GET/PUT/DELETE /spaces/{spaceId}/entries/{entryId} | 列表、发布、读取、完整保存、永久删除 |
| 编辑模式 | PUT /spaces/{spaceId}/entries/{entryId}/permissions | 原作者切换 AUTHOR_ONLY 与 BOTH |
| 媒体 | POST /spaces/{spaceId}/uploads、PUT /spaces/{spaceId}/uploads/{uploadId}/content、POST /spaces/{spaceId}/uploads/{uploadId}/complete | 初始化、受控二进制上传、完成处理；首版只接受图片 |
| 媒体状态与读取 | GET /spaces/{spaceId}/assets/{assetId}、GET /spaces/{spaceId}/assets/{assetId}/content | 返回处理状态与授权后的媒体内容，不泄露永久公开地址 |
| 注销 | POST /me/deletion-requests | 近期身份确认后提交不可撤销清除请求，撤销账号访问并冻结相关空间 |
| 清除进度 | GET /deletion-operations/{operationId} | 查询自己发起的删除任务；注销后使用仅能查该进度的秘密回执 |

邮箱和用户 ID 邀请请求以 oneOf 表达，只允许一种目标字段。首版目标必须已注册且验证邮箱。建议统一 202 表示邀请请求已接收，并不承诺已送达；不存在、不符合条件的目标不向发起者暴露账号查询结果。不能在另一个“发送结果查询”接口重新暴露同样的信息。邀请内容最少化，未确认加入不能读取历史。

待加入空间是否可发布尚未决定，本稿建议首版只允许 ACTIVE 状态发布；这一分支在 OpenAPI 中标为待决策，不能以已确认需求实现。若允许提前发布，必须重新明确加入时的历史可见范围。

## 5 数据库布局与模型

### 5.1 首版建议单库多 schema

推荐一个 PostgreSQL 实例中的一个业务数据库，按 identity、space、memory、media、ops 划分 schema。不是一张大 JSON 表，也不是给每个用户或每个空间建一个数据库。

选择单库是为了让成员、权限、冻结、账号状态和清除标记统一协调，保留外键与本地事务。前期两个独立数据库的建议在本轮收敛为单库逻辑隔离，属于技术方案演进，不更改产品规则。

schema 不是自动安全屏障：撤销不必要的 PUBLIC 权限、按角色授权，并对内容表采用空间级 RLS。身份资料与正文仍分区存放、分别加密和授权。若未来必须隔离管理员或实例故障域，再单独拆库并设计跨库生命周期。

### 5.2 表分类

| schema 与表 | 主要字段或关系 | 关键约束 |
| --- | --- | --- |
| identity.principals | id、status、created_at | 不含邮箱和密码；作为业务归属标识；注销后是否保留最小不可登录占位标识需产品评审 |
| identity.credentials | principal_id、email_ciphertext、email_lookup_hmac、password_hash、email_verified_at | 用户唯一、邮箱匹配摘要唯一；只保存密码哈希 |
| identity.sessions | id、principal_id、access_token_hash、expires_at、revoked_at | 访问令牌摘要唯一；撤销和账号状态逐次核验 |
| identity.refresh_tokens | id、session_id、token_hash、consumed_at、expires_at | 单次轮换与重放检测，不存明文刷新令牌 |
| identity.verification_tickets | id、principal_id、purpose、secret_hash、expires_at、consumed_at | 邮箱验证、找回及近期确认按用途隔离，单次使用 |
| space.spaces | id、status、version、frozen_at、frozen_by | PENDING、ACTIVE、FROZEN；关闭待加入空间的规则待确认 |
| space.members | space_id、principal_id、slot、joined_at | 空间与用户唯一；slot 限制 1 或 2，空间与 slot 唯一 |
| space.invitations | id、space_id、inviter_id、可为空的 recipient_id、status、expires_at | 无匹配目标时仍可有统一受理回执但不可接受；真实邀请接受时核验接收者及容量 |
| memory.entries | id、space_id、author_id、payload_ciphertext、wrapped_key、key_version、edit_mode、version、时间 | 原作者与空间归属固定；编辑模式限 AUTHOR_ONLY、BOTH |
| media.assets | id、space_id、state、kind、受保护对象引用、wrapped_key、大小与类型 | 首版 kind 仅 IMAGE；正式对象不可由客户端原地覆盖 |
| media.entry_assets | space_id、entry_id、asset_id、position | 联合外键保证同空间；建议同一 asset 仅属于一条记录，不跨记录共享 |
| media.uploads | id、space_id、requester_id、目标记录或新建请求、状态、expires_at | 上传身份与目标绑定，完成不等于已关联发布 |
| ops.idempotency | 用户、操作、目标、key、request_hash、最小结果与期限 | 作用域唯一，禁止存完整旧正文 |
| ops.deletion_operations | id、requester_id、scope、target、status、receipt_hash、清除阶段 | 支持失败重试；回执只查询本任务，不恢复账号权限 |
| ops.outbox | id、事件类型、资源引用、版本、处理状态 | 与关键变更同事务；不存正文和图片 |

安全审计独立受保护，不默认与业务进程共享任意删除权限；具体日志设施不在关系表中强行展开。表数量随具体职责细化调整，不为减少表数把全部对象塞进 JSON。

### 5.3 字段、索引与约束

标识建议 UUID，时间为 timestamptz，版本为 bigint，大小使用非负整数，枚举用受约束文本。正文中的 phrase 与 context 作为一个经过认证加密的 payload 存为 bytea；归属、权限、版本等需要关系约束的元数据仍为独立列。jsonb 只用于经过约束的非敏感技术扩展元数据，不保存未加密正文。

核心索引建议覆盖成员用户查空间、空间下记录按时间和 ID 分页、记录原作者清除范围、接收者待处理邀请、到期票据及待处理任务。正文密文不建立全文索引；不因为将来可能搜索而额外复制明文。

entry_assets 通过 `(space_id, entry_id)` 和 `(space_id, asset_id)` 联合外键引用对应唯一键；成员与作者关系由约束及用例共同核验。原作者和空间归属不可通过编辑接口改变。删除主体身份不允许使用一个全局 ON DELETE CASCADE 误删对方空间或回忆；需要受控清除用例。

RLS 对记录、图片、成员及上传等按可信请求身份隔离。业务角色不得是表所有者、超级用户或 BYPASSRLS；必要时 FORCE RLS。连接池用户上下文限定在当前事务。RLS 策略不得递归查询自身而未处理，也不能只依赖应用可随意传入的 spaceId。[S5]

## 6 并发与状态契约

### 6.1 状态

空间：PENDING → ACTIVE → FROZEN；已冻结不能通过普通接口恢复，恢复能力不纳入当前草案。邀请：PENDING → ACCEPTED、REJECTED、REVOKED 或 EXPIRED；并发接受最多占用一个空位。上传和资源分别区分上传中、处理中、可用、清除中及失败，不把不同对象生命周期强行合成一个状态。

梗的普通编辑不是实时协同，提交 expectedVersion，不一致时返回 ENTRY_VERSION_CONFLICT。领域检查当前操作者、原作者、编辑模式及空间状态；技术层以明确事务和锁保证检查与变更之间没有空隙。

### 6.2 冻结与注销

所有内容变更先进入相同写入协调路径，普通请求锁定空间再校验和处理目标记录；多空间、多记录操作按稳定 ID 排序。账号注销与邀请创建或接受等涉及账号资格的命令，还需要先按统一顺序锁定相关主体资格。最终锁顺序应统一为主体资格、空间、记录，不能有某一路径逆序。

注销提交先将账号置为不可继续使用，撤销会话，冻结其活动空间，建立清除任务。与该账号相关的新增成员或写入在最终资格校验时被阻止。对外读取同时排除已进入清除的原作者记录，随后任务物理清理正文、媒体、凭据和技术副本。具体大批量分段处理不能造成注销提交后旧内容又恢复可见。

双方历史读取资格与账号在线资格是不同概念。另一位仍有效用户继续查看自己保留的内容；被注销账号不能凭原会话继续查看。记录归属按最初作者，不能按 last_updated_by 删除；对方记录中由注销者参与编辑的内容按已确认规则仍保留。

注销清除是冻结后的受控例外，普通删除仍被冻结状态拒绝。后台清除不能拿到通用超级用户身份，而应按清除任务限定目标；验证不会误删另一方内容。

## 7 永久删除的返回与恢复契约

建议删除接口返回 202 和 deletion operation。202 表示请求已接受、在线访问按约定已撤销，不表示所有对象与备份已瞬间物理擦除。删除后不提供回收站；后台任务对正文关联、图片及派生资源、未完成上传和相关缓存执行幂等清理。

注销后原会话失效，不能依赖原登录态轮询。因此在注销前的近期身份确认步骤预留任务 ID 并签发高熵秘密回执，客户端先安全保存再提交注销。服务端仅保存回执摘要，通过 `Deletion-Receipt` 请求头仅查询该次任务的最小进度，无权查看或恢复任何回忆；即使注销提交响应丢失，仍能用预先保存的回执查询。未实际提交的预留任务返回统一未找到状态，不能作为读取内容的入口。普通记录删除任务使用有效登录态查询，且只对原请求者可见。

删除前把清理所需对象引用写入受控任务，再删除引用数据，避免级联删除元数据后遗失资源清理目标。清理任务完成状态、在线不可访问状态与备份过期清除阶段分别表达。任务结果不对外宣称未经证实的物理完成。已发布附图的清除归属随所在梗的最初作者，不单按图片上传者删除，否则会误删注销者为另一方梗补充但按规则应保留的图片。

每条梗及媒体建议使用独立数据密钥，KMS 包装密钥分离管理，不使用“销毁整个空间密钥”来删除某位作者内容。删除密钥不能自动抹除旧备份中的密钥副本；必须有备份期限、受保护的删除记录及恢复后再次清除流程。具体时限待确认。

## 8 媒体契约与扩展

首版为控制访问与实现复杂度，推荐图片二进制通过受限 API 流式上传与读取；不向客户端发放正式资源覆盖权限或永久公开链接。上传体积、处理超时及内存使用必须有可配置边界，数值在容量决策后确定。未来实际带宽需要时，再评估仅暂存区直传。

上传完成时校验真实文件类型并在受限处理组件中处理，正式资源创建后返回可用 assetId；发布或修改梗时才在事务中关联，重新校验空间、作者、版本、上传者及目标。针对新建记录上传绑定新建请求；针对共同编辑上传绑定目标记录，不能仅要求上传者等于原作者，否则会错误禁止朋友添加图片。

正式对象和派生图片准备完成后才可发布，避免冻结后异步替换可见内容。未关联资源受控清理，不能与发布关联竞争而误删；正式对象使用新键或固定版本，不可原地覆盖。

首版契约 kind 仅允许 IMAGE；将来扩展 AUDIO 时增加相关请求和处理策略，旧客户端必须有未知类型的兼容处理或受支持版本限制，不能因加枚举就假定旧客户端不会崩溃。共同编辑、冻结、归属和删除复用原规则。

## 9 契约交付与校验

配套 `contracts/openapi.draft.json` 是 OpenAPI 3.1.1 评审草案，覆盖核心资源、请求、响应、安全方式与错误模型；不是已运行服务。未确定的容量、邀请期限等以说明或 x-pending-decisions 标明，不填虚假业务限制。

实施前应完成：OpenAPI 规范与引用校验、接口变更差异检查、客户端模型生成验证、请求响应契约测试及真实 PostgreSQL 权限与并发集成测试。SQLite 或纯 mock 不能证明 RLS、锁和事务正确。Java 侧可选 JUnit 与 Testcontainers 建立这些验证。

OpenAPI 不能表达全部规则，必须与本稿的权限表、状态机、幂等和删除语义共同构成契约。开发阶段不把参数校验通过视为业务授权通过。

## 10 还需确认的产品分支

1. phrase 必填、context 可空及首版是否允许纯图片；图片、正文及空间容量。
2. 朋友加入前能否记录；本稿建议先只允许 ACTIVE 发布。
3. 邀请期限、撤销、拒绝及待加入空间的取消流程；草案提供操作但不自定有效期。
4. 账号注销后保留最小匿名主体标识及显示“已注销用户”的方案。
5. 永久删除的物理清除和备份期限，令牌、票据与幂等窗口配置。
6. 云预算、具体地域、邮件域名与投递配置、最低 Android 版本与可靠性目标。

这些未决项不妨碍评审技术方案，但影响对应功能最终实现与上线验收。本文未执行云资源购买、数据库迁移、服务部署或真实邮件发送。

## 参考资料

核对日期为 2026年11月6日；只用官方项目、规范与厂商材料核对能力，具体组合和优先级为本项目设计建议。

- [S1 Android Jetpack](https://developer.android.com/jetpack)
- [S2 Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [S3 Spring JDBC Core](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)
- [S4 PostgreSQL Versioning Policy](https://www.postgresql.org/support/versioning/)
- [S5 PostgreSQL Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [S6 OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html)
- [S7 RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)
- [S8 OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [S9 Docker Compose Production](https://docs.docker.com/compose/how-tos/production/)
- [S10 OSS Authorization](https://www.alibabacloud.com/help/en/oss/user-guide/authentication)
- [S11 Direct Mail Overview](https://www.alibabacloud.com/help/en/direct-mail/user-guide/overview)
- [S12 springdoc-openapi](https://github.com/springdoc/springdoc-openapi)
