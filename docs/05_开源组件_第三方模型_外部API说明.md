# 开源组件、第三方模型、外部 API 使用说明

## 1. 说明目的

根据比赛要求，作品使用开源组件、第三方模型、外部 API 或云服务时，需要如实说明依赖关系和使用方式。本文件用于说明“识忆承缘”系统中使用的主要外部依赖。

## 2. 前端开源组件

### 2.1 Vue

用途：

- 构建前端单页面应用。
- 实现登录、聊天、人物管理、知识库、图片分析、管理员后台等页面。

版本：

```text
Vue 2.7.x
```

### 2.2 Vue Router

用途：

- 管理前端页面路由。
- 实现 `/login`、`/dashboard`、`/chat`、`/persons` 等页面跳转。

### 2.3 Vue CLI

用途：

- 前端项目构建和打包。
- 通过 `npm run serve` 启动开发环境。
- 通过 `npm run build` 生成生产环境 `dist` 文件。

## 3. 后端开源组件

### 3.1 Spring Boot

用途：

- 构建后端 Web 服务。
- 提供 Controller、Service、配置管理和依赖注入能力。

### 3.2 Spring MVC

用途：

- 提供 REST API。
- 接收前端请求并返回 JSON 数据。

### 3.3 Spring WebFlux

用途：

- 支持流式或异步相关能力。

### 3.4 MyBatis

用途：

- 操作 MySQL 数据库。
- 完成用户、人物、记忆、聊天、知识库、日志等数据的增删改查。

### 3.5 MySQL Connector/J

用途：

- Java 后端连接 MySQL 数据库。

### 3.6 Spring Data Redis

用途：

- 连接 Redis。
- 支持验证码缓存、向量检索相关数据访问。

### 3.7 LangChain4j

用途：

- 接入大模型服务。
- 调用聊天模型、Embedding 模型和相关 AI 能力。
- 支持知识库向量检索和 AI 服务封装。

### 3.8 LangChain4j Redis Community

用途：

- 将 Redis 作为向量存储。
- 支持知识库 Embedding 的写入和相似度检索。

### 3.9 JWT

用途：

- 实现用户登录后的身份认证。
- 前端请求通过 Authorization 请求头携带 Token。

### 3.10 BCrypt

用途：

- 对用户密码进行加密存储。
- 登录时校验密码。

### 3.11 Apache PDFBox

用途：

- 支持 PDF 文档内容解析。
- 用于知识库文件上传中的 PDF 内容提取。

### 3.12 SpringDoc OpenAPI / Swagger

用途：

- 自动生成接口文档。
- 支持在线查看和测试接口。

## 4. 第三方模型与外部 API

### 4.1 DashScope OpenAI-compatible API

系统通过 DashScope 的 OpenAI-compatible API 接入大模型。

用途：

- AI 聊天回答
- 图片分析
- 知识库 Embedding
- 关系维护建议生成
- 人物记忆相关智能处理

配置项示例：

```properties
langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.api-key=${DASHSCOPE_API_KEY}
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=${DASHSCOPE_API_KEY}
```



## 5. 云服务与部署依赖

### 5.1 云服务器

用途：

- 部署前端、后端、MySQL 和 Redis。
- 提供线上演示环境。

### 5.2 Docker

用途：

- 容器化部署前端、后端、MySQL 和 Redis。

### 5.3 Docker Compose

用途：

- 编排多个容器。
- 一键启动系统所需服务。

### 5.4 Nginx

用途：

- 前端静态资源服务。
- 将 `/api/` 请求转发到后端服务。

### 5.5 Caddy

用途：

- 提供 HTTPS。
- 将域名请求反向代理到前端容器。

### 5.6 Cloudflare

用途：

- 域名解析。
- HTTPS 和代理能力。

## 6. 数据集说明

本项目不依赖公开训练数据集，不进行自训练模型训练。系统中的数据主要来自：

- 用户主动创建的人物档案。
- 用户录入的人物记忆。
- 用户上传的知识库文件。
- 用户上传的图片。
- 用户聊天历史。

这些数据仅用于当前系统业务流程，不作为公开数据集发布。

