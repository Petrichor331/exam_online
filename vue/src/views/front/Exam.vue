<template>
  <div class="exam-container" v-loading="loading">
    <!-- 顶部信息栏 -->
    <div class="exam-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle @click="goBack"></el-button>
        <h2>{{ paperName }}</h2>
        <span class="question-progress">题目 {{ currentIndex + 1 }} / {{ questions.length }}</span>
      </div>
      <div class="header-right">
        <div class="countdown" :class="{ warning: remainingTime < 300000 }">
          <el-icon><Timer /></el-icon>
          <span>{{ formatTime(remainingTime) }}</span>
        </div>
        <el-button @click="saveTempAnswer" :loading="saving"> 保存答案 </el-button>
        <el-button type="primary" @click="submitExam" :loading="submitting">交卷</el-button>
      </div>
    </div>
    
    <div class="exam-body">
      <!-- 左侧题目导航 -->
      <div class="question-nav">
        <div class="nav-title">题目导航</div>
        <div class="nav-list">
          <div 
            v-for="(q, index) in questions" 
            :key="q.id"
            :class="['nav-item', { 
              active: currentIndex === index, 
              answered: isAnswered(q.id),
              current: currentIndex === index 
            }]"
            @click="goToQuestion(index)"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="nav-legend">
          <div class="legend-item">
            <span class="dot answered"></span>
            <span>已作答</span>
          </div>
          <div class="legend-item">
            <span class="dot"></span>
            <span>未作答</span>
          </div>
        </div>
      </div>
      
      <!-- 中间答题区 -->
      <div class="question-content">
        <div v-if="currentQuestion" class="question-card">
          <div class="question-header">
            <el-tag :type="getTypeTag(currentQuestion.typeId)" size="small">
              {{ currentQuestion.typeName }}
            </el-tag>
            <span class="score">{{ currentQuestion.score }}分</span>
          </div>
          
          <div class="question-text">
            <span class="question-number">{{ currentIndex + 1 }}.</span>
            {{ currentQuestion.name }}
          </div>
          
          <!-- 单选题 -->
          <div v-if="currentQuestion.typeId === 1" class="options-area">
            <el-radio-group v-model="answers[currentQuestion.id]" class="vertical-radio">
              <el-radio 
                v-for="opt in currentQuestion.options" 
                :key="opt.id" 
                :label="opt.optionLabel"
                class="option-item"
              >
                <span class="option-label">{{ opt.optionLabel }}.</span>
                <span class="option-content">{{ opt.optionContent }}</span>
              </el-radio>
            </el-radio-group>
          </div>
          
          <!-- 多选题 -->
          <div v-else-if="currentQuestion.typeId === 2" class="options-area">
            <el-checkbox-group v-model="answers[currentQuestion.id]" class="vertical-checkbox">
              <el-checkbox 
                v-for="opt in currentQuestion.options" 
                :key="opt.id" 
                :label="opt.optionLabel"
                class="option-item"
              >
                <span class="option-label">{{ opt.optionLabel }}.</span>
                <span class="option-content">{{ opt.optionContent }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
          
          <!-- 判断题 -->
          <div v-else-if="currentQuestion.typeId === 3" class="options-area">
            <el-radio-group v-model="answers[currentQuestion.id]" class="vertical-radio">
              <el-radio label="正确" class="option-item">正确</el-radio>
              <el-radio label="错误" class="option-item">错误</el-radio>
            </el-radio-group>
          </div>
          
          <!-- 填空题 -->
          <div v-else-if="currentQuestion.typeId === 4" class="input-area">
            <el-input 
              v-model="answers[currentQuestion.id]"
              type="textarea"
              :rows="3"
              placeholder="请输入答案"
              resize="none"
            />
          </div>
          
          <!-- 简答题 -->
          <div v-else-if="currentQuestion.typeId === 5" class="input-area">
            <el-input 
              v-model="answers[currentQuestion.id]"
              type="textarea"
              :rows="8"
              placeholder="请输入答案"
              resize="none"
              maxlength="2000"
              show-word-limit
            />
          </div>
          
          <!-- 导航按钮 -->
          <div class="nav-buttons">
            <el-button 
              @click="prevQuestion" 
              :disabled="currentIndex === 0"
              size="large"
            >
              <el-icon><ArrowLeft /></el-icon>上一题
            </el-button>
            <el-button 
              @click="nextQuestion" 
              :disabled="currentIndex === questions.length - 1"
              type="primary"
              size="large"
            >
              下一题<el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.js'
import { Timer, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 数据
const loading = ref(true)
const submitting = ref(false)
const paperId = ref(null)
const scoreId = ref(null)
const paperName = ref('')
const endTime = ref(0)
const paperEndTime = ref(0)  // 试卷设置的结束时间
const remainingTime = ref(0)
const questions = ref([])
const currentIndex = ref(0)
const answers = ref({})
  const saving = ref(false)
  const submitted = ref(false)  // 标记是否已提交试卷
  const isPractice = ref(false)  // 标记是否是模拟考试

let timer = null
let autoSaveTimer = null

// 计算属性
const currentQuestion = computed(() => {
  return questions.value[currentIndex.value]
})

// 判断是否已作答
const isAnswered = (questionId) => {
  const ans = answers.value[questionId]
  if (Array.isArray(ans)) {
    return ans.length > 0
  }
  return ans && ans.trim() !== ''
}

// 获取题型标签类型
const getTypeTag = (typeId) => {
  const types = {
    1: 'primary',    // 单选
    2: 'success',    // 多选
    3: 'warning',    // 判断
    4: 'info',       // 填空
    5: 'danger'      // 简答
  }
  return types[typeId] || 'info'
}

// 格式化时间
const formatTime = (ms) => {
  if (ms <= 0) return '00:00:00'
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

// 开始考试
const startExam = async () => {
  try {
    const res = await request.post('/exam/start', {
      paperId: paperId.value
    })
    if (res.code === '200') {
      const data = res.data
      scoreId.value = data.scoreId
      paperName.value = data.paperName
      endTime.value = data.endTime
      paperEndTime.value = data.paperEndTime || 0
      questions.value = data.questions
      isPractice.value = data.isPractice || false  // 记录是否是模拟考试
      
      // 初始化答案对象
      questions.value.forEach(q => {
        if (q.typeId === 2) {
          answers.value[q.id] = []
        } else {
          answers.value[q.id] = ''
        }
      })
      //尝试获取临时答案
      try{
        const res = await request.get(`/exam/get-temp-answer/${paperId.value}`)
        if(res.code === '200' && res.data && res.data.answers){
          res.data.answers.forEach(item => {
            if(answers.value[item.questionId] !==  undefined){
              //多选题是数组，需要拆分
              if(Array.isArray(answers.value[item.questionId])){
                answers.value[item.questionId] = item.answer ? item.answer.split(',') : []
              }else{
                //除了多选题，其他都是字符串
                answers.value[item.questionId] = item.answer || ''
              }
            }
          })
          ElMessage.success('已恢复上次考试')
        }
      }catch(error){

      }
      // 启动倒计时
      startTimer()
      loading.value = false
    } else {
      ElMessage.error(res.msg || '开始考试失败')
      router.back()
    }
  } catch (error) {
    ElMessage.error(error.message || '开始考试失败')
    router.back()
  }
}

// 倒计时
const startTimer = () => {
  updateRemainingTime()
  timer = setInterval(() => {
    updateRemainingTime()
    if (remainingTime.value <= 0) {
      clearInterval(timer)
      clearInterval(autoSaveTimer)
      ElMessage.warning('考试时间到，正在自动交卷...')
      submitExam(true) //传入true代表自动交卷
    }
  }, 1000)
  
  // 每30秒自动保存答案
  autoSaveTimer = setInterval(() => {
    saveTempAnswer()
  }, 30000)
}

// 检查是否有未保存的答案
const hasUnsavedAnswer = () => {
  return Object.values(answers.value).some(ans => {
    if (Array.isArray(ans)) return ans.length > 0
    return ans && ans.trim() !== ''
  })
}

// 离开确认处理
const handleLeaveConfirm = async () => {
  // 如果已提交试卷，直接离开
  if (submitted.value) {
    return true
  }
  
  //没有未保存的答案，直接离开
  if (!hasUnsavedAnswer()) {
    return true
  }
  
  try {
    await ElMessageBox.confirm(
      '确定要离开吗？是否保存当前答案？', 
      '提示',
      {
        confirmButtonText: '保存',
        cancelButtonText: '不保存',
        distinguishCancelAndClose: true,
      }
    )
    // 点击"保存" -> 保存后直接返回
    await saveTempAnswer()
    ElMessage.success('答案已保存')
    return true
  } catch(action) {
    if (action === 'cancel'){
      return true //不保存但允许离开
    }
    return false//取消离开
  }
  
}

let isNavigatingBack = false

//返回上一页
const goBack = async () => {
  isNavigatingBack = true
  try {
    const result = await handleLeaveConfirm()
    if (result === true) {
      router.back()
    } else {
      isNavigatingBack = false
    }
  } catch {
    isNavigatingBack = false  // 异常时也要重置
  }
}

// 路由离开守卫
onBeforeRouteLeave(async () =>{
  if (isNavigatingBack) {
    isNavigatingBack = false
    return true  // 跳过确认
  }
  const result = await handleLeaveConfirm()
  return result === true
    }
)

const updateRemainingTime = () => {
  const now = Date.now()
  // 取考试时长结束时间和试卷结束时间中较早的那个
  let minEndTime = endTime.value
  if (paperEndTime.value > 0 && paperEndTime.value < minEndTime) {
    minEndTime = paperEndTime.value
  }
  remainingTime.value = Math.max(0, minEndTime - now)
}

// 题目导航
const goToQuestion = (index) => {
  currentIndex.value = index
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

//临时保存答案
const saveTempAnswer = async() =>{
  saving.value = true
  try{
    await request.post('/exam/save-temp-answers',{
      paperId: paperId.value,
      answers: questions.value.map(q => ({
        questionId: q.id,
        answer: Array.isArray(answers.value[q.id])
          ? answers.value[q.id].join(',')
          : answers.value[q.id]
      }))
    })
    ElMessage.success('临时保存成功')
  }catch ( error){
    console.error('保存失败',error)
  }finally {
    saving.value = false
  }
}

// 交卷
const submitExam = async (force=false) => {
  // 检查未作答题目
  const unanswered = questions.value.filter(q => !isAnswered(q.id))
  //如果不是强制交卷，且还有未作答题目
  if (!force && unanswered.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered.length} 道题未作答，确定要交卷吗？`,
        '确认交卷',
        {
          confirmButtonText: '确定交卷',
          cancelButtonText: '继续答题',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    // 保存答案
    const answerList = questions.value.map(q => ({
      questionId: q.id,
      answer: Array.isArray(answers.value[q.id]) 
        ? answers.value[q.id].join(',')
        : answers.value[q.id]
    }))
    
    await request.post('/exam/save-answers', {
      paperId: paperId.value,
      answers: answerList
    })
    
    // 提交试卷
    await request.post(`/exam/submit/${scoreId.value}`)
    
    submitted.value = true  // 标记已提交
    ElMessage.success('交卷成功！')
    
    // 模拟考试：跳转到查看答卷页面；正式考试：跳转到成绩列表
    if (isPractice.value) {
      router.push(`/front/answer/${scoreId.value}`)
    } else {
      router.push('/front/myCourse')
    }
  } catch (error) {
    ElMessage.error(error.message || '交卷失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  paperId.value = parseInt(route.params.paperId)
  if (!paperId.value) {
    ElMessage.error('试卷ID无效')
    router.back()
    return
  }
  startExam()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
  }
})

onMounted(()=>{
  //监听页面关闭/刷新，尝试自动保存
  window.addEventListener('beforeunload', (e) => {
    if (hasUnsavedAnswer()) {
      // 尝试使用 sendBeacon 自动保存（异步，不阻塞页面关闭）
      const answerList = questions.value.map(q => ({
        questionId: q.id,
        answer: Array.isArray(answers.value[q.id])
          ? answers.value[q.id].join(',')
          : answers.value[q.id]
      }))
      
      const data = JSON.stringify({
        paperId: paperId.value,
        answers: answerList
      })
      navigator.sendBeacon('http://localhost:9090/exam/save-temp-answers', new Blob([data], {type: 'application/json'}))
      
      e.returnValue = '答案正在保存...'
    }
  })
})
</script>

<style scoped>
.exam-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 顶部信息栏 */
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.question-progress {
  color: #909399;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f0f9ff;
  border-radius: 20px;
  color: #409eff;
  font-size: 18px;
  font-weight: bold;
  font-family: 'Courier New', monospace;
}

.countdown.warning {
  background: #fef0f0;
  color: #f56c6c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 主体区域 */
.exam-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧导航 */
.question-nav {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 20px;
  overflow-y: auto;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.nav-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}

.nav-item {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.nav-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.nav-item.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.nav-item.answered {
  background: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

.nav-item.answered.active {
  background: #409eff;
}

.nav-legend {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.dot {
  width: 12px;
  height: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
}

.dot.answered {
  background: #67c23a;
  border-color: #67c23a;
}

/* 答题区 */
.question-content {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
}

.question-card {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.score {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 6px;
}

.question-number {
  font-weight: bold;
  margin-right: 8px;
  color: #409eff;
}

/* 选项区域 */
.options-area {
  margin-bottom: 30px;
}

.vertical-radio,
.vertical-checkbox {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s;
  width: 100%;
  margin: 0 !important; /* 移除默认的 margin */
  display: flex !important;
  align-items: flex-start;
}

.option-item:deep(.el-radio__label),
.option-item:deep(.el-checkbox__label) {
  flex: 1;
  white-space: normal;
  line-height: 1.6;
}

.option-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}




.option-label {
  font-weight: bold;
  margin-right: 8px;
  color: #409eff;
}

.option-content {
  color: #303133;
}

/* 输入区域 */
.input-area {
  margin-bottom: 30px;
}

/* 导航按钮 */
.nav-buttons {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* 响应式 */
@media (max-width: 768px) {
  .question-nav {
    display: none;
  }
  
  .exam-header {
    flex-direction: column;
    gap: 10px;
    padding: 10px;
  }
  
  .header-left h2 {
    font-size: 16px;
  }
  
  .question-content {
    padding: 15px;
  }
  
  .question-card {
    padding: 20px;
  }
}
</style>
