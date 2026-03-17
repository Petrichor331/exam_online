<template>
  <div class="student-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle @click="goBack"></el-button>
        <div class="header-title">
          <h2>{{ courseName }} - 已选学生</h2>
          <p>共 {{ data.tableData.length }} 名学生</p>
        </div>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="table-card">
      <el-table
        v-loading="data.loading"
        stripe
        :data="data.tableData"
        highlight-current-row
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 'bold' }"
      >
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.avatar || defaultAvatar">
              {{ row.name?.charAt(0) || '学生' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="150" align="center">
          <template #default="{ row }">
            <span>{{ row.username || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="selectTime" label="选课时间" min-width="180" align="center">
          <template #default="{ row }">
            <span>{{ row.selectTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              :icon="Delete"
              @click="handleRemove(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无学生选择此课程"/>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import request from "@/utils/request.js"
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, ArrowLeft } from "@element-plus/icons-vue"

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const courseId = ref(parseInt(route.params.courseId))
const courseName = ref('')

const data = reactive({
  loading: false,
  tableData: []
})

const goBack = () => {
  router.back()
}

const loadStudents = async () => {
  data.loading = true
  try {
    const res = await request.get(`/studentCourse/teacher/list/${courseId.value}`)
    if (res.code === '200') {
      data.tableData = res.data || []
      
      // 获取课程名称
      if (data.tableData.length > 0) {
        courseName.value = data.tableData[0].courseName || ''
      }
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  } finally {
    data.loading = false
  }
}

const handleRemove = (row) => {
  ElMessageBox.confirm(
    `确定要将学生「${row.name || row.username}」从课程中移除吗？`,
    '移除确认',
    {
      confirmButtonText: '确定移除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await request.delete(`/studentCourse/teacher/${courseId.value}/${row.studentId}`)
      if (res.code === '200') {
        ElMessage.success('已移除该学生')
        loadStudents()
      } else {
        ElMessage.error(res.msg || '移除失败')
      }
    } catch (error) {
      ElMessage.error('移除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  if (!courseId.value) {
    ElMessage.error('课程ID无效')
    router.back()
    return
  }
  loadStudents()
})
</script>

<style scoped>
.student-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-title p {
  margin: 4px 0 0;
  font-size: 14px;
  color: #909399;
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}
</style>
