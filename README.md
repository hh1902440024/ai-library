# AI 智能书屋（AI Smart Library）

前后端分离的智能图书管理系统：在经典"用户 / 图书 / 借阅"能力之上，引入 LLM 智能体，提供 **AI 问答助手（流式）、自然语言搜书、个性化推荐、图书自动摘要** 四项智能能力。

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Spring Boot 3.5 (Java 17) · MyBatis + PageHelper · MySQL 8 · JWT (jjwt 0.12) · Knife4j · POI · Lombok |
| 前端 | Vue 3.5 + TypeScript · Vite · Element Plus · Pinia · Vue Router · Axios · ECharts · markdown-it |
| AI | 智谱 GLM-4-Flash（OpenAI 兼容接口，SSE 流式），JDK HttpClient 接入 |

## 目录结构

```
ai-library/
├── ai-library-server/     # 后端（Spring Boot）
│   └── src/main/java/com/library/
│       ├── controller/    # REST 接口
│       ├── service/       # 业务逻辑（事务）
│       ├── mapper/        # MyBatis Mapper（XML 在 resources/mapper）
│       ├── entity/ dto/   # 实体与传输对象
│       ├── interceptor/   # JWT 拦截器、登录上下文、@Anonymous/@RequireAdmin
│       ├── exception/     # BizException + 全局异常处理
│       ├── common/ util/  # Result 统一响应、Jwt/Bcrypt/Excel 工具
│       └── ai/            # ★ AI 智能体（LlmClient/BookRetriever/PromptBuilder/AiChatService）
└── ai-library-web/        # 前端（Vue 3 + TS）
    └── src/
        ├── api/           # auth/book/borrow/ai/stats/user 接口封装
        ├── router/ store/ # 路由守卫、Pinia 用户状态
        ├── components/    # BasicLayout/AdminLayout/BookCard/ChatMessage/StatsChart
        └── views/         # 前台 8 页 + 管理后台 5 页
```

## 快速启动

### 1. 准备数据库

MySQL 8 执行建表 SQL（见《AI智能书屋项目设计方案.md》§5.2，库名 `ai-library`）。
初始管理员：`admin / 123456`。

> 若数据库账号密码不同，请修改 `ai-library-server/src/main/resources/application.yml` 的 `spring.datasource`。

### 2. 启动后端（端口 8080）

```bash
cd ai-library-server
./mvnw spring-boot:run
```

接口文档：http://localhost:8080/doc.html

### 3. 启动前端（端口 5173，已配置 /api 与 /upload 代理）

```bash
cd ai-library-web
npm install
npm run dev
```

访问 http://localhost:5173

### 4. 启用 AI 功能（可选）

AI 功能依赖大模型 API key（智谱 GLM-4-Flash 免费申请：https://open.bigmodel.cn ）。

```bash
# Windows (Git Bash)
export AI_API_KEY=你的key
./mvnw spring-boot:run

# Windows (CMD)
set AI_API_KEY=你的key
mvnw.cmd spring-boot:run
```

不配置 key 时 AI 对话/推荐/摘要自动降级为提示文案，其余功能不受影响。
更换 DeepSeek / 通义千问等 OpenAI 兼容模型只需改 `application.yml` 中 `ai.base-url / model / api-key`。

## 主要接口一览

| 模块 | 端点（前缀 /api） | 权限 |
|------|------------------|------|
| 认证 | POST /auth/register、/auth/login | 公开 |
| 用户 | GET/PUT /users/me、PUT /users/password；GET /users、PUT /users/{id}/status|role | 登录 / 管理员 |
| 图书 | GET /books（分页筛选）、/books/{id}、/books/hot、/books/{id}/similar；POST/PUT/DELETE /books、POST /books/{id}/summary | 查询公开 / 管理维护 |
| 分类 | GET /categories；POST/PUT/DELETE | 公开 / 管理员 |
| 借阅 | POST /borrows、PUT /borrows/{id}/return、GET /borrows/my；GET /borrows、/borrows/export、POST /borrows/mark-overdue | 登录 / 管理员 |
| AI | POST /ai/chat（SSE 流式）、GET /ai/sessions、/ai/sessions/{id}/messages、DELETE /ai/sessions/{id}、GET /ai/recommend | 登录 |
| 统计 | GET /stats/dashboard、/stats/overview、/stats/trend、/stats/category、/stats/hot | 管理员 |
| 上传 | POST /upload/image（头像/封面） | 登录 |

## 说明

- 登录态：JWT（24h 过期），前端 401 自动登出并跳转登录页。
- 防超卖：借书使用 `UPDATE ... WHERE available_stock > 0` 乐观扣减 + 事务。
- 逾期：每日 0 点定时标记，管理员也可在"借阅管理"页手动执行。
- 密码：BCrypt 加盐存储；登录失败 5 次锁定 10 分钟。
