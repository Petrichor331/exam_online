<template>
  <div class="teacher-home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h1>欢迎回来，{{ data.user?.name }} ！</h1>
        <p>今天是 {{ currentDate }}，准备好教学任务了吗？</p>
      </div>
    </div>

    <!-- 快捷入口 -->

    <div class="quick-access">
      <div class="quick-grid">
        <div class="quick-item" @click="goTo('/teacher/grading')">
          <div class="quick-icon">
            <el-icon><Edit /></el-icon>
          </div>
          <div class="quick-text">试卷批阅</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/course')">
          <div class="quick-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="quick-text">课程管理</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/testPaper')">
          <div class="quick-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="quick-text">试卷管理</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/question')">
          <div class="quick-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="quick-text">题库管理</div>
        </div>
        <div class="quick-item" @click="goTo('/teacher/examPlan')">
          <div class="quick-icon">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="quick-text">考试安排</div>
        </div>
      </div>
    </div>

    <!-- 公告 -->
    <div class="section">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Bell /></el-icon>
          <span>系统公告</span>
        </div>
      </div>
      <div v-if="noticeList.length === 0" class="empty-state">
        <el-empty description="暂无公告" />
      </div>
      <div v-else class="notice-list">
        <div 
          v-for="item in noticeList.slice(0, 5)" 
          :key="item.id"
          class="notice-item"
        >
          <div class="notice-title">{{ item.title }}</div>
          <div class="notice-time">{{ item.time }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { getCurrentUser } from '@/utils/userStorage.js'
import { Edit, Reading, Document, Collection, Calendar, Bell } from '@element-plus/icons-vue'

const router = useRouter()

const data = reactive({
  user: getCurrentUser()
})

const currentDate = computed(() => {
  const date = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
})

const noticeList = ref([])

const goTo = (path) => {
  router.push(path)
}

const loadNotice = () => {
  request.get('/notice/selectAll').then(res => {
    if (res.code === '200') {
      noticeList.value = res.data || []
    }
  })
}

onMounted(() => {
  loadNotice()
})
</script>

<style scoped>
.teacher-home {
  max-width: 1200px;
  margin: 0 auto;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #2b2828 0%, #2c2a2a 100%);
  border-radius: 8px;
  padding: 32px 40px;
  color: white;
  margin-bottom: 24px;
}

.banner-content h1 {
  font-size: 28px;
  margin-bottom: 8px;
  color: #fdfdfd
}

.banner-content p {
  font-size: 16px;
  opacity: 0.9;
  color: #fdfdfd
}

/* 快捷入口 */
.quick-access {
  margin-bottom: 24px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  background: #2a2a2a;
  border-radius: 8px;
  border: 1px solid #444;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  border-color: #666;
  background: #333;
}

.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #ed5683;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  border-color: #666;
  background: #fafafa;
}

.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #ed5683;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-bottom: 12px;
}

.quick-text {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
}

/* 区块 */
.section {
  background: #7e7c7c;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #444;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.section-title .el-icon {
  font-size: 18px;
}

/* 区块 */
.section {
  background: #2a2a2a;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #444;
}

/* 公告列表 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #1b1a1a;
  border-radius: 6px;
  cursor: pointer;
}

.notice-item:hover {
  background: #444;
}

.notice-title {
  color: #fff;
  font-weight: 500;
}

.notice-time {
  color: #999;
  font-size: 13px;
}

.empty-state {
  padding: 40px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #46b4e4;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-bottom: 12px;
}
</style>
