<template>
  <div class="practice-container">
    <div class="page-header">
      <h1>刷题中心</h1>
      <p>选择课程开始刷题</p>
    </div>

    <div class="course-grid">
      <el-card 
        v-for="course in courseList" 
        :key="course.id" 
        class="course-card"
        shadow="hover"
      >
        <div class="course-info">
          <h3>{{ course.name }}</h3>
          <p class="course-desc">{{ course.description || '点击开始刷题' }}</p>
        </div>
        <div class="course-action">
          <el-button class="btn-black" @click="enterPractice(course)">开始刷题</el-button>
          <el-button class="btn-wrong" @click="practiceWrong(course)">刷错题</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="courseList.length === 0" description="暂无可用课程"></el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { getCurrentUser } from '@/utils/userStorage.js'

const router = useRouter()
const courseList = ref([])
const user = getCurrentUser()

const loadCourses = async () => {
  try {
    const res = await request.get('/course/selectAll')
    if (res.code === '200') {
      courseList.value = res.data || []
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
    // 从localStorage读取错题ID列表
    const wrongKey = `wrong_${course.id}`
    console.log('读取错题，key:', wrongKey)
    const wrongIds = JSON.parse(localStorage.getItem(wrongKey) || '[]')
    console.log('读取到的错题ID:', wrongIds)
    
    if (!wrongIds || wrongIds.length === 0) {
      ElMessage.warning('暂无错题记录')
      return
    }
    
    // 转成逗号分隔的字符串
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
}

.course-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
  gap: 15px;
}

.course-info {
  flex: 1;
}

.course-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}

.course-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.btn-black {
  background: #333;
  border-color: #333;
  color: #fff;
}

.btn-black:hover {
  background: #666;
  border-color: #666;
  color: #fff;
}

.btn-wrong {
  background: #fff;
  border-color: #333;
  color: #333;
}

.btn-wrong:hover {
  background: #333;
  border-color: #333;
  color: #fff;
}
</style>
