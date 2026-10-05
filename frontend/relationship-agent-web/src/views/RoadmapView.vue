<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">迭代计划</h2>
        <p class="page-desc">集中展示后续可以继续开发的功能，方便你按阶段完善项目。</p>
      </div>
      <span class="tag">Roadmap</span>
    </div>

    <section class="summary-panel panel">
      <div>
        <span class="summary-label">当前状态</span>
        <strong>阶段性成品已完成</strong>
        <p>登录权限、人物记忆、知识库 RAG、聊天历史、图片分析、意见箱、管理员后台、Swagger 和部署能力已经形成完整闭环。</p>
      </div>
      <div class="summary-count">
        <strong>{{ unfinishedCount }}</strong>
        <span>个后续迭代点</span>
      </div>
    </section>

    <section class="roadmap-grid">
      <article
        v-for="item in roadmapItems"
        :key="item.title"
        class="roadmap-card"
      >
        <div class="card-head">
          <span class="status-tag" :class="item.level">{{ item.status }}</span>
          <span class="priority">{{ item.priority }}</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
        <ul>
          <li v-for="point in item.points" :key="point">{{ point }}</li>
        </ul>
      </article>
    </section>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'

export default {
  name: 'RoadmapView',
  components: {
    AppLayout
  },
  data() {
    return {
      roadmapItems: [
        {
          title: '战狼公司意见收集箱',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '用户可以提交意见，系统会进行分类，并支持查看自己已经发送过的意见。',
          points: ['提交意见', '自动/手动分类', '查看我的历史意见', '管理员查看全部意见']
        },
        {
          title: '管理员后台',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '管理员可以查看全站数据、管理用户角色、处理意见，并查看全站操作日志。',
          points: ['全站统计', '用户管理', '角色切换', '意见状态处理', '日志分页搜索']
        },
        {
          title: '图片分析体验',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '图片分析页面已支持拖拽上传、预览、文件信息展示、快捷提示词模板和复制结果。',
          points: ['拖拽上传', '快捷分析模板', '图片大小校验', '结果复制']
        },
        {
          title: '知识库上传安全',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '知识库上传已经支持更严格的文件校验，避免异常文件影响服务稳定性。',
          points: ['70MB 限制', 'PDF 真实格式校验', 'TXT UTF-8 校验', 'PDF 页数限制', '中文异常提示']
        },
        {
          title: 'Swagger 接口文档',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '后端接口已经接入 Swagger / OpenAPI，可用于接口测试、联调和项目展示。',
          points: ['接口可视化', 'JWT 授权测试', '接口分组展示']
        },
        {
          title: 'Docker 与服务器部署',
          status: '已完成',
          level: 'done',
          priority: '已上线',
          description: '项目已经具备前端、后端、MySQL、Redis 的部署基础，并支持服务器和域名访问。',
          points: ['前端构建', '后端打包', 'Docker Compose', '服务器部署', 'HTTPS 域名']
        },
        {
          title: '意见箱回复功能',
          status: '待开发',
          level: 'todo',
          priority: '优先级 P1',
          description: '当前可以提交和处理意见，后续可以让管理员直接回复用户，形成完整反馈闭环。',
          points: ['管理员回复内容', '用户查看回复', '回复时间记录', '站内通知']
        },
        {
          title: '人物画像页',
          status: '待开发',
          level: 'todo',
          priority: '优先级 P0',
          description: '把人物基础信息、人物记忆和 AI 总结集中展示，形成真正的关系画像。',
          points: ['基础资料展示', '按类型分组记忆', 'AI 生成画像摘要']
        },
        {
          title: 'AI 自动整理记忆',
          status: '待开发',
          level: 'todo',
          priority: '优先级 P0',
          description: '聊天中识别用户提到的新信息，自动分类并写入人物记忆。',
          points: ['识别人物姓名', '判断记忆类型', '调用 Tool 保存']
        },
        {
          title: '记忆确认机制',
          status: '待开发',
          level: 'todo',
          priority: '优先级 P1',
          description: 'AI 发现新记忆后先让用户确认，避免误保存、乱保存。',
          points: ['待确认记忆列表', '确认保存', '忽略错误记忆']
        },
        {
          title: '图片分析历史',
          status: '待开发',
          level: 'todo',
          priority: '优先级 P2',
          description: '把每次图片分析的结果保存下来，用户可以查看历史分析记录。',
          points: ['保存分析记录', '按用户隔离', '历史结果搜索', '再次复制结果']
        },
        {
          title: '知识库处理进度',
          status: '可优化',
          level: 'optimize',
          priority: '优先级 P2',
          description: '大文件上传和向量化时展示处理状态，让用户知道系统正在分段、向量化和入库。',
          points: ['上传进度', '切片数量展示', '向量化状态', '失败重试']
        },
        {
          title: 'AOP 操作日志增强',
          status: '可优化',
          level: 'optimize',
          priority: '优先级 P2',
          description: '把更多后台操作统一接入注解式日志，减少 Controller 里的手动记录代码。',
          points: ['@OperationLog 注解', '切面统一保存日志', '记录请求结果']
        },
        {
          title: '生产环境安全加固',
          status: '可优化',
          level: 'optimize',
          priority: '优先级 P1',
          description: '项目上线给朋友使用前，继续加强密钥、接口、日志和数据库备份。',
          points: ['API Key 环境变量', '关闭生产请求日志', '数据库定时备份', 'Swagger 访问限制']
        }
      ]
    }
  },
  computed: {
    unfinishedCount() {
      return this.roadmapItems.filter(item => item.level !== 'done').length
    }
  }
}
</script>

<style scoped>
.summary-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.summary-label {
  color: #6b7280;
  font-size: 14px;
}

.summary-panel strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
}

.summary-panel p {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.summary-count {
  min-width: 132px;
  padding: 16px;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  text-align: center;
  background: #fff1f2;
}

.summary-count strong {
  margin: 0;
  color: #b91c1c;
  font-size: 34px;
}

.summary-count span {
  color: #6b7280;
}

.roadmap-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.roadmap-card {
  padding: 18px;
  transition: transform 0.18s ease, background 0.18s ease;
}

.roadmap-card:hover {
  transform: translateY(-2px);
  background: #ffffff;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-tag.todo {
  color: #b91c1c;
  background: #fff1f2;
}

.status-tag.optimize {
  color: #047857;
  background: #d1fae5;
}

.status-tag.done {
  color: #ffffff;
  background: #111827;
}

.priority {
  color: #6b7280;
  font-size: 13px;
}

.roadmap-card h3 {
  margin: 0;
  font-size: 18px;
}

.roadmap-card p {
  margin: 10px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.roadmap-card ul {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #6b7280;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .roadmap-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-panel {
    display: block;
  }

  .summary-count {
    margin-top: 16px;
  }

  .roadmap-grid {
    grid-template-columns: 1fr;
  }
}
</style>
