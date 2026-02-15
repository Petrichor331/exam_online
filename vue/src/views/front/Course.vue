<template>
  <div class="course-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>课程中心</h1>
      <p>选择您感兴趣的课程，开启学习之旅</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card blue">
        <div class="stat-icon"><el-icon><Reading /></el-icon></div>
        <div class="stat-info">
          <div class="stat-num">{{ totalCourses }}</div>
          <div class="stat-label">全部课程</div>
        </div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon"><el-icon><Collection /></el-icon></div>
        <div class="stat-info">
          <div class="stat-num">{{ myCourseCount }}</div>
          <div class="stat-label">已选课程</div>
        </div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon"><el-icon><Timer /></el-icon></div>
        <div class="stat-info">
          <div class="stat-num">{{ upcomingExamsCount }}</div>
          <div class="stat-label">待考考试</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKey"
        placeholder="搜索课程名称..."
        clearable
        :prefix-icon="Search"
        @keyup.enter="loadCourses"
      />
      <el-button type="primary" @click="loadCourses">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <!-- 课程列表 -->
    <div class="course-grid" v-loading="loading">
      <div v-if="courseList.length === 0" class="empty-state">
        <el-empty description="暂无课程数据" />
      </div>
      
      <div
        v-for="course in courseList"
        :key="course.id"
        class="course-card"
        :class="{ selected: course.selected }"
      >
        <!-- 课程封面 -->
        <div class="course-cover">
          <img v-if="course.img" :src="course.img" :alt="course.name" class="cover-img" />
          <div v-else class="course-fallback" :style="{ background: getGradient(course.id) }">
            <div class="course-code">{{ course.code || 'C' + course.id }}</div>
          </div>
          <div class="selected-badge" v-if="course.selected">
            <el-icon><Check /></el-icon>已选
          </div>
        </div>
        
        <!-- 课程信息 -->
        <div class="course-info">
          <h3 class="course-name">{{ course.name }}</h3>
          <p class="course-desc">{{ course.description || '暂无课程描述' }}</p>
          
          <div class="course-meta">
            <span><el-icon><User /></el-icon> {{ course.teacherName || '待定' }}</span>
            <span><el-icon><Document /></el-icon> {{ course.questionCount || 0 }}题</span>
          </div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="course-actions">
          <el-button
            v-if="!course.selected"
            type="primary"
            @click="selectCourse(course)"
            :loading="course.loading"
          >
            选课
          </el-button>
          <el-button
            v-else
            type="danger"
            plain
            @click="dropCourse(course)"
            :loading="course.loading"
          >
            退课
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 12, 16, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadCourses"
        @current-change="loadCourses"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.js'
import { Search, Reading, Collection, Timer, User, Document, Check } from '@element-plus/icons-vue'

// 数据
const loading = ref(false)
const searchKey = ref('')
const courseList = ref([])
const myCourseCount = ref(0)
const upcomingExamsCount = ref(0)
const totalCourses = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 渐变色数组
const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
]

const getGradient = (id) => {
  return gradients[id % gradients.length]
}

// 加载课程列表
const loadCourses = async () => {
  loading.value = true
  try {
    // 获取所有课程
    const res = await request.get('/course/selectPage', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        name: searchKey.value
      }
    })
    
    if (res.code === '200') {
      courseList.value = res.data?.list || []
      total.value = res.data?.total || 0
      totalCourses.value = total.value
      
      // 获取已选课程，标记状态
      await markSelectedCourses()
    }
  } catch (error) {
    console.error('加载课程失败:', error)
    ElMessage.error('加载课程失败')
  } finally {
    loading.value = false
  }
}

// 标记已选课程
const markSelectedCourses = async () => {
  try {
    const res = await request.get('/studentCourse/my-courses')
    if (res.code === '200') {
      const myCourses = res.data || []
      myCourseCount.value = myCourses.length
      
      const selectedIds = myCourses.map(c => c.courseId)
      courseList.value.forEach(course => {
        course.selected = selectedIds.includes(course.id)
        course.loading = false
      })
    }
  } catch (error) {
    console.error('获取已选课程失败:', error)
  }
}

// 选课
const selectCourse = async (course) => {
  course.loading = true
  try {
    const res = await request.post(`/studentCourse/select/${course.id}`)
    if (res.code === '200') {
      ElMessage.success('选课成功')
      course.selected = true
      myCourseCount.value++
    } else {
      ElMessage.error(res.msg || '选课失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '选课失败')
  } finally {
    course.loading = false
  }
}

// 退课
const dropCourse = async (course) => {
  try {
    await ElMessageBox.confirm(
      `确定要退选课程【${course.name}】吗？`,
      '确认退课',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    course.loading = true
    const res = await request.post(`/studentCourse/drop/${course.id}`)
    if (res.code === '200') {
      ElMessage.success('退课成功')
      course.selected = false
      myCourseCount.value--
    } else {
      ElMessage.error(res.msg || '退课失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '退课失败')
    }
  } finally {
    course.loading = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchKey.value = ''
  pageNum.value = 1
  loadCourses()
}

// 加载待考考试数量
const loadUpcomingExams = async () => {
  try {
    const res = await request.get('/exam/home')
    if (res.code === '200') {
      upcomingExamsCount.value = (res.data?.upcomingExams || []).length
    }
  } catch (error) {
    console.error('加载待考考试失败:', error)
  }
}

onMounted(() => {
  loadCourses()
  loadUpcomingExams()
})
</script>

<style scoped>
.course-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 32px;
  color: #303133;
  margin-bottom: 10px;
}

.page-header p {
  color: #909399;
  font-size: 16px;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.stat-card.blue .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-card.green .stat-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-card.orange .stat-icon { background: linear-gradient(135deg, #fa709a, #fee140); }

.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-bar .el-input {
  flex: 1;
  max-width: 400px;
}

/* 课程网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.course-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: all 0.3s;
  border: 2px solid transparent;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.course-card.selected {
  border-color: #67c23a;
}

/* 课程封面 */
.course-cover {
  height: 120px;
  position: relative;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-code {
  font-size: 48px;
  font-weight: bold;
  color: rgba(255,255,255,0.9);
}

.selected-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #67c23a;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 课程信息 */
.course-info {
  padding: 16px;
}

.course-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-desc {
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
  height: 42px;
}

.course-meta {
  display: flex;
  gap: 16px;
  color: #606266;
  font-size: 13px;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 操作按钮 */
.course-actions {
  padding: 0 16px 16px;
}

.course-actions .el-button {
  width: 100%;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .course-grid {
    grid-template-columns: 1fr;
  }
  
  .search-bar {
    flex-wrap: wrap;
  }
  
  .search-bar .el-input {
    max-width: 100%;
    width: 100%;
  }
}
</style>