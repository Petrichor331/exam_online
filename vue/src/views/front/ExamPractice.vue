<template>
  <div class="practice-container">
    <div class="page-header">
      <h1>模拟考试</h1>
      <p>选择课程和难度，生成完整试卷进行考试</p>
    </div>

    <div class="practice-form">
      <el-card>
        <el-form :model="form" label-width="120px" label-position="top">
          <el-form-item label="选择课程">
            <el-select 
              v-model="form.courseId" 
              placeholder="请选择课程"
              style="width: 100%"
              size="large"
              @change="loadKnowledgePoints"
            >
              <el-option
                v-for="course in courseList"
                :key="course.id"
                :label="course.name"
                :value="course.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="选择难度">
            <el-radio-group v-model="form.difficulty" size="large">
              <el-radio :value="1">简单</el-radio>
              <el-radio :value="2">较简单</el-radio>
              <el-radio :value="3">中等</el-radio>
              <el-radio :value="4">较难</el-radio>
              <el-radio :value="5">困难</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="知识点" v-if="knowledgePointList.length > 0">
            <el-checkbox-group v-model="form.knowledgePoints">
              <el-checkbox 
                v-for="kp in knowledgePointList" 
                :key="kp" 
                :value="kp"
              >
                {{ kp }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              style="padding: 20px 40px; font-size: 16px;"
              @click="startPractice"
              :loading="loading"
              :disabled="!form.courseId"
            >
              开始考试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div class="practice-tips">
      <el-card>
        <h3>考试说明</h3>
        <ul>
          <li>模拟考试会根据您选择的条件自动生成一套完整试卷</li>
          <li>可以选择难度和知识点进行针对性考试</li>
          <li>考试有时间限制，请在规定时间内完成</li>
          <li>考试完成后可以看到成绩和正确答案</li>
          <li>模拟考试不计入正式成绩，仅供练习使用</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'

const router = useRouter()

const loading = ref(false)
const courseList = ref([])
const knowledgePointList = ref([])

const form = reactive({
  courseId: null,
  difficulty: 3,
  knowledgePoints: []
})

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

const loadKnowledgePoints = async () => {
  if (!form.courseId) {
    knowledgePointList.value = []
    return
  }
  
  try {
    const res = await request.get('/question/getKnowledgePoints', {
      params: { courseId: form.courseId }
    })
    if (res.code === '200') {
      knowledgePointList.value = res.data || []
    }
  } catch (error) {
    console.error('加载知识点失败:', error)
    knowledgePointList.value = []
  }
}

const startPractice = async () => {
  if (!form.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  
  loading.value = true
  try {
    const res = await request.post('/exam/practice', null, {
      params: {
        courseId: form.courseId,
        difficulty: form.difficulty,
        knowledgePoints: form.knowledgePoints.length > 0 ? form.knowledgePoints.join(',') : null
      }
    })
    
    if (res.code === '200') {
      ElMessage.success('试卷生成成功，开始考试')
      router.push(`/front/exam/${res.data.paperId}`)
    } else {
      ElMessage.error(res.msg || '生成试卷失败')
    }
  } catch (error) {
    console.error('开始考试失败:', error)
    ElMessage.error('生成试卷失败，请确保课程题库题目充足')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.practice-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.practice-form {
  margin-bottom: 24px;
}

.practice-form :deep(.el-card) {
  padding: 20px;
}

.practice-tips {
  margin-top: 24px;
}

.practice-tips h3 {
  color: #333;
  margin-bottom: 16px;
}

.practice-tips ul {
  color: #666;
  padding-left: 20px;
}

.practice-tips li {
  margin-bottom: 8px;
  line-height: 1.6;
}
</style>
