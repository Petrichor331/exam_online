<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>欢迎回来，{{ data.user?.name }}！</h1>
        <p>今天是 {{ currentDate }}，祝您教学愉快！</p>
      </div>
      <div class="banner-illustration">
        <el-carousel :interval="5000" arrow="never" height="450px" indicator-position="none">
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

    <!-- 快捷入口 -->
    <div class="section quick-access">
      <div class="quick-grid">
        <div class="quick-item" @click="goTo('/teacher/grading')">
          <div class="quick-icon black">
            <el-icon><Edit /></el-icon>
          </div>
          <div class="quick-text">试卷批阅</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/course')">
          <div class="quick-icon black">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="quick-text">课程管理</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/testPaper')">
          <div class="quick-icon black">
            <el-icon><Document /></el-icon>
          </div>
          <div class="quick-text">试卷管理</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/question')">
          <div class="quick-icon black">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="quick-text">题库管理</div>
        </div>
      </div>
    </div>

    <!-- 主内容区：左右分栏 -->
    <div class="main-grid">
      <!-- 左侧：待批改试卷 -->
      <div class="section upcoming-section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Tickets /></el-icon>
            <span>待批改试卷</span>
          </div>
          <el-button text @click="goTo('/teacher/grading')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="courseList.length === 0" class="empty-state">
          <el-empty description="暂无待批改试卷" />
        </div>

        <div v-else class="exam-list">
          <div
              v-for="course in courseList"
              :key="course.courseId"
              class="exam-list-item"
              @click="goToGrading(course)"
          >
            <div class="exam-info">
              <div class="exam-title">{{ course.courseName }}</div>
              <div class="exam-time">
                <el-icon><Document /></el-icon>
                {{ course.examCount || 0 }} 份待批改
              </div>
            </div>
            <div class="exam-action">
              <el-tag type="warning" v-if="course.examCount > 0">
                {{ course.examCount }} 份
              </el-tag>
              <el-tag type="info" v-else>
                已完成
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：考试安排 -->
      <div class="section score-section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Calendar /></el-icon>
            <span>考试安排</span>
          </div>
          <el-button text @click="goTo('/teacher/examPlan')">
            管理考试 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="examPlanList.length === 0" class="empty-state">
          <el-empty description="暂无考试安排" />
        </div>

        <div v-else class="timeline">
          <div
            v-for="(exam, index) in examPlanList"
            :key="exam.id"
            class="timeline-item"
          >
            <div class="timeline-dot" :class="{ active: index === 0 }"></div>
            <div class="timeline-content">
              <div class="timeline-title">{{ exam.title }}</div>
              <div class="timeline-content">
                <el-icon><Clock /></el-icon>
                {{exam.content}}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request.js'
import { getCurrentUser } from '@/utils/userStorage.js'
import { ElMessage } from 'element-plus'
import {
  Edit, Reading, Document, Collection, Calendar, ArrowRight,
  Tickets, Clock
} from '@element-plus/icons-vue'

const router = useRouter()

const data = reactive({
  user: getCurrentUser()
})

const currentDate = computed(() => {
  const date = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
})

const courseList = ref([])
const examPlanList = ref([])

const goTo = (path) => {
  router.push(path)
}

const goToGrading = (course) => {
  router.push({
    path: '/teacher/grading',
    query: { courseId: course.courseId }
  })
}

const goToExamPlan = () => {
  router.push('/manager/examPlan')
}

const getExamStatus = (startTime, endTime) => {
  const now = new Date()
  const start = new Date(startTime)
  const end = new Date(endTime)
  
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '进行中'
}

const getExamStatusType = (startTime, endTime) => {
  const now = new Date()
  const start = new Date(startTime)
  const end = new Date(endTime)
  
  if (now < start) return 'info'
  if (now > end) return 'success'
  return 'warning'
}

// 加载课程列表（待批改试卷）
const loadCourseList = () => {
  request.get('/score/courses').then(res => {
    if (res.code === '200') {
      courseList.value = res.data || []
    }
  })
}

// 加载考试安排
const loadExamPlanList = () => {
  request.get('/examPlan/selectPage', {
    params: {
      pageNum: 1,
      pageSize: 5
    }
  }).then(res => {
    if (res.code === '200') {
      examPlanList.value = res.data?.list || []
    }
  })
}

onMounted(() => {
  loadCourseList()
  loadExamPlanList()
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  background: linear-gradient(135deg, #2b2828 0%, #2c2a2a 100%);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.banner-content {
  position: relative;
  z-index: 1;
  padding-left: 100px;
  padding-top: 100px;
  padding-right: 100px;
}

.banner-content h1 {
  font-size: 32px;
  margin-bottom: 30px;
  font-weight: 600;
  color: #fff;
}

.banner-content p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 30px;
  color: #ccc;
}

.banner-illustration {
  width: 1000px;
}

.banner-illustration img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  cursor: pointer;
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

/* 时间轴 */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 16px;
  padding-bottom: 24px;
  cursor: pointer;
  position: relative;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: #e4e7ed;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #e4e7ed;
  flex-shrink: 0;
  margin-top: 4px;
}

.timeline-dot.active {
  background: #409eff;
}

.timeline-content {
  flex: 1;
}

.timeline-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.timeline-time {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-info {
  font-size: 12px;
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

  .banner-content {
    padding-left: 0;
    padding: 40px 20px;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
