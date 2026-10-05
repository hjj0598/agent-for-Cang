# 识忆承缘

## 项目简介

识忆承缘是一套面向个人关系管理场景的多模态长期记忆智能体系统。系统围绕“识别关系信息、沉淀长期记忆、辅助关系维护”构建完整业务流程，支持用户通过人物档案、知识库、聊天历史和图片分析等方式记录关系信息，并结合大模型、向量检索和长期记忆机制，为用户提供具有上下文理解能力的 AI 对话与关系维护建议。

本项目不是单纯的 AI 聊天页面，而是将智能技术嵌入真实业务流程中，形成从信息采集、记忆沉淀、上下文调用到主动建议生成的智能体闭环。

线上演示地址：

```text
https://jj.warwolf.cc.cd/login
```

## 项目定位

项目名称：识忆承缘

项目类型：多模态长期记忆智能体系统

应用场景：个人关系管理、人际关系维护、人物信息沉淀、私域知识问答

核心目标：

- 帮助用户记录和管理重要人物关系。
- 将聊天、图片、知识库等非结构化信息转化为长期记忆。
- 在后续对话中调用人物记忆和知识库内容，生成更贴近上下文的回答。
- 根据重要日期、人物档案和历史记忆生成关系维护建议。

## 核心功能

### 1. 用户认证与权限体系

系统支持用户注册、登录、JWT 鉴权、密码加密、登录失败次数限制和管理员角色控制。普通用户只能访问自己的数据，管理员可以进入管理台查看系统整体数据。

### 2. 人物关系管理

用户可以创建人物档案，记录人物姓名、关系、备注等基础信息。人物档案是后续长期记忆、聊天上下文和关系维护建议的基础。

### 3. 长期记忆管理

系统支持围绕具体人物沉淀长期记忆，例如兴趣爱好、重要事件、生日纪念日、相处偏好等。AI 在后续对话中可以结合这些记忆进行回答。

### 4. AI 聊天与历史记录

系统支持 AI 对话、聊天历史保存、历史会话恢复和会话搜索。用户点击历史会话后，可以继续上次的对话上下文。

### 5. 知识库与向量检索

用户可以手动录入或上传知识库文件。后端会对知识内容进行切分、生成 embedding，并写入 Redis 向量库。用户提问时，系统会先检索相关知识片段，再将检索结果与问题一起交给大模型生成回答。

### 6. 图片分析

系统支持上传图片进行 AI 分析，可用于聊天截图识别、图片内容理解、文字提取和关系信息提取。该功能体现了视觉模型与关系记忆智能体的结合。

### 7. 重要日期识别与关系维护建议

系统可以围绕人物记忆和重要日期生成提醒，并结合人物上下文给出 AI 关系维护建议，帮助用户从“被动记录”转向“主动维护关系”。

### 8. 意见收集箱

系统提供“战狼公司意见收集箱”，用户可以提交意见和建议，并查看自己提交过的内容。该功能用于收集用户反馈，支持后续产品迭代。

### 9. 管理员后台

管理员可以查看系统用户、数据统计、操作日志、反馈意见等信息，提升系统的运营和管理能力。

### 10. 操作日志与 Swagger 文档

系统通过 AOP 记录关键操作日志，并集成 Swagger/OpenAPI 自动生成接口文档，便于接口测试、前后端联调和后期维护。

## 技术栈

### 前端

- Vue 2.7
- Vue Router
- Vue CLI
- 原生 Fetch API
- CSS 响应式布局

### 后端

- Java 17
- Spring Boot
- Spring MVC
- Spring WebFlux
- MyBatis
- MySQL
- Redis
- LangChain4j
- JWT
- BCrypt
- Swagger / OpenAPI
- Apache PDFBox

### AI 能力

- DashScope OpenAI-compatible Chat Model
- DashScope Embedding Model
- 向量检索
- RAG 知识库问答
- 图片理解与信息提取
- 长期记忆增强对话

### 部署

- Docker
- Docker Compose
- Nginx
- Caddy
- Cloudflare
- Linux 云服务器

## 系统架构

```text
用户浏览器
   ↓
Vue 前端页面
   ↓
Nginx 前端容器
   ↓ /api
Spring Boot 后端容器
   ↓
MySQL：用户、人物、聊天记录、知识库文档、操作日志
Redis：验证码、缓存、向量检索数据
   ↓
DashScope 大模型：AI 对话、Embedding、图片分析
```

## 智能体闭环

```text
多源信息输入
  ↓
文本 / 图片 / 知识库内容解析
  ↓
AI 提取人物信息和关系事实
  ↓
用户确认或系统保存长期记忆
  ↓
后续聊天自动结合人物记忆和知识库
  ↓
生成个性化回答和关系维护建议
```

该闭环使系统区别于普通 AI 聊天工具。普通聊天机器人只回答当前问题，而识忆承缘能够持续积累用户关系上下文，并在后续任务中复用这些记忆。

## 项目结构

```text
shiyi-chengyuan
├── back
│   └── demo1
│       ├── src
│       ├── pom.xml
│       └── ...
├── frontend
│   └── relationship-agent-web
│       ├── public
│       ├── src
│       ├── package.json
│       └── ...
├── .gitignore
└── README.md
```

## 本地运行

### 1. 启动 MySQL 和 Redis

本项目需要 MySQL 和 Redis。可以使用本地服务，也可以使用 Docker 启动。

数据库名称示例：

```text
agent_test
```

### 2. 配置后端

进入后端目录：

```cmd
cd back\demo1
```

