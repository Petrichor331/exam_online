<template>
  <div class="my-course-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>我的课程</h1>
      <p>管理您已选的课程，查看相关考试</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-value">{{ myCourses.length }}</div>
        <div class="stat-label">已选课程</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value">{{ ongoingExams.length }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value">{{ upcomingExams.length }}</div>
        <div class="stat-label">待考试</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value">{{ finishedExams.length }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <!-- 进行中的考试 -->
    <div v-if="ongoingExams.length > 0" class="section">
      <div class="section-header">
        <div class="section-title">
          <el-icon class="pulse-icon"><Timer /></el-icon>
          <span>进行中的考试</span>
          <el-tag type="danger">{{ ongoingExams.length }}</el-tag>
        </div>
      </div>
      <div class="exam-list urgent">
        <div
          v-for="exam in ongoingExams"
          :key="exam.id"
          class="exam-card urgent-card"
          @click="continueExam(exam)"
        >
          <div class="exam-info">
            <h4>{{ exam.name }}</h4>
            <p>剩余时间: {{ formatTime(exam.remainingSeconds) }}</p>
          </div>
          <el-button type="danger">继续考试</el-button>
        </div>
      </div>
    </div>

    <!-- 待考科目 -->
    <div v-if="upcomingExams.length > 0" class="section">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Calendar /></el-icon>
          <span>待考科目</span>
          <el-tag type="warning">{{ upcomingExams.length }}</el-tag>
        </div>
      </div>
      <div class="exam-list">
        <div
          v-for="exam in upcomingExams"
          :key="exam.id"
          class="exam-card"
        >
          <div class="exam-info">
            <h4>{{ exam.name }}</h4>
            <p>
              <el-icon><Clock /></el-icon>
              开始时间: {{ exam.startTime }}
            </p>
          </div>
          <el-button 
            v-if="isExamStart(exam.startTime)" 
            type="primary" 
            @click="startExam(exam.id)"
          >
            开始考试
          </el-button>
          <el-tag v-else :type="getTimeTagType(exam.startTime)">
            {{ getTimeLabel(exam.startTime) }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 已选课程列表 -->
    <div class="section">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Collection /></el-icon>
          <span>课程列表</span>
        </div>
        <el-button type="primary" text @click="goToCourseCenter">
          去选课 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div v-if="myCourses.length === 0" class="empty-state">
        <el-empty description="您还没有选课">
          <el-button type="primary" @click="goToCourseCenter">去选课</el-button>
        </el-empty>
      </div>

      <div v-else class="course-list" v-loading="loading">
        <div
          v-for="course in myCourses"
          :key="course.id"
          class="course-item"
        >
          <div class="course-main">
            <div class="course-icon">
              <img v-if="course.courseImg" :src="course.courseImg" :alt="course.courseName" class="icon-img" />
              <div v-else class="icon-fallback" :style="{ background: getGradient(course.courseId) }">
                {{ course.courseName?.charAt(0) || '课' }}
              </div>
            </div>
            <div class="course-detail">
              <h4>{{ course.courseName }}</h4>
              <p>授课教师: {{ course.teacherName || '待定' }}</p>
              <p class="course-time">选课时间: {{ formatDate(course.createTime) }}</p>
            </div>
          </div>
          <div class="course-actions">
            <el-button 
              type="danger" 
              link
              @click="dropCourse(course)"
              :loading="course.loading"
            >
              <el-icon><Delete /></el-icon>退课
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近成绩 -->
    <div v-if="recentScores.length > 0" class="section">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Trophy /></el-icon>
          <span>最近成绩</span>
        </div>
      </div>
      <div class="score-list">
        <div
          v-for="score in recentScores.slice(0, 5)"
          :key="score.id"
          class="score-item"
        >
          <div class="score-info">
            <span class="exam-name">{{ score.examName }}</span>
            <span class="exam-time">{{ score.submitTime }}</span>
          </div>
          <div class="score-value" :class="getScoreClass(score.score, score.totalScore)">
            {{ score.score }}<span class="total">/{{ score.totalScore }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.js'
import { Timer, Calendar, Clock, Collection, ArrowRight, Trophy, Delete } from '@element-plus/icons-vue'

const router = useRouter()

// 数据
const loading = ref(false)
const myCourses = ref([])
const ongoingExams = ref([])
const upcomingExams = ref([])
const finishedExams = ref([])
const recentScores = ref([])

// 渐变色
const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
]

const getGradient = (id) => {
  return gradients[(id || 0) % gradients.length]
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds || seconds <= 0) return '已结束'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')}`
}

