<template>
  <div class="profile-page">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="user-info">
        <div class="avatar-wrapper">
          <el-upload
            :action="baseUrl + '/files/upload'"
            :headers="{token:data.user?.token || ''}"
            :on-success="handleFileUpload"
            show-file-list="false"
            class="avatar-uploader"
          >
            <img v-if="data.user.avatar" :src="data.user.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </div>
        <div class="user-name">{{ data.user.name || '未设置姓名' }}</div>
        <div class="user-role">{{ getRoleText(data.user.role) }}</div>
      </div>
      
      <div class="menu-list">
        <div 
          v-for="item in menuList" 
          :key="item.key"
          class="menu-item"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <!-- 基本信息 -->
      <div v-if="activeMenu === 'profile'" class="content-card">
        <div class="card-header">
          <h2>基本信息</h2>
        </div>
        <el-form :model="data.user" label-width="80px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="data.user.username" disabled></el-input>
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="data.user.name" placeholder="请输入姓名"></el-input>
          </el-form-item>
          <el-form-item label="电话">
            <el-input v-model="data.user.phone" placeholder="请输入电话"></el-input>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="data.user.email" placeholder="请输入邮箱"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="update">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码 -->
      <div v-if="activeMenu === 'password'" class="content-card">
        <div class="card-header">
          <h2>修改密码</h2>
        </div>
        <el-form ref="formRef" :model="passwordForm" :rules="passwordRules" label-width="80px" class="password-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码"></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码"></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="changePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>

        <!-- 成绩分析 -->
      <div v-if="activeMenu === 'analysis'" class="content-card analysis-card">
        <div class="card-header">
          <h2>成绩分析</h2>
        </div>
        <!-- 课程选择 -->
        <div class="course-select-wrapper">
          <div class="course-select-label">
            <el-icon><Collection /></el-icon>
            <span>请选择课程</span>
          </div>
          <el-select 
            v-model="selectedCourse" 
            placeholder="选择课程" 
            class="course-select"
            @change="handleCourseChange"
          >
            <el-option v-for="c in courseList" :key="c.courseId" :label="c.courseName" :value="c.courseId" />
          </el-select>
        </div>
        
        <!-- 统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statsData.totalExams }}</div>
              <div class="stat-label">考试次数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statsData.avgScore }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statsData.maxScore }}</div>
              <div class="stat-label">最高分</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statsData.passRate }}%</div>
              <div class="stat-label">及格率</div>
            </div>
          </div>
        </div>
        

        
        <!-- 图表区域 -->
        <div class="charts-container">
          <div class="chart-wrapper">
            <div class="chart-title">成绩趋势</div>
            <v-chart :option="lineChartOption" :autoresize="true" class="chart"></v-chart>
          </div>
          <div class="chart-wrapper">
            <div class="chart-title">知识点掌握程度</div>
            <div v-if="knowledgeStats.length === 0" class="no-data">暂无数据</div>
            <div v-else class="knowledge-list">
              <div v-for="item in knowledgeStats.slice(0, 6)" :key="item.name" class="knowledge-item">
                <div class="knowledge-header">
                  <span class="knowledge-name">{{ item.name }}</span>
                  <span class="knowledge-rate">{{ item.rate }}%</span>
                </div>
                <div class="knowledge-bar">
                  <div class="knowledge-progress" :style="{ width: item.rate + '%' }"></div>
                </div>
                <div class="knowledge-detail">{{ item.correct }}/{{ item.total }} 题</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 成绩列表 -->
        <div class="score-list">
          <div class="list-title">最近成绩</div>
          <el-table :data="filteredScoreList" stripe>
            <el-table-column prop="paperName" label="考试名称" />
            <el-table-column prop="submitTime" label="考试时间" width="180" />
            <el-table-column prop="score" label="得分" width="100">
              <template #default="{ row }">
                <span :class="getScoreClass(row.score)">{{ row.score || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'finished' ? 'success' : 'warning'" size="small">
                  {{ row.status === 'finished' ? '已完成' : '评分中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { getCurrentUser, setCurrentUser } from '@/utils/userStorage.js'
import { 
  User, 
  Lock, 
  DataAnalysis, 
  Document, 
  TrendCharts, 
  Trophy, 
  CircleCheck,
  Plus,
  Collection
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const baseUrl = import.meta.env.VITE_BASE_URL
const formRef = ref()

const activeMenu = ref('profile')
const data = reactive({
  user: getCurrentUser()
})

const menuList = [
  { key: 'profile', label: '基本信息', icon: User },
  { key: 'password', label: '修改密码', icon: Lock },
  { key: 'analysis', label: '成绩分析', icon: TrendCharts }
]

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const statsData = reactive({
  totalExams: 0,
  avgScore: 0,
  maxScore: 0,
  passRate: 0
})

const scoreList = ref([])
const courseList = ref([])
const selectedCourse = ref(null)
const knowledgeStats = ref([])

const filteredScoreList = computed(() => {
  let list = scoreList.value
  // 按课程筛选
  if (selectedCourse.value) {
    list = list.filter(s => s.courseId === selectedCourse.value)
  }
  // 只显示最近5条，按时间倒序
  list = list.slice(0, 5)
  return list
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

const getRoleText = (role) => {
  const roles = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return roles[role] || role
}

const handleFileUpload = (res) => {
  data.user.avatar = res.data
}

const update = () => {
  const apis = {
    ADMIN: '/admin/update',
    TEACHER: '/teacher/update',
    STUDENT: '/student/update'
  }
  request.put(apis[data.user.role], data.user).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      setCurrentUser(data.user)
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const changePassword = () => {
  formRef.value.validate(valid => {
    if (valid) {
      request.put('/updatePassword', {
        password: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      }).then(res => {
        if (res.code === '200') {
          ElMessage.success('修改成功，请重新登录')
          router.push('/login')
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

const lineChartOption = computed(() => {
  const list = filteredScoreList.value.slice().reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { 
      type: 'category', 
      boundaryGap: false,
      data: list.map(s => s.submitTime?.slice(0, 10) || '')
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      name: '成绩',
      type: 'line',
      smooth: true,
      data: list.map(s => s.score || 0),
      lineStyle: { color: '#333' },
      itemStyle: { color: '#333' },
      areaStyle: { color: 'rgba(51,51,51,0.1)' }
    }]
  }
})

const loadScoreData = () => {
  request.get('/score/myScores', {
    params: { studentId: data.user.id }
  }).then(res => {
    if (res.code === '200') {
      scoreList.value = (res.data || []).map(score => ({
        scoreId: score.scoreId,
        paperId: score.paperId,
        paperName: score.paperName,
        studentId: score.studentId,
        submitTime: score.submitTime,
        status: score.status,
        score: score.totalScore,
        courseId: score.courseId,
      }));
      calculateStats()
    }
  })
  
  // 加载课程列表（学生选修的课程）
  request.get('/studentCourse/my-courses').then(res => {
    if (res.code === '200') {
      courseList.value = res.data || []
      // 默认选中第一个课程
      if (courseList.value.length > 0 && !selectedCourse.value) {
        selectedCourse.value = courseList.value[0].courseId
        calculateStats()
        loadKnowledgeStats()
      }
    }
  })
}

const loadKnowledgeStats = () => {
  request.get('/score/knowledge-stats', {
    params: { 
      studentId: data.user.id,
      courseId: selectedCourse.value || null
    }
  }).then(res => {
    console.log('知识点数据:', res)
    if (res.code === '200') {
      knowledgeStats.value = res.data || []
    }
  }).catch(err => {
    console.error('加载知识点失败:', err)
  })
}

const handleCourseChange = () => {
  console.log('选择课程:', selectedCourse.value)
  console.log('成绩列表:', scoreList.value)
  calculateStats()
  loadKnowledgeStats()
}

const calculateStats = () => {
  let list = scoreList.value
  if (selectedCourse.value) {
    // 根据课程筛选
    list = scoreList.value.filter(s => s.courseId === selectedCourse.value)
  }
  const finished = list.filter(s => s.status === 'finished' && s.score !== undefined && s.score !== null)
  if (finished.length > 0) {
    statsData.totalExams = list.length
    const scores = finished.map(s => s.score)
    statsData.avgScore = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
    statsData.maxScore = Math.max(...scores)
    statsData.passRate = Math.round(scores.filter(s => s >= 60).length / scores.length * 100)
  } else {
    statsData.totalExams = 0
    statsData.avgScore = 0
    statsData.maxScore = 0
    statsData.passRate = 0
  }
}

onMounted(() => {
  loadScoreData()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 240px;
  background: #222;
  padding: 30px 0;
  display: flex;
  flex-direction: column;
}

.user-info {
  text-align: center;
  padding-bottom: 30px;
  border-bottom: 1px solid #333;
  margin-bottom: 20px;
}

.avatar-wrapper {
  margin-bottom: 15px;
}

.avatar-uploader {
  display: inline-block;
}

.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: block;
}

.avatar-uploader .el-upload {
  border: 2px dashed #444;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #666;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #666;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.user-role {
  color: #888;
  font-size: 14px;
}

.menu-list {
  flex: 1;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 15px 30px;
  color: #aaa;
  cursor: pointer;
  transition: all 0.3s;
  gap: 12px;
}

.menu-item:hover {
  background: #333;
  color: #fff;
}

.menu-item.active {
  background: #333;
  color: #fff;
  border-left: 3px solid #fff;
}

.content {
  flex: 1;
  padding: 30px;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.card-header {
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.profile-form, .password-form {
  max-width: 500px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 25px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  background: #333;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #888;
  margin-top: 5px;
}

.analysis-card .charts-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  margin-bottom: 30px;
}

.course-select-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #222;
  padding: 20px 25px;
  border-radius: 8px;
  margin-bottom: 25px;
}

.course-select-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
}

.course-select-label .el-icon {
  font-size: 20px;
}

.course-select {
  flex: 1;
}

.course-select :deep(.el-input__wrapper) {
  background: #fff;
  border-radius: 6px;
  box-shadow: none;
  border: 2px solid #444;
}

.course-select :deep(.el-input__wrapper:hover),
.course-select :deep(.el-input__wrapper.is-focus) {
  border-color: #666;
}

.course-select :deep(.el-select__wrapper) {
  box-shadow: none !important;
}

.chart-wrapper {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
  min-height: 300px;
}

.chart-wrapper .no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
  font-size: 14px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.knowledge-item {
  padding: 10px;
  background: #fff;
  border-radius: 6px;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.knowledge-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.knowledge-rate {
  font-size: 14px;
  color: #333;
  font-weight: bold;
}

.knowledge-bar {
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.knowledge-progress {
  height: 100%;
  background: #333;
  border-radius: 4px;
  transition: width 0.3s;
}

.knowledge-detail {
  font-size: 12px;
  color: #888;
  margin-top: 5px;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.chart {
  height: 300px;
}

.score-list .list-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.score-excellent { color: #333; font-weight: bold; }
.score-good { color: #555; font-weight: bold; }
.score-pass { color: #777; font-weight: bold; }
.score-fail { color: #999; font-weight: bold; }

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .analysis-card .charts-container {
    grid-template-columns: 1fr;
  }
}
</style>
