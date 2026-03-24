<template>
  <div class="practice-exam-container" v-loading="loading">
    <!-- 顶部进度条 -->
    <div class="progress-bar">
      <div class="progress-info">
        <span class="course-name">{{ courseName }}</span>
        <span class="progress-text">已做 {{ answeredCount }} / {{ questions.length }}</span>
      </div>
      <div class="progress-actions">
        <el-button size="small" @click="resetPractice" :disabled="answeredCount === 0">重置</el-button>
        <el-button size="small" @click="goBack">返回</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-bar" v-if="questions.length > 0">
      <el-checkbox-group v-model="filterTypes" @change="applyFilter">
        <el-checkbox :value="1">单选题</el-checkbox>
        <el-checkbox :value="2">多选题</el-checkbox>
        <el-checkbox :value="3">判断题</el-checkbox>
        <el-checkbox :value="4">填空题</el-checkbox>
        <el-checkbox :value="5">简答题</el-checkbox>
      </el-checkbox-group>
    </div>

    <!-- 题目列表 -->
    <div class="questions-container" v-if="filteredQuestions.length > 0">
      <div 
        v-for="(question, index) in filteredQuestions" 
        :key="question.id"
        class="question-card"
        :class="{ answered: userAnswers[question.id] }"
        :id="`question-${question.id}`"
      >
        <div class="question-header">
          <div class="question-meta">
            <el-tag :type="getTypeTag(question.typeId)" size="small">
              {{ getTypeName(question.typeId) }}
            </el-tag>
            <span class="score">{{ question.score }}分</span>
            <el-tag v-if="userAnswers[question.id]" :type="isCorrect(question.id) ? 'success' : 'danger'" size="small">
              {{ isCorrect(question.id) ? '正确' : '错误' }}
            </el-tag>
          </div>
          <span class="question-number">{{ index + 1 }}</span>
        </div>

        <div class="question-text">
          {{ question.name }}
        </div>

        <!-- 单选题 -->
        <div v-if="question.typeId === 1" class="options-area">
          <el-radio-group 
            v-model="userAnswers[question.id]" 
            @change="(val) => submitSingleAnswer(question.id, val, question.referenceAnswer)"
          >
            <el-radio 
              v-for="opt in question.options" 
              :key="opt.id" 
              :label="opt.optionLabel"
              class="option-item"
              :disabled="submittedAnswers[question.id]"
            >
              <span class="option-label">{{ opt.optionLabel }}.</span>
              <span class="option-content">{{ opt.optionContent }}</span>
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 多选题 -->
        <div v-else-if="question.typeId === 2" class="options-area">
          <el-checkbox-group 
            v-model="multiAnswers[question.id]" 
            @change="(val) => submitMultiAnswer(question.id, val, question.referenceAnswer)"
          >
            <el-checkbox 
              v-for="opt in question.options" 
              :key="opt.id" 
              :label="opt.optionLabel"
              class="option-item"
              :disabled="submittedAnswers[question.id]"
            >
              <span class="option-label">{{ opt.optionLabel }}.</span>
              <span class="option-content">{{ opt.optionContent }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <!-- 判断题 -->
        <div v-else-if="question.typeId === 3" class="options-area">
          <el-radio-group 
            v-model="userAnswers[question.id]" 
            @change="(val) => submitSingleAnswer(question.id, val, question.referenceAnswer)"
          >
            <el-radio label="正确" class="option-item" :disabled="submittedAnswers[question.id]">正确</el-radio>
            <el-radio label="错误" class="option-item" :disabled="submittedAnswers[question.id]">错误</el-radio>
          </el-radio-group>
        </div>

        <!-- 填空题/简答题 -->
        <div v-else class="options-area">
          <el-input
            v-model="textAnswers[question.id]"
            type="textarea"
            :rows="3"
            placeholder="请输入答案"
            :disabled="submittedAnswers[question.id]"
          />
          <el-button 
            type="primary" 
            size="small" 
            style="margin-top: 10px"
            @click="submitTextAnswer(question.id, question.referenceAnswer)"
            :disabled="submittedAnswers[question.id] || !textAnswers[question.id]"
          >
            提交
          </el-button>
        </div>

        <!-- 答题结果 -->
        <div v-if="submittedAnswers[question.id]" class="answer-result">
          <div class="result-row">
            <span class="label">正确答案：</span>
            <span class="value correct">{{ question.referenceAnswer || '无' }}</span>
          </div>
          <div class="result-row" v-if="question.typeId !== 4 && question.typeId !== 5">
            <span class="label">你的答案：</span>
            <span class="value" :class="isCorrect(question.id) ? 'correct' : 'wrong'">
              {{ getUserAnswer(question.id) }}
            </span>
          </div>
          <div class="ai-help-row">
            <el-button 
              type="primary" 
              size="small" 
              @click="askAI(question)"
              class="ask-ai-btn"
            >
              <el-icon><ChatDotRound /></el-icon>
              问AI
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="没有找到符合条件的题目"></el-empty>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import { getCurrentUser } from '@/utils/userStorage.js'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const courseId = ref(route.params.courseId)
const isWrongMode = ref(route.query.wrong === '1')
const user = getCurrentUser()
const courseName = ref(isWrongMode.value ? '错题练习' : '')
const questions = ref([])
const allQuestions = ref([])

const userAnswers = reactive({})
const multiAnswers = reactive({})
const textAnswers = reactive({})
const submittedAnswers = reactive({})
const answerResults = reactive({})

const filterTypes = ref([])

const filteredQuestions = computed(() => {
  if (filterTypes.value.length === 0) {
    return questions.value
  }
  return questions.value.filter(q => filterTypes.value.includes(q.typeId))
})

const answeredCount = computed(() => {
  return Object.keys(submittedAnswers).length
})

const STORAGE_KEY = computed(() => `practice_${courseId.value}`)
const WRONG_KEY = computed(() => `wrong_${courseId.value}`)
const WRONG_PROGRESS_KEY = computed(() => `wrong_practice_${courseId.value}`)

// 保存错题到localStorage
const saveWrongQuestion = (questionId) => {
  const key = WRONG_KEY.value
  console.log('保存错题，key:', key, 'questionId:', questionId)
  const wrongIds = JSON.parse(localStorage.getItem(key) || '[]')
  if (!wrongIds.includes(questionId)) {
    wrongIds.push(questionId)
    localStorage.setItem(key, JSON.stringify(wrongIds))
    console.log('错题已保存，当前错题列表:', wrongIds)
  }
}

const getTypeTag = (typeId) => {
  const tags = { 1: 'success', 2: 'success', 3: 'info', 4: 'warning', 5: 'danger' }
  return tags[typeId] || 'info'
}

const getTypeName = (typeId) => {
  const names = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
  return names[typeId] || '未知'
}

const isCorrect = (questionId) => {
  return answerResults[questionId] === true
}

const getUserAnswer = (questionId) => {
  const q = questions.value.find(q => q.id === questionId)
  if (!q) return ''
  
  if (q.typeId === 2) {
    return multiAnswers[questionId]?.join(',') || ''
  }
  return userAnswers[questionId] || ''
}

const submitSingleAnswer = (questionId, answer, correctAnswer) => {
  submittedAnswers[questionId] = true
  const isCorrect = answer === correctAnswer
  answerResults[questionId] = isCorrect
  if (!isCorrect) {
    saveWrongQuestion(questionId)
  }
  saveProgress()
}

const submitMultiAnswer = (questionId, answers, correctAnswer) => {
  if (!answers || answers.length === 0) return
  
  const userAnswer = answers.join(',')
  const correct = correctAnswer ? answers.sort().join(',') === correctAnswer.split(',').sort().join(',') : false
  
  submittedAnswers[questionId] = true
  answerResults[questionId] = correct
  if (!correct) {
    saveWrongQuestion(questionId)
  }
  saveProgress()
}

const submitTextAnswer = (questionId, correctAnswer) => {
  submittedAnswers[questionId] = true
  // 简答题和填空题暂时标记为正确（用户自查）
  answerResults[questionId] = null
  saveProgress()
}

const applyFilter = () => {
  questions.value = allQuestions.value
  if (filterTypes.value.length > 0) {
    questions.value = allQuestions.value.filter(q => filterTypes.value.includes(q.typeId))
  }
}

const loadQuestions = async () => {
  loading.value = true
  try {
    let res
    if (isWrongMode.value) {
      // 刷错题模式：从localStorage读取错题ID
      const wrongKey = `wrong_${courseId.value}`
      const wrongIds = JSON.parse(localStorage.getItem(wrongKey) || '[]')
      
      if (!wrongIds || wrongIds.length === 0) {
        ElMessage.warning('暂无错题记录')
        loading.value = false
        return
      }
      
      const questionIdsStr = wrongIds.join(',')
      res = await request.get('/question/wrong-questions', {
        params: { questionIds: questionIdsStr }
      })
    } else {
      // 普通刷题模式
      res = await request.get('/question/list', {
        params: { courseId: courseId.value }
      })
    }
    
    if (res.code === '200') {
      allQuestions.value = res.data || []
      questions.value = [...allQuestions.value]
      if (!isWrongMode.value) {
        courseName.value = allQuestions.value[0]?.courseName || '刷题练习'
      }
      
      // 加载保存的进度
      loadProgress()
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

const saveProgress = () => {
  // 错题练习模式和普通刷题模式使用不同的key
  const key = isWrongMode.value ? WRONG_PROGRESS_KEY.value : STORAGE_KEY.value
  const progress = {
    userAnswers: { ...userAnswers },
    multiAnswers: { ...multiAnswers },
    textAnswers: { ...textAnswers },
    submittedAnswers: { ...submittedAnswers },
    answerResults: { ...answerResults }
  }
  localStorage.setItem(key, JSON.stringify(progress))
}

const loadProgress = () => {
  // 错题练习模式不加载普通刷题的进度
  if (isWrongMode.value) {
    return
  }
  const key = STORAGE_KEY.value
  const saved = localStorage.getItem(key)
  if (saved) {
    try {
      const progress = JSON.parse(saved)
      Object.assign(userAnswers, progress.userAnswers || {})
      Object.assign(multiAnswers, progress.multiAnswers || {})
      Object.assign(textAnswers, progress.textAnswers || {})
      Object.assign(submittedAnswers, progress.submittedAnswers || {})
      Object.assign(answerResults, progress.answerResults || {})
    } catch (e) {
      console.error('加载进度失败:', e)
    }
  }
}

const resetPractice = () => {
  // 错题练习模式和普通刷题模式使用不同的key
  const key = isWrongMode.value ? WRONG_PROGRESS_KEY.value : STORAGE_KEY.value
  localStorage.removeItem(key)
  Object.keys(userAnswers).forEach(k => delete userAnswers[k])
  Object.keys(multiAnswers).forEach(k => delete multiAnswers[k])
  Object.keys(textAnswers).forEach(k => delete textAnswers[k])
  Object.keys(submittedAnswers).forEach(k => delete submittedAnswers[k])
  Object.keys(answerResults).forEach(k => delete answerResults[k])
  ElMessage.success('已重置')
}

const goBack = () => {
  router.push('/front/practice')
}

const askAI = (question) => {
  // 构造题目信息
  let questionInfo = `题目：${question.name}\n`
  
  // 添加选项信息（如果有）
  if (question.options && question.options.length > 0) {
    questionInfo += '选项：\n'
    question.options.forEach(opt => {
      questionInfo += `${opt.optionLabel}. ${opt.optionContent}\n`
    })
  }
  
  // 添加题目类型和分值
  questionInfo += `题型：${getTypeName(question.typeId)}\n`
  questionInfo += `分值：${question.score}分\n`
  
  // 添加正确答案（如果已提交）
  if (submittedAnswers[question.id] && question.referenceAnswer) {
    questionInfo += `正确答案：${question.referenceAnswer}\n`
    questionInfo += `你的答案：${getUserAnswer(question.id)}\n`
  }
  
  // 构造AI提问内容
  const aiMessage = `请帮我分析这道${getTypeName(question.typeId)}：\n\n${questionInfo}\n请详细解释解题思路和知识点。`
  
  // 跳转到AI助手页面并传递题目信息
  const encodedMessage = encodeURIComponent(aiMessage)
  router.push(`/front/ai?message=${encodedMessage}`)
}

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.practice-exam-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

.progress-bar {
  background: #fff;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.course-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.progress-text {
  font-size: 16px;
  color: #409eff;
  font-weight: bold;
}

.progress-actions {
  display: flex;
  gap: 10px;
}

.filter-bar {
  background: #fff;
  padding: 15px 30px;
  margin-bottom: 20px;
}

.questions-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.question-card {
  background: #fff;
  border-radius: 8px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.question-card.answered {
  border-left: 4px solid #409eff;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score {
  color: #f56c6c;
  font-weight: bold;
}

.question-number {
  font-size: 18px;
  font-weight: bold;
  color: #909399;
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 20px;
  color: #303133;
}

.options-area {
  margin-bottom: 15px;
}

.option-item {
  display: block;
  padding: 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  margin-bottom: 10px;
  transition: all 0.3s;
}

.option-item:hover:not(.is-disabled) {
  border-color: #409eff;
}

.option-item.is-disabled {
  cursor: default;
  opacity: 0.7;
}

.option-label {
  font-weight: bold;
  margin-right: 8px;
}

.answer-result {
  margin-top: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.result-row {
  display: flex;
  margin-bottom: 8px;
}

.result-row:last-child {
  margin-bottom: 0;
}

.result-row .label {
  width: 80px;
  color: #909399;
}

.result-row .value.correct {
  color: #67c23a;
  font-weight: bold;
}

.result-row .value.wrong {
  color: #f56c6c;
  font-weight: bold;
}

.ai-help-row {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #e0e0e0;
}

.ask-ai-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  font-weight: 500;
  transition: all 0.3s ease;
}

.ask-ai-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.ask-ai-btn .el-icon {
  margin-right: 6px;
}
</style>