// 获取时间标签类型
const getTimeTagType = (startTime) => {
  const diff = new Date(startTime) - new Date()
  const hours = diff / (1000 * 60 * 60)
  if (hours <= 0) return 'success'  // 已经开始
  if (hours < 24) return 'danger'
  if (hours < 72) return 'warning'
  return 'info'
}

// 获取时间标签文本
const getTimeLabel = (startTime) => {
  const diff = new Date(startTime) - new Date()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)
  
  if (hours <= 0) return '进行中'  // 已经开始
  if (hours < 1) return '即将开始'
  if (hours < 24) return `${hours}小时后`
  return `${days}天后`
}

// 获取分数样式
const getScoreClass = (score, total) => {
  const rate = score / total
  if (rate >= 0.9) return 'excellent'
  if (rate >= 0.6) return 'pass'
  return 'fail'
}

// 判断考试是否已开始
const isExamStart = (startTime) => {
  const diff = new Date(startTime) - new Date()
  return diff <= 0
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 加载首页数据（包含进行中的考试、待考科目、最近成绩）
    const homeRes = await request.get('/exam/home')
    if (homeRes.code === '200') {
      const data = homeRes.data
      ongoingExams.value = data.ongoingExams || []
      upcomingExams.value = data.upcomingExams || []
      recentScores.value = data.recentScores || []
      
      // 启动倒计时
      if (ongoingExams.value.length > 0) {
        startCountdown()
      }
    }
    
    // 加载已选课程
    const courseRes = await request.get('/studentCourse/my-courses')
    if (courseRes.code === '200') {
      myCourses.value = (courseRes.data || []).map(c => ({...c, loading: false}))
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 倒计时
let countdownTimer = null
const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    ongoingExams.value.forEach(exam => {
      if (exam.remainingSeconds > 0) {
        exam.remainingSeconds--
      }
    })
  }, 1000)
}

// 退课
const dropCourse = async (course) => {
  try {
    await ElMessageBox.confirm(
      `确定要退选【${course.courseName}】吗？退选后将无法参加该课程的考试。`,
      '确认退课',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    course.loading = true
    const res = await request.post(`/studentCourse/drop/${course.courseId}`)
    if (res.code === '200') {
      ElMessage.success('退课成功')
      myCourses.value = myCourses.value.filter(c => c.id !== course.id)
    } else {
      ElMessage.error(res.msg || '退课失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退课失败')
    }
  } finally {
    course.loading = false
  }
}

// 继续考试
const continueExam = (exam) => {
  router.push(`/front/exam/${exam.paperId}`)
}

// 开始考试
const startExam = (paperId) => {
  router.push(`/front/exam/${paperId}`)
}

// 去选课中心
const goToCourseCenter = () => {
  router.push('/front/course')
}

onMounted(() => {
  loadData()
})

// onUnmounted(() => {
//   if (countdownTimer) {
//     clearInterval(countdownTimer)
//   }
// })
</script>

<style scoped>
.my-course-container {
  max-width: 1000px;
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
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  color: #909399;
}

/* 统计行 */
.stats-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 40px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 4px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #e4e7ed;
}

/* 区块 */
.section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pulse-icon {
  animation: pulse 2s infinite;
  color: #f56c6c;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 考试列表 */
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.exam-card:hover {
  background: #e4e7ed;
}

.exam-card.urgent-card {
  background: linear-gradient(to right, #fff5f5, #ffffff);
  border-left: 4px solid #f56c6c;
  cursor: pointer;
}

.exam-info h4 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 4px;
}

.exam-info p {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 课程列表 */
.course-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
}

.course-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64,158,255,0.1);
}

.course-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.course-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.icon-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: white;
}

.course-detail h4 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 4px;
}

.course-detail p {
  font-size: 13px;
  color: #606266;
}

.course-time {
  color: #909399 !important;
}

/* 成绩列表 */
.score-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.score-item:last-child {
  border-bottom: none;
}

.score-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.exam-name {
  font-weight: 500;
  color: #303133;
}

.exam-time {
  font-size: 13px;
  color: #909399;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
}

.score-value .total {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

.score-value.excellent { color: #67c23a; }
.score-value.pass { color: #e6a23c; }
.score-value.fail { color: #f56c6c; }

/* 空状态 */
.empty-state {
  padding: 40px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    flex-wrap: wrap;
    gap: 20px;
  }
  
  .stat-divider {
    display: none;
  }
  
  .course-main {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .exam-card {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>