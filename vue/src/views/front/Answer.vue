<template>
  <div class="answer-container" v-loading="loading">
    <!-- 顶部信息 -->
    <div class="answer-header">
      <div class="header-info">
        <h1>{{ examInfo.paperName }}</h1>
        <div class="exam-meta">
          <span><el-icon><Clock /></el-icon> 考试时间：{{ examInfo.startTime }}</span>
          <span><el-icon><User /></el-icon> 授课教师：{{ examInfo.teacherName }}</span>
        </div>
      </div>
      <div class="score-info">
        <div class="score-box">
          <span class="score-label">考试成绩</span>
          <span class="score-value">{{ examInfo.score }}</span>
          <span class="score-total">/ {{ examInfo.totalScore }}分</span>
        </div>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="question-list">
      <div
        v-for="(question, index) in questions"
        :key="question.id"
        class="question-item"
      >
        <!-- 题目头部 -->
        <div class="question-header">
          <div class="question-title">
            <span class="question-number">{{ index + 1 }}.</span>
            <el-tag :type="getTypeTag(question.typeId)" size="small">
              {{ question.typeName }}
            </el-tag>
            <span class="question-score">({{ question.score }}分)</span>
          </div>
          <div class="question-result">
            <el-tag v-if="question.isCorrect === true" type="success" size="small">正确</el-tag>
            <el-tag v-else-if="question.isCorrect === false" type="danger" size="small">错误</el-tag>
            <el-tag v-else type="info" size="small">未批改</el-tag>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="question-content">
          <div class="question-text">{{ question.name }}</div>

          <!-- 选项 -->
          <div v-if="question.options && question.options.length > 0" class="options-list">
            <div
              v-for="option in question.options"
              :key="option.id"
              class="option-item"
              :class="getOptionClass(question, option)"
            >
              <span class="option-label">{{ option.optionLabel }}.</span>
              <span class="option-content">{{ option.optionContent }}</span>
              <el-icon v-if="option.isCorrect" class="correct-icon"><Check /></el-icon>
              <el-icon v-if="isSelected(question, option.optionLabel) && !option.isCorrect" class="wrong-icon"><Close /></el-icon>
            </div>
          </div>

          <!-- 学生答案 -->
          <div class="student-answer">
            <span class="answer-label">你的答案：</span>
            <span class="answer-value" :class="{'is-correct': question.isCorrect === true, 'is-wrong': question.isCorrect === false}">
              {{ question.studentAnswer || '未作答' }}
            </span>
          </div>

          <!-- 标准答案 -->
          <div class="correct-answer" v-if="question.correctAnswer">
            <span class="answer-label">标准答案：</span>
            <span class="answer-value">
              {{ question.correctAnswer }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div class="back-btn">
      <el-button type="primary" size="large" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回考试列表
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { Clock, User, Check, Close, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 数据
const loading = ref(false)
const examInfo = ref({})
const questions = ref([])

// 获取题型标签类型
const getTypeTag = (typeId) => {
  const map = {
    1: '',        // 单选题 - 默认
    2: 'success', // 多选题 - 绿色
    3: 'warning'  // 判断题 - 橙色
  }
  return map[typeId] || ''
}

// 判断选项是否被选中
const isSelected = (question, optionLabel) => {
  const answer = question.studentAnswer
  if (!answer) return false
  
  // 多选题
  if (Array.isArray(answer)) {
    return answer.includes(optionLabel)
  }
  // 单选题/判断题
  return answer === optionLabel
}

// 获取选项样式类
const getOptionClass = (question, option) => {
  const selected = isSelected(question, option.optionLabel)
  
  // 正确选项 - 绿色
  if (option.isCorrect) {
    return 'is-correct'
  }
  // 学生选错的选项 - 红色
  if (selected && !option.isCorrect) {
    return 'is-wrong'
  }
  return ''
}

// 加载答卷数据
const loadAnswer = async () => {
  const scoreId = route.params.scoreId
  if (!scoreId) {
    ElMessage.error('参数错误')
    router.push('/front/examList')
    return
  }

  loading.value = true
  try {
    const res = await request.get(`/exam/answer/${scoreId}`)
    if (res.code === '200') {
      examInfo.value = res.data.examInfo || {}
      questions.value = res.data.questions || []
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载答卷失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/front/examList')
}

onMounted(() => {
  loadAnswer()
})
</script>

<style scoped>
.answer-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

/* 顶部信息 */
.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #e0e0e0;
}

.header-info h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 12px 0;
}

.exam-meta {
  display: flex;
  gap: 24px;
  color: #666;
  font-size: 14px;
}

.exam-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-info {
  text-align: center;
}

.score-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-label {
  color: #666;
  font-size: 14px;
  margin-bottom: 4px;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
  color: #333;
}

.score-total {
  color: #999;
  font-size: 16px;
}

/* 题目列表 */
.question-list {
  background: white;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  padding: 20px;
}

.question-item {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.question-item:last-child {
  border-bottom: none;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.question-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-number {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.question-score {
  color: #999;
  font-size: 14px;
}

/* 题目内容 */
.question-content {
  padding-left: 8px;
}

.question-text {
  font-size: 16px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 16px;
}

/* 选项 */
.options-list {
  margin-bottom: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 6px;
  background: #f9f9f9;
  border: 1px solid #e8e8e8;
  position: relative;
}

.option-item.is-correct {
  background: #f0f9f0;
  border-color: #67c23a;
}

.option-item.is-wrong {
  background: #fef0f0;
  border-color: #f56c6c;
}

.option-label {
  font-weight: bold;
  color: #333;
  margin-right: 8px;
}

.option-content {
  color: #333;
}

.correct-icon {
  position: absolute;
  right: 12px;
  color: #67c23a;
  font-size: 18px;
}

.wrong-icon {
  position: absolute;
  right: 12px;
  color: #f56c6c;
  font-size: 18px;
}

/* 学生答案 */
.student-answer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
  margin-bottom: 8px;
}

/* 标准答案 */
.correct-answer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f0f9f0;
  border-radius: 6px;
  border: 1px solid #33d17a;
}

.correct-answer .answer-value {
  color: #33d17a;
}

.answer-label {
  color: #666;
  font-size: 14px;
}

.answer-value {
  font-size: 16px;
  font-weight: bold;
}

.answer-value.is-correct {
  color: #33d17a;
}

.answer-value.is-wrong {
  color: #e01b24;
}

/* 返回按钮 */
.back-btn {
  text-align: center;
  margin-top: 24px;
}

.back-btn .el-button {
  padding: 12px 32px;
  font-size: 16px;
}
</style>