在 `application.properties` 中配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/agent_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的数据库密码

spring.data.redis.host=localhost
spring.data.redis.port=6379

langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.api-key=你的API_KEY
langchain4j.open-ai.chat-model.model-name=你的模型名称
```

### 3. 启动后端

```cmd
mvn spring-boot:run
```

或打包运行：

```cmd
mvn clean package -DskipTests
java -jar target\demo1-0.0.1-SNAPSHOT.jar
```

后端默认运行在：

```text
http://localhost:8080
```

### 4. 启动前端

进入前端目录：

```cmd
cd frontend\relationship-agent-web
```

安装依赖：

```cmd
npm install
```

启动开发环境：

```cmd
npm run serve
```

前端开发环境一般运行在：

```text
http://localhost:8082
```

具体端口以终端输出为准。

## 打包与部署

### 前端打包

```cmd
cd C:\Users\yanghao\Desktop\codex\relationship-agent-web
npm run build
```

生成目录：

```text
dist
```

### 后端打包

```cmd
cd D:\Java_project\agent\demo1
mvn clean package -DskipTests
```

生成 JAR：

```text
target\demo1-0.0.1-SNAPSHOT.jar
```

### 服务器目录结构

```text
/opt/relationship-agent
├── .env
├── docker-compose.yml
├── frontend
│   ├── dist
│   ├── Dockerfile
│   └── nginx.conf
└── backend
    ├── app.jar
    └── Dockerfile
```

### 常用部署命令

前端更新：

```bash
cd /opt/relationship-agent
docker compose build --no-cache frontend
docker compose up -d frontend
docker exec -it relationship-frontend chmod -R a+rX /usr/share/nginx/html
```

后端更新：

```bash
cd /opt/relationship-agent
docker compose build --no-cache backend
docker compose up -d backend
docker compose logs -f backend
```

前后端全部更新：

```bash
cd /opt/relationship-agent
docker compose up -d --build
```

查看容器状态：

```bash
docker compose ps
```

查看后端日志：

```bash
docker compose logs -f backend
```

## Swagger 接口文档

本项目集成 Swagger/OpenAPI，用于自动生成后端接口文档。

本地访问：

```text
http://localhost:8080/swagger-ui/index.html
```

服务器访问：

```text
http://服务器IP:8081/swagger-ui/index.html
```

Swagger 的作用：

- 查看后端接口列表。
- 查看请求参数和返回结构。
- 在线测试接口。
- 方便前后端联调。
- 体现系统工程规范和可维护性。

## 数据存储说明

### MySQL

MySQL 用于保存核心业务数据：

- 用户账号
- 人物档案
- 人物记忆
- 聊天会话
- 聊天消息
- 知识库文档
- 意见反馈
- 操作日志
- 管理员相关数据

### Redis

Redis 用于：

- 验证码缓存
- AI 记忆相关缓存
- 知识库向量数据存储
- LangChain4j 向量检索

### 向量检索流程

```text
用户上传知识库
  ↓
后端切分文本
  ↓
调用 Embedding 模型生成向量
  ↓
向量写入 Redis
  ↓
用户提问
  ↓
问题生成向量
  ↓
Redis 相似度检索
  ↓
返回相关知识片段
  ↓
大模型结合知识片段生成回答
```

## 比赛适配说明

本项目面向“智能软件与智能体工程实践”主题，符合综合系统赛道对完整软件系统的要求。

### 业务场景完整

系统围绕个人关系管理构建，从用户登录、人物建档、记忆沉淀、知识库管理、AI 对话到后台管理形成完整业务流程。

### 智能技术深度参与业务

系统不是简单调用通用模型进行问答，而是将大模型、向量检索、图片分析和长期记忆机制嵌入关系管理流程。

### 具备工程体系

系统具备前后端分离、数据库持久化、Redis 向量存储、JWT 鉴权、管理员后台、操作日志、接口文档和 Docker 部署能力。

### 创新点

1. 多模态关系记忆采集：支持文本、图片、知识库等多源输入，将非结构化信息转化为关系记忆。
2. 长期记忆增强智能体：系统能够沉淀用户关系上下文，并在后续对话中持续调用。
3. 关系维护闭环：从信息采集、记忆保存、上下文检索到主动建议生成，形成完整智能体闭环。
4. 私域知识增强问答：结合知识库向量检索，提升回答与用户个人场景的相关性。
5. 工程化落地：具备可部署、可测试、可维护的完整软件系统形态。

## 演示建议

比赛展示时可以按照以下流程演示：

```text
1. 登录系统
2. 创建人物档案
3. 添加人物记忆
4. 上传知识库文件
5. 上传聊天截图进行图片分析
6. 发起 AI 对话，展示系统调用记忆和知识库回答
7. 查看关系维护提醒和 AI 建议
8. 进入管理员后台查看数据统计和操作日志
9. 打开 Swagger 展示接口文档
```

## 后续迭代方向

- 图片分析历史记录增强。
- 记忆确认机制进一步优化。
- 关系画像页可视化。
- 知识库处理进度展示。
- 管理员回复用户意见。
- 更完整的量化测试报告。
- 多模型切换与模型不可用降级策略。

## 注意事项

正式公开仓库前建议：

- 不要提交真实 API Key。
- 不要提交服务器密码。
- 不要提交数据库密码。
- 不要提交 `node_modules`、`dist`、`target` 等构建产物。
- 可以提供 `application-example.properties` 作为配置示例。

如果仓库为私有仓库，也建议保留以上习惯，方便后续维护和团队协作。
