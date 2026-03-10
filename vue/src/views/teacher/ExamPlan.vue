<template>
  <div class="examplan-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>考试安排</h2>
      <p>管理考试发布时间和安排</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        v-model="data.title"
        placeholder="搜索考试标题..."
        clearable
        style="width: 300px;"
        @keyup.enter="load"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="action-right">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增考试</el-button>
      </div>
    </div>

    <!-- 考试安排列表 -->
    <div class="exam-grid" v-loading="data.loading">
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无考试安排" />

      <div
        v-for="exam in data.tableData"
        :key="exam.id"
        class="exam-card"
      >
        <!-- 考试头部 -->
        <div class="exam-header">
          <div class="exam-icon">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="exam-title-wrap">
            <h3 class="exam-title">{{ exam.title }}</h3>
          </div>
        </div>

        <!-- 考试内容 -->
        <div class="exam-content">
          <div class="exam-desc">{{ exam.content || '暂无描述' }}</div>
        </div>

        <!-- 考试信息 -->
        <div class="exam-info">
          <div class="info-item">
            <span class="info-label">发布时间</span>
            <span class="info-value">{{ exam.time }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="exam-actions">
          <el-button type="primary" link @click="handleEdit(exam)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(exam.id)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="data.total">
      <el-pagination
        v-model:current-page="data.pageNum"
        v-model:page-size="data.pageSize"
        :page-sizes="[8, 12, 16, 20]"
        :total="data.total"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="data.form.id ? '编辑考试信息' : '新增考试'"
      v-model="data.formVisible"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="80px">
        <el-form-item prop="title" label="标题">
          <el-input v-model="data.form.title" placeholder="请输入标题" maxlength="100" />
        </el-form-item>
        <el-form-item prop="content" label="内容">
          <el-input v-model="data.form.content" type="textarea" :rows="6" placeholder="请输入内容" maxlength="500" resize="none" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="data.saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import request from "@/utils/request.js"
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, Edit, Plus, Search, Calendar } from "@element-plus/icons-vue"

const formRef = ref()

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  title: '',
  ids: [],
  loading: false,
  saving: false
})

const load = async () => {
  data.loading = true
  try {
    const res = await request.get('/examPlan/selectPage', {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        title: data.title
      }
    })
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    data.loading = false
  }
}

const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该考试安排吗？', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/examPlan/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning('请选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确定删除选中的 <strong style="color: #f56c6c;">${data.ids.length}</strong> 条考试吗？`, '批量删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    dangerouslyUseHTMLString: true
  }).then(() => {
    request.delete('/examPlan/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('批量删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.id)
}

const add = async () => {
  data.saving = true
  try {
    const res = await request.post('/examPlan/add', data.form)
    if (res.code === '200') {
      ElMessage.success('新增成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    data.saving = false
  }
}

const update = async () => {
  data.saving = true
  try {
    const res = await request.put('/examPlan/update', data.form)
    if (res.code === '200') {
      ElMessage.success('修改成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    data.saving = false
  }
}

const save = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const reset = () => {
  data.title = ''
  data.pageNum = 1
  load()
}

load()
</script>

<style scoped>
.examplan-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  color: #909399;
  font-size: 14px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.action-right {
  margin-left: auto;
}

.exam-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 300px;
}

.exam-card {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s;
}

.exam-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.exam-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f8f9fc;
  border-bottom: 1px solid #f0f0f0;
}

.exam-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  flex-shrink: 0;
}

.exam-title-wrap {
  flex: 1;
  min-width: 0;
}

.exam-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-content {
  flex: 1;
  padding: 16px;
  min-height: 80px;
}

.exam-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.exam-info {
  flex-shrink: 0;
  padding: 0 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #606266;
}

.exam-actions {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

@media (max-width: 1200px) {
  .exam-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .exam-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .exam-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-wrap: wrap;
  }

  .action-right {
    width: 100%;
    margin-left: 0;
    margin-top: 10px;
  }
}
</style>
