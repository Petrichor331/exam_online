<template>
  <div class="practice-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1>刷题中心</h1>
          <p>选择课程开始刷题，提升学习成绩</p>
        </div>
        <div class="header-decoration">
          <div class="decoration-circle"></div>
          <div class="decoration-circle"></div>
          <div class="decoration-circle"></div>
        </div>
      </div>
    </div>

    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-icon">📚</span>
        <div class="stat-info">
          <span class="stat-value">{{ courseList.length }}</span>
          <span class="stat-label">可选课程</span>
        </div>
      </div>
    </div>

    <div class="course-grid">
      <el-card 
        v-for="course in courseList" 
        :key="course.id" 
        class="course-card"
        shadow="hover"
      >
        <div class="course-cover">
          <img v-if="course.img" :src="course.img" :alt="course.name" class="cover-img" />
          <div v-else class="cover-placeholder" :style="{ background: getCourseColor(course.id) }">
            <span>{{ course.name?.charAt(0) || '课' }}</span>
          </div>
          <div class="course-tag" v-if="course.credit">{{ course.credit }}学分</div>
        </div>
        
        <div class="course-body">
          <h3 class="course-name">{{ course.name }}</h3>
          <p class="course-desc">{{ course.description || '点击开始刷题' }}</p>
          
          <div class="course-meta" v-if="course.teacherName">
            <el-icon><User /></el-icon>
            <span>{{ course.teacherName }}</span>
          </div>
          
          <div class="course-actions">
            <el-button type="primary" class="btn-start" @click="enterPractice(course)">
              <el-icon><Edit /></el-icon>
              开始刷题
            </el-button>
            <el-button class="btn-wrong" @click="practiceWrong(course)">
              <el-icon><Warning /></el-icon>
              错题本
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-if="courseList.length === 0" description="暂无可用课程">
      <el-button type="primary" @click="$router.push('/front/course')">去选课</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Warning, User } from '@element-plus/icons-vue'
import request from '@/utils/request.js'

const router = useRouter()
const courseList = ref([])
const totalQuestions = ref(0)

const colorPalette = [
  'linear-gradient(135deg, #2c3e50 0%, #34495e 100%)',
  'linear-gradient(135deg, #434343 0%, #000000 100%)',
  'linear-gradient(135deg, #304352 0%, #d7d2cc 100%)',
  'linear-gradient(135deg, #232526 0%, #414345 100%)',
  'linear-gradient(135deg, #373737 0%, #222222 100%)',
  'linear-gradient(135deg, #525252 0%, #3c4e62 100%)',
  'linear-gradient(135deg, #2b2b2b 0%, #1a1a1a 100%)',
  'linear-gradient(135deg, #3a3a3a 0%, #1c1c1c 100%)',
]

const getCourseColor = (id) => {
  return colorPalette[id % colorPalette.length]
}

const loadCourses = async () => {
  try {
    const res = await request.get('/course/selectAll')
    if (res.code === '200') {
      courseList.value = res.data || []
      totalQuestions.value = courseList.value.length * Math.floor(Math.random() * 50 + 50)
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const enterPractice = (course) => {
  router.push(`/front/practice-exam/${course.id}`)
}

const practiceWrong = async (course) => {
  try {
    const wrongKey = `wrong_${course.id}`
    const wrongIds = JSON.parse(localStorage.getItem(wrongKey) || '[]')
    
    if (!wrongIds || wrongIds.length === 0) {
      ElMessage.warning('暂无错题记录')
      return
    }
    
    const questionIdsStr = wrongIds.join(',')
    
    const res = await request.get('/question/wrong-questions', {
      params: { questionIds: questionIdsStr }
    })
    if (res.code === '200') {
      if (!res.data || res.data.length === 0) {
        ElMessage.warning('暂无错题记录')
        return
      }
      router.push(`/front/practice-exam/${course.id}?wrong=1`)
    }
  } catch (error) {
    console.error('获取错题失败:', error)
    ElMessage.error('获取错题失败')
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.practice-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 120px);
}

.page-header {
  margin-bottom: 32px;
}

.header-content {
  position: relative;
  padding: 32px 40px;
  background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%);
  border-radius: 16px;
  overflow: hidden;
}

.header-text {
  position: relative;
  z-index: 1;
}

.header-text h1 {
  font-size: 32px;
  color: #fff;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.header-text p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.header-decoration {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 12px;
}

.decoration-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.decoration-circle:nth-child(2) {
  width: 80px;
  height: 80px;
}

.stats-bar {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  flex: 1;
}

.stat-icon {
  font-size: 24px;
  opacity: 0.8;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.course-card {
  border: none;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.course-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12) !important;
}

.course-card :deep(.el-card__body) {
  padding: 0;
}

.course-cover {
  position: relative;
  height: 140px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-placeholder span {
  font-size: 48px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.6);
}

.course-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  font-size: 12px;
  color: #fff;
}

.course-body {
  padding: 20px;
}

.course-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 12px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 42px;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

.course-actions {
  display: flex;
  gap: 12px;
}

.btn-start {
  flex: 1;
  height: 40px;
  background: #1a1a1a;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  color: #fff;
}

.btn-start:hover {
  background: #333;
}

.btn-wrong {
  flex: 1;
  height: 40px;
  background: #fff;
  border: 1px solid #1a1a1a;
  border-radius: 8px;
  color: #1a1a1a;
  font-weight: 500;
}

.btn-wrong:hover {
  background: #1a1a1a;
  color: #fff;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 768px) {
  .header-content {
    padding: 24px;
  }
  
  .header-text h1 {
    font-size: 24px;
  }
  
  .header-decoration {
    display: none;
  }
  
  .stats-bar {
    flex-direction: column;
  }
  
  .course-grid {
    grid-template-columns: 1fr;
  }
}
</style>