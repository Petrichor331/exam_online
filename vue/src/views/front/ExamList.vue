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
          <!-- 开始考试/继续考试 -->
          <el-button 
            v-if="exam.status === 'pending' || exam.status === 'ongoing'" 
            type="primary" 
            size="large"
            @click="goToExam(exam)"
          >
            {{ exam.status === 'ongoing' && exam.scoreId ? '继续考试' : '开始考试' }}
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

    <!-- 考试确认弹窗 -->
    <el-dialog
      v-model="examConfirmVisible"
      :title="examConfirmTitle"
      width="580px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="exam-confirm-dialog"
    >
      <div class="exam-notice">
        <!-- 一、考试前准备 -->
        <div class="notice-section">
          <div class="notice-header">
            <div class="notice-icon prepare">
              <el-icon><Setting /></el-icon>
            </div>
            <h4>考试前准备</h4>
          </div>
          <ul class="notice-list">
            <li>确保设备电量充足，网络连接稳定</li>
            <li>建议使用Chrome、Firefox或Edge浏览器</li>
            <li>关闭其他无关程序，保持考试环境安静</li>
          </ul>
        </div>
        
        <!-- 二、考试规则 -->
        <div class="notice-section">
          <div class="notice-header">
            <div class="notice-icon rules">
              <el-icon><Warning /></el-icon>
            </div>
            <h4>考试规则</h4>
          </div>
          <ul class="notice-list warning">
            <li>考试期间禁止切换浏览器标签页或窗口</li>
            <li>禁止使用任何外部资源（如百度、课本等）</li>
            <li>考试倒计时结束将自动提交试卷</li>
            <li>考试中途离开页面考试倒计时仍会继续</li>
            <li>请诚信考试，系统会记录您的操作行为</li>
          </ul>
        </div>
        
        <!-- 三、重要提醒 -->
        <div class="notice-section">
          <div class="notice-header">
            <div class="notice-icon important">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <h4>重要提醒</h4>
          </div>
          <ul class="notice-list important">
            <li>任何违规行为都可能被记录，并可能影响您的考试成绩</li>
            <li>请认真审题，合理分配答题时间</li>
          </ul>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="examConfirmVisible = false">取消</el-button>
          <el-button type="primary" size="large" @click="confirmStartExam">
            <el-icon><Check /></el-icon>
            {{ isContinueExam ? '确认继续考试' : '我已仔细阅读并理解以上考试规则，确认开始考试' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { Search, Document, Star, User, Clock, Setting, Warning, InfoFilled, Check } from '@element-plus/icons-vue'

const router = useRouter()

// 数据
const loading = ref(false)
const searchKey = ref('')
const examList = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const examConfirmVisible = ref(false)
const examConfirmTitle = ref('')
const examConfirmPath = ref('')
const isContinueExam = ref(false)

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
  const isContinue = exam.status === 'ongoing' && exam.scoreId
  isContinueExam.value = isContinue
  examConfirmTitle.value = isContinue ? '继续考试确认' : '考试注意事项'
  examConfirmPath.value = `/front/exam/${exam.paperId}`
  examConfirmVisible.value = true
}

// 确认开始考试
const confirmStartExam = () => {
  examConfirmVisible.value = false
  router.push(examConfirmPath.value)
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

/* 考试确认弹窗样式 */
.exam-confirm-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.exam-confirm-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #333 0%, #555 100%);
  padding: 20px 24px;
  margin: 0;
}

.exam-confirm-dialog :deep(.el-dialog__title) {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}

.exam-confirm-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.exam-confirm-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0f0f0;
}

.exam-notice {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.notice-section {
  background: #fafafa;
  border-radius: 10px;
  padding: 16px 20px;
  border-left: 4px solid #333;
}

.notice-section:nth-child(2) {
  border-left-color: #e6a23c;
}

.notice-section:nth-child(3) {
  border-left-color: #f56c6c;
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.notice-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
}

.notice-icon.prepare {
  background: linear-gradient(135deg, #333 0%, #666 100%);
}

.notice-icon.rules {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
}

.notice-icon.important {
  background: linear-gradient(135deg, #f56c6c 0%, #fab6b6 100%);
}

.notice-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.notice-list {
  margin: 0;
  padding-left: 24px;
  list-style: none;
}

.notice-list li {
  position: relative;
  padding-left: 20px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.notice-list li:last-child {
  margin-bottom: 0;
}

.notice-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #909399;
  font-weight: bold;
}

.notice-list.warning li::before {
  color: #e6a23c;
}

.notice-list.important li::before {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  min-width: 100px;
}

.dialog-footer .el-button--primary {
  padding: 12px 24px;
  font-weight: 500;
}
</style>
