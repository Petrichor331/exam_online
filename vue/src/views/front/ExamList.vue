<template>
  <div class="exam-list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>考试列表</h1>
      <p>查看所有考试，记录成绩</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKey"
        placeholder="搜索考试名称..."
        clearable
        :prefix-icon="Search"
        @keyup.enter="loadExamList"
      />
      <el-button type="primary" @click="loadExamList">
        <el-icon><Search /></el-icon>搜索
      </el-button>
    </div>

    <!-- 考试列表 -->
    <div class="exam-grid" v-loading="loading">
      <div v-if="examList.length === 0" class="empty-state">
        <el-empty description="暂无考试数据" />
      </div>

      <div
        v-for="exam in examList"
        :key="exam.id"
        class="exam-card"
        :class="getCardClass(exam.status)"
      >
        <!-- 卡片头部 -->
        <div class="exam-card-header">
          <h3>{{ exam.paperName }}</h3>
          <el-tag :type="getTagType(exam.status)" size="small">
            {{ getStatusText(exam.status) }}
          </el-tag>
        </div>

        <!-- 卡片内容 -->
        <div class="exam-card-body">
          <div class="exam-info">
            <span><el-icon><Document /></el-icon> {{ exam.questionCount }}题</span>
            <span><el-icon><Star /></el-icon> {{ exam.totalScore }}分</span>
            <span><el-icon><User /></el-icon> {{ exam.teacherName }}</span>
          </div>
          <div class="exam-time" v-if="exam.startTime">
            <el-icon><Clock /></el-icon>
            {{ exam.startTime }} - {{ exam.endTime }}
          </div>
          <!-- 显示分数 -->
          <div class="exam-score" v-if="exam.status === 'finished'">
            <span class="score-label">考试成绩：</span>
            <span class="score-value">{{ exam.examScore }}分</span>
            <span class="score-total">/ {{ exam.totalScore }}分</span>
          </div>
          <!-- 待评分提示 -->
          <div class="exam-waiting" v-if="exam.status === 'waiting'">
            <el-icon><Clock /></el-icon> 老师正在评阅中，请耐心等待...
          </div>
        </div>

        <!-- 卡片底部按钮 -->
        <div class="exam-card-footer">
          <!-- 未考/进行中 -->
          <el-button 
            v-if="exam.status === 'pending' || exam.status === 'ongoing'" 
            type="primary" 
            size="large"
            @click="goToExam(exam)"
          >
            {{ exam.status === 'ongoing' ? '继续考试' : '开始考试' }}
          </el-button>
          <!-- 待评分 -->
          <el-button 
            v-else-if="exam.status === 'waiting'" 
            disabled 
            size="large"
          >
            等待评阅
          </el-button>
          <!-- 已完成 - 查看答卷 -->
          <el-button 
            v-else-if="exam.status === 'finished'" 
            size="large"
            @click="viewAnswer(exam)"
          >
            查看答卷
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 12, 16]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadExamList"
        @current-change="loadExamList"
      />
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { Search, Document, Star, User, Clock } from '@element-plus/icons-vue'

const router = useRouter()

// 数据
const loading = ref(false)
const searchKey = ref('')
const examList = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 状态映射
const getStatusText = (status) => {
  const map = {
    'pending': '待考试',
    'ongoing': '进行中',
    'grading': '待评分',
    'finished': '已完成'
  }
  return map[status] || '未知'
}

const getTagType = (status) => {
  const map = {
    'pending': 'info',
    'ongoing': 'danger',
    'grading': 'warning',
    'finished': 'success'
  }
  return map[status] || 'info'
}

const getCardClass = (status) => {
  return `card-${status}`
}

// 加载考试列表
const loadExamList = async () => {
  loading.value = true
  try {
    const res = await request.get('/exam/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        name: searchKey.value
      }
    })
    
    if (res.code === '200') {
      examList.value = res.data?.list || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 去考试
const goToExam = (exam) => {
  router.push(`/front/exam/${exam.paperId}`)
}

// 查看答卷
const viewAnswer = (exam) => {
  router.push(`/front/answer/${exam.scoreId}`)
}

onMounted(() => {
  loadExamList()
})
</script>

<style scoped>
.exam-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-bar .el-input {
  flex: 1;
  max-width: 400px;
}

/* 考试卡片网格 */
.exam-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 考试卡片 - 不同状态不同颜色 */
.exam-card {
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s;
}

/* 待考试 - 灰色 */
.exam-card.card-pending {
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
}
.exam-card.card-pending:hover {
  border-color: #999;
}

/* 进行中 - 浅红色 */
.exam-card.card-ongoing {
  background: #fff5f5;
  border: 1px solid #ffcccc;
}
.exam-card.card-ongoing:hover {
  border-color: #e01b24;
}

/* 待评分 - 浅黄色 */
.exam-card.card-waiting {
  background: #fffbf0;
  border: 1px solid #ffe7ba;
}
.exam-card.card-waiting:hover {
  border-color: #ff7800;
}

/* 已完成 - 浅绿色 */
.exam-card.card-finished {
  background: #f0f9f0;
  border: 1px solid #ccf2cc;
}
.exam-card.card-finished:hover {
  border-color: #33d17a;
}

/* 卡片头部 */
.exam-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.exam-card-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

/* 卡片内容 */
.exam-card-body {
  margin-bottom: 16px;
}

.exam-info {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.exam-info span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.exam-time {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 13px;
}

.exam-score {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
}

.score-label {
  color: #666;
  font-size: 14px;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.score-total {
  color: #999;
  font-size: 14px;
}

.exam-waiting {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #996600;
  font-size: 14px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
}

/* 卡片底部 */
.exam-card-footer {
  text-align: center;
}

.exam-card-footer .el-button {
  width: 100%;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .exam-grid {
    grid-template-columns: 1fr;
  }
}
</style>
