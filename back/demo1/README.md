# AI 个人关系记忆助手

一个基于 Spring Boot、Vue、LangChain4j、Redis Vector 和 MySQL 的 AI 关系记忆系统。项目目标是帮助用户管理人物信息、长期记忆、私有知识库和 AI 聊天上下文，让 AI 能结合人物记忆和知识库进行回答。

## 功能模块

- 登录注册：支持账号注册、登录、JWT 登录校验。
- 找回密码：通过 Redis 保存 1 分钟验证码，校验后使用 BCrypt 重置密码。
- 人物管理：支持新增、查询、修改、删除人物。
- 人物记忆：支持记录爱好、说过的话、性格特点、不喜欢、重要日期、备注等信息。
- AI Tool：AI 可以查询人物记忆，也可以根据用户指令保存新记忆。
- 知识库管理：支持手动新增知识，上传 TXT/PDF 文件。
- RAG 检索：知识库内容切片后写入 Redis 向量库，聊天时按用户隔离检索。
- 引用来源：AI 回答可以展示参考文档来源，并在前端查看原文。
- 聊天历史：保存会话、消息、memoryId，支持恢复历史会话和搜索聊天记录。
- 首页仪表盘：展示人物数量、知识库数量、聊天会话数量、聊天消息数量和最近操作。
- 操作日志：记录用户新增人物、上传知识库、提交意见等关键操作。
- 意见收集箱：用户可以提交意见，系统自动/手动分类，并查看自己的历史意见。
- 品牌化前端：黑灰红 UI、公司 Logo、视频展示和 slogan。
- 迭代计划：展示已完成功能和后续待开发功能。

## 技术栈

后端：

- Java 17
- Spring Boot 4.1.0
- Spring WebMVC / WebFlux
- MyBatis
- MySQL
- Redis / Redis Stack
- LangChain4j
- PDFBox
- JWT
- BCrypt
- Spring Validation
- Springdoc OpenAPI / Swagger UI

前端：

- Vue 2.7
- Vue Router
- Fetch API
- CSS

## 目录说明

后端项目：

```text
D:\Java_project\agent\demo1
```

前端项目：

```text
C:\Users\yanghao\Desktop\codex\relationship-agent-web
```

## 启动准备

需要先启动：

- MySQL
- Redis Stack
- Spring Boot 后端
- Vue 前端

Redis Stack 需要支持 RedisSearch，否则向量库功能无法使用。可以用下面命令确认：

```bash
docker exec -it ai-agent-redis redis-cli MODULE LIST
```

看到 `search` 模块表示 RedisSearch 可用。

## 数据库

当前数据库配置在：

```text
src/main/resources/application.properties
```

核心配置示例：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/agent_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

项目里已经提供部分 SQL：

```text
src/main/resources/chat_history.sql
src/main/resources/company_feedback.sql
```

意见箱表：

```sql
CREATE TABLE company_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '意见ID',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    category VARCHAR(30) NOT NULL COMMENT '意见分类',
    content TEXT NOT NULL COMMENT '意见内容',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '处理状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='战狼公司意见收集箱';
```

## 后端启动

在后端目录执行：

```bash
mvn spring-boot:run
```

或者使用 IDE 启动：

```text
Demo1Application
```

后端默认地址：

```text
http://localhost:8080
```

## 前端启动

在前端目录执行：

```bash
npm install
npm run serve
```

前端默认地址：

```text
http://localhost:8081
```

前端通过代理访问后端：

```text
/api -> http://localhost:8080
```

## Swagger 接口文档

后端启动后访问：

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON：

```text
http://localhost:8080/v3/api-docs
```

需要登录的接口在 Swagger 页面中点击 `Authorize`，填写：

```text
Bearer 你的JWT
```

JWT 获取方式：

```http
POST /auth/login
```

请求体：

```json
{
  "username": "1",
  "password": "1"
}
```

## 常用接口

认证：

```text
POST /auth/register
POST /auth/login
POST /auth/forgot-code
POST /auth/reset-password
GET  /auth/check
```

人物：

```text
GET    /persons
POST   /persons
GET    /persons/{id}
PUT    /persons/{id}
DELETE /persons/{id}
```

人物记忆：

```text
GET    /persons/{personId}/memories
POST   /persons/{personId}/memories
DELETE /persons/{personId}/memories/{memoryId}
GET    /person-memories/search
```

知识库：

```text
GET    /knowledge
POST   /knowledge/text
POST   /knowledge/upload
GET    /knowledge/{id}
GET    /knowledge/{id}/chunks
PUT    /knowledge/{id}
DELETE /knowledge/{id}
```

AI 聊天：

```text
GET /ai/chat
```

聊天历史：

```text
GET    /chat-sessions
GET    /chat-sessions/{sessionId}/messages
GET    /chat-sessions/search
PUT    /chat-sessions/{sessionId}/title
DELETE /chat-sessions/{sessionId}
```

仪表盘和日志：

```text
GET /dashboard/stats
GET /operation-logs/recent
```

意见箱：

```text
GET  /feedback
POST /feedback
```

## 意见箱分类

意见箱支持用户选择分类，也支持 `AUTO` 自动分类。

分类值：

```text
FEATURE     功能建议
BUG         问题反馈
EXPERIENCE  体验优化
AI          AI建议
OTHER       其他
```

当前状态值：

```text
SUBMITTED 已提交
REVIEWED  已查看
DONE      已处理
```

## 项目学习重点

这个项目覆盖了从基础业务到 AI 应用落地的完整链路：

- Controller / Service / Mapper 分层
- DTO 和参数校验
- JWT 鉴权和用户数据隔离
- Redis 临时验证码
- BCrypt 密码加密
- 文件上传和 PDF 解析
- RAG 知识库检索
- LangChain4j Tool 调用
- 聊天历史保存
- 操作日志和仪表盘
- Swagger 接口文档
- 前后端分离联调

## 后续迭代方向

- 人物画像页
- AI 自动整理记忆
- 记忆确认机制
- 意见箱管理员回复
- AOP 操作日志
- Docker 部署
- 管理员后台
