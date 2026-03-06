<template>
  <div class="teacher-container">
    <!-- 顶部导航 -->
    <div class="teacher-header">
      <div class="header-left">
        <img src="@/assets/img/logo.png" alt="">
        <div class="title">教师中心</div>
      </div>
      <div class="header-center">
        <el-menu 
          :default-active="currentMenu" 
          mode="horizontal" 
          :ellipsis="false"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/teacher/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/teacher/grading">
            <el-icon><Edit /></el-icon>
            <span>试卷批阅</span>
          </el-menu-item>
          <el-menu-item index="/teacher/course">
            <el-icon><Reading /></el-icon>
            <span>课程管理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/testPaper">
            <el-icon><Document /></el-icon>
            <span>试卷管理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/question">
            <el-icon><Collection /></el-icon>
            <span>题库管理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/examPlan">
            <el-icon><Calendar /></el-icon>
            <span>考试安排</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <el-dropdown style="cursor: pointer">
          <div style="display: flex; align-items: center; color: #fff;">
            <img style="width: 36px; height: 36px; border-radius: 50%;" :src="data.user?.avatar || defaultAvatar" />
            <span style="margin-left: 8px;">{{ data.user?.name || '未登录' }}</span>
            <el-icon style="margin-left: 4px;"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/teacher/person')">个人资料</el-dropdown-item>
              <el-dropdown-item @click="router.push('/teacher/password')">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="teacher-main">
      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Edit, Reading, Document, Collection, Calendar, ArrowDown } from '@element-plus/icons-vue'
import defaultAvatar from '@/assets/img/avatar.png'
import { getCurrentUser, clearCurrentUser } from '@/utils/userStorage.js'

const router = useRouter()
const route = useRoute()

const data = reactive({
  user: getCurrentUser()
})

const currentMenu = computed(() => {
  return route.path
})

const handleMenuSelect = (index) => {
  router.push(index)
}

const logout = () => {
  clearCurrentUser()
  router.push('/login')
}
</script>

<style scoped>
.teacher-container {
  min-height: 100vh;
  background: #ffffff;
}

/* 顶部导航 - 黑底白字 */
.teacher-header {
  display: flex;
  height: 60px;
  background: #1a1a1a;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #333;
}

.header-left {
  display: flex;
  align-items: center;
  padding-right: 40px;
}

.header-left img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.header-left .title {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  margin-left: 12px;
}

.header-center {
  flex: 1;
}

.header-center .el-menu {
  background: transparent !important;
  border: none !important;
}

.header-center .el-menu-item {
  color: #fdfdfd !important;
  height: 60px;
  line-height: 60px;
}

.header-center .el-menu-item:hover {
  color: #716f6f !important;
  background: #131313 !important;
}

.header-center .el-menu-item.is-active {
  color: #fdfdfd !important;
  border-bottom-color: #fdfdfd !important;
  background: #0e0d0d !important;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-right .el-dropdown {
  color: #fff;
}

/* 主内容区 */
.teacher-main {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: #ffffff;
  min-height: calc(100vh - 60px);
}
</style>
