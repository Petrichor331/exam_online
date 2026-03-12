<template>
  <div class="home-container">
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>欢迎回来，{{ userName }}！</h1>
        <p>今天是 {{ currentDate }}，准备好迎接新的挑战了吗？</p>
      </div>
      <div class="banner-illustration">
        <el-carousel :interval="4000" arrow="never" height="320px" indicator-position="none">
          <el-carousel-item>
            <img src="@/assets/img/在线考试系统登录背景1.png" alt="轮播图1">
          </el-carousel-item>
          <el-carousel-item>
            <img src="@/assets/img/在线考试系统登录背景2.png" alt="轮播图2">
          </el-carousel-item>
          <el-carousel-item>
            <img src="@/assets/img/登录背景1.png" alt="轮播图3">
          </el-carousel-item>
        </el-carousel>
      </div>
    </div>

    <!-- 进行中的考试 -->
    <div v-if="validOngoingExams.length > 0" class="section ongoing-section">
      <div class="section-header">
        <div class="section-title">
          <el-icon class="pulse-icon"><Timer /></el-icon>
          <span>进行中的考试</span>
          <el-tag type="danger" effect="dark">{{ validOngoingExams.length }}</el-tag>
        </div>
      </div>
      <div class="ongoing-cards">
        <div
            v-for="exam in validOngoingExams"
            :key="exam.id"
            class="ongoing-card"
            @click="continueExam(exam)"
        >
          <div class="ongoing-left">
            <div class="exam-name">{{ exam.name }}</div>
            <div class="exam-meta">
              <span><el-icon><Document /></el-icon> {{ exam.questionCount }}题</span>
              <span><el-icon><Star /></el-icon> {{ exam.totalScore }}分</span>
              <span><el-icon><User /></el-icon> {{ exam.teacherName }}</span>
            </div>
          </div>
          <div class="ongoing-right">
            <div class="countdown" :class="{ warning: exam.remainingMinutes <= 30 }">
              <div class="countdown-label">剩余时间</div>
              <div class="countdown-time">{{ formatTime(exam.remainingSeconds) }}</div>
            </div>
            <el-button type="primary" size="large" class="continue-btn">
              继续考试 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="section quick-access">
      <div class="quick-grid">
        <div class="quick-item" @click="goTo('/front/course')">
          <div class="quick-icon black">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="quick-text">课程中心</div>
        </div>
        <div class="quick-item" @click="goTo('/front/myCourse')">
          <div class="quick-icon black">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="quick-text">我的课程</div>
        </div>
        <div class="quick-item"  @click="goTo('/front/examList')">
          <div class="quick-icon black">
            <el-icon><Document /></el-icon>
          </div>
          <div class="quick-text">全部考试</div>
        </div>
        <div class="quick-item" @click="goTo('/front/practice')">
          <div class="quick-icon black">
            <el-icon><EditPen /></el-icon>
          </div>
          <div class="quick-text">模拟练习</div>
        </div>
      </div>
    </div>

    <!-- 主内容区：左右分栏 -->
    <div class="main-grid">
      <!-- 左侧： upcoming exams -->
      <div class="section upcoming-section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Calendar /></el-icon>
            <span>待考科目</span>
          </div>
          <el-button text @click="goTo('/front/examList')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="upcomingExams.length === 0" class="empty-state">
          <el-empty description="暂无待考科目" />
        </div>

        <div v-else class="exam-list">
          <div
              v-for="exam in upcomingExams.slice(0, 5)"
              :key="exam.id"
              class="exam-list-item"
          >
            <div class="exam-info">
              <div class="exam-title">{{ exam.name }}</div>
              <div class="exam-time">
                <el-icon><Clock /></el-icon>
                {{ exam.startTime }} 开始
              </div>
            </div>
            <div class="exam-action">
              <el-tag :type="getTimeTagType(exam.startTime)">
                {{ getTimeLabel(exam.startTime) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：最近成绩 -->
      <div class="section score-section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Trophy /></el-icon>
            <span>最近成绩</span>
          </div>
          <el-button text @click="goTo('/front/score')">
            历史记录 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="recentScores.length === 0" class="empty-state">
          <el-empty description="暂无考试记录" />
        </div>

        <div v-else class="score-list">
          <div
              v-for="score in recentScores.slice(0, 5)"
              :key="score.id"
              class="score-item"
          >
            <div class="score-left">
              <div class="score-course">{{ score.examName }}</div>
              <div class="score-date">{{ score.submitTime }}</div>
            </div>
            <div class="score-right">
              <div
                  class="score-num"
                  :class="score.score !== null ? getScoreClass(score.score, score.totalScore) : 'pending'"
              >
                {{ score.score !== null ? score.score : '待批改' }}
              </div>
              <div class="score-total" v-if="score.score !== null">/ {{ score.totalScore }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { getCurrentUser } from '@/utils/userStorage.js'
import { ElMessage } from 'element-plus'
import {
  Timer, Document, Star, User, ArrowRight,
  Collection, EditPen, CircleClose, TrendCharts,
  Calendar, Clock, Trophy, Lightning, Reading
} from '@element-plus/icons-vue'

const router = useRouter()

const validOngoingExams = computed(()=>{
  //这里的filter是只保留时间大于0的
  return ongoingExams.value.filter(exam=>exam.remainingSeconds > 0)
})

// 用户名
const userName = computed(() => {
  const user = getCurrentUser()
  return user.name || '同学'
})

// 当前日期
const currentDate = computed(() => {
  const date = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
})

// 数据
const ongoingExams = ref([])
const upcomingExams = ref([])
const recentScores = ref([])
const loading = ref(false)

// 加载首页数据
const loadHomeData = async () => {
  loading.value = true
  try {
    const res = await request.get('/exam/home')
    if (res.code === '200') {
      const data = res.data
      
      // 进行中的考试 - 添加倒计时
      ongoingExams.value = (data.ongoingExams || []).map(exam => ({
        ...exam,
        remainingSeconds: exam.remainingSeconds || 0
      }))
      
      // 待考科目
      upcomingExams.value = data.upcomingExams || []
      
      // 最近成绩
      recentScores.value = data.recentScores || []
      
      // 启动倒计时
      if (ongoingExams.value.length > 0 && !countdownTimer) {
        countdownTimer = setInterval(updateCountdowns, 1000)
      }
    } else {
      ElMessage.error(res.msg || '加载数据失败')
    }
  } catch (error) {
    console.error('加载首页数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化倒计时
const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
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

// 跳转路由
const goTo = (path) => {
  router.push(path)
}

// 继续考试
const continueExam = (exam) => {
  router.push(`/front/exam/${exam.id}`)
}

// 倒计时定时器
let countdownTimer = null

// 更新倒计时
const updateCountdowns = () => {
  ongoingExams.value.forEach(exam => {
    if (exam.remainingSeconds > 0) {
      exam.remainingSeconds--
    }
  })
}

onMounted(() => {
  loadHomeData()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 120px);
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(140deg, #0e0d0d 0%, #555 100%);
  border-radius: 8px;
  padding: 32px 40px;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  height: 300px;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 500px;
  height: 500px;
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
}

.banner-content {
  position: relative;
  z-index: 1;
  padding-left: 50px;
}

.banner-content h1 {
  font-size: 32px;
  margin-bottom: 8px;
  font-weight: 600;
}

.banner-content p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 24px;
}

.banner-illustration {
  width: 650px;

}

.banner-illustration img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 15px;
}

/* 区块通用样式 */
.section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #e0e0e0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-title .el-icon {
  font-size: 20px;
  color: #333;
}

.pulse-icon {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 进行中考试 */
.ongoing-section {
  background: #fff;
  border: 1px solid #e4e7ed;
}

.ongoing-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ongoing-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  border-left: 4px solid #333;
  cursor: pointer;
  transition: all 0.2s;
}

.ongoing-card:hover {
  background: #f5f5f5;
}

.exam-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.exam-meta {
  display: flex;
  gap: 20px;
  color: #606266;
  font-size: 14px;
}

.exam-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ongoing-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.countdown {
  text-align: center;
}

.countdown-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.countdown-time {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

.countdown.warning .countdown-time {
  color: #e6a23c;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.continue-btn {
  padding: 12px 24px;
  font-size: 16px;
}

/* 快捷入口 */
.quick-access {
  padding: 20px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f0f2f5;
}

.quick-item:hover {
  background: #e8eaed;
}

.quick-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  color: white;
  font-size: 28px;
}

.quick-icon.black { background: #333; }
.quick-icon.green { background: #444; }
.quick-icon.orange { background: #555; }
.quick-icon.purple { background: #666; }

.quick-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

/* 主内容网格 */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* 考试列表 */
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  transition: all 0.2s;
}

.exam-list-item:hover {
  background: #f0f2f5;
}

.exam-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
}

.exam-time {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
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
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.score-item:last-child {
  border-bottom: none;
}

.score-course {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.score-date {
  font-size: 13px;
  color: #909399;
}

.score-right {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-num {
  font-size: 24px;
  font-weight: bold;
}

.score-num.excellent { color: #333; }
.score-num.pass { color: #555; }
.score-num.fail { color: #777; }
.score-num.pending { color: #909399; font-size: 14px; }

.score-total {
  font-size: 14px;
  color: #909399;
}

/* 空状态 */
.empty-state {
  padding: 40px 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    text-align: center;
  }

  .banner-illustration {
    display: none;
  }

  .ongoing-card {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .ongoing-right {
    flex-direction: column;
    width: 100%;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>