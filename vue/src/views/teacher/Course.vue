<template>
  <div class="course-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>课程管理</h2>
      <p>管理您的课程信息</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        v-model="data.name"
        placeholder="搜索课程名称..."
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
      </div>
    </div>

    <!-- 课程列表 -->
    <div class="course-grid" v-loading="data.loading">
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无课程数据" />

      <div
        v-for="course in data.tableData"
        :key="course.id"
        class="course-card"
      >
        <!-- 课程封面 -->
        <div class="course-cover">
          <img v-if="course.img" :src="course.img" :alt="course.name" />
          <div v-else class="course-fallback">
            <span>{{ course.name?.charAt(0) || '课' }}</span>
          </div>
        </div>

        <!-- 课程信息 -->
        <div class="course-info">
          <h3 class="course-name">{{ course.name }}</h3>
          <div class="course-meta">
            <span><el-icon><Document /></el-icon> {{ course.credit || 0 }} 学分</span>
          </div>
          <div class="course-teacher">
            <el-icon><User /></el-icon> {{ course.teacherName || '待设置' }}
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="course-actions">
          <el-button type="primary" link @click="handleEdit(course)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(course.id)">
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
      :title="data.form.id ? '编辑课程' : '新增课程'"
      v-model="data.formVisible"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="80px">
        <el-form-item prop="name" label="课程标题">
          <el-input v-model="data.form.name" placeholder="请输入课程标题" maxlength="100" />
        </el-form-item>
        <el-form-item prop="img" label="课程封面">
          <el-upload
            :action="baseUrl + '/files/upload'"
            :on-success="handleFileUpload"
            list-type="picture"
            :show-file-list="false"
          >
            <div class="upload-preview" v-if="data.form.img">
              <img :src="data.form.img" />
              <div class="upload-mask">点击更换</div>
            </div>
            <div v-else class="upload-btn">
              <el-icon><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item prop="credit" label="课程学分">
          <el-input v-model="data.form.credit" placeholder="请输入课程学分" />
        </el-form-item>
        <el-form-item prop="code" label="课程码">
          <div class="code-input-wrapper">
            <el-input
              v-model="data.form.code"
              placeholder="留空则无需验证码"
              maxlength="10"
            />
            <el-button @click="generateCode">生成</el-button>
          </div>
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
import { getCurrentUser } from "@/utils/userStorage.js"
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, Edit, Plus, Search, Document, User } from "@element-plus/icons-vue"

const formRef = ref()
const baseUrl = import.meta.env.VITE_BASE_URL

const handleFileUpload = (res) => {
  data.form.img = res.data
}

const rules = {
  name: [{ required: true, message: '请输入课程标题', trigger: 'blur' }]
}

const data = reactive({
  user: getCurrentUser(),
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  name: '',
  ids: [],
  loading: false,
  saving: false
})

const load = async () => {
  data.loading = true
  try {
    const res = await request.get('/course/selectPage', {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        name: data.name
      }
    })
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
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
  ElMessageBox.confirm('确定删除该课程吗？', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/course/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const add = async () => {
  data.saving = true
  try {
    const res = await request.post('/course/add', data.form)
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
    const res = await request.put('/course/update', data.form)
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
  data.name = ''
  data.pageNum = 1
  load()
}

const generateCode = () => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  data.form.code = code
}

load()
</script>

<style scoped>
.course-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面头部 */
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

/* 操作栏 */
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

/* 课程网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 400px;
}

/* 课程卡片 */
.course-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s;
}

.course-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 课程封面 */
.course-cover {
  height: 140px;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-fallback span {
  font-size: 48px;
  font-weight: bold;
  color: white;
}

/* 课程信息 */
.course-info {
  padding: 16px;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.course-meta .el-icon {
  font-size: 14px;
}

.course-teacher {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

/* 操作按钮 */
.course-actions {
  display: flex;
  justify-content: space-around;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}

.course-actions .el-button {
  padding: 4px 8px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

/* 课程码输入 */
.code-input-wrapper {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input-wrapper .el-input {
  flex: 1;
}

/* 上传预览 */
.upload-preview {
  width: 120px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-preview:hover .upload-mask {
  opacity: 1;
}

.upload-btn {
  width: 120px;
  height: 80px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
}

.upload-btn .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .course-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .course-grid {
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
