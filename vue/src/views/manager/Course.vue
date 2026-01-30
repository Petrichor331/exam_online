<template>
  <div class="course-container">
    <!-- 搜索区域 -->
    <div class="search-card">
      <div class="search-left">
        <el-input
            v-model="data.name"
            :prefix-icon="Search"
            placeholder="请输入课程名称搜索..."
            class="search-input"
            clearable
            @keyup.enter="load"
        >
          <template #append>
            <el-button :icon="Search" type="primary" @click="load">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="search-right">
        <el-button @click="reset" :icon="RefreshRight">重置</el-button>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-button v-if="data.user.role==='TEACHER'" type="primary" :icon="Plus" @click="handleAdd">新增课程</el-button>
        <el-button type="danger" plain :icon="Delete" @click="delBatch" :disabled="!data.ids.length">
          批量删除
          <el-tag v-if="data.ids.length" type="danger" effect="dark" size="small" class="batch-tag">
            {{ data.ids.length }}
          </el-tag>
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-tooltip content="刷新数据">
          <el-button circle :icon="Refresh" @click="load"></el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="table-card">
      <el-table
          v-loading="data.loading"
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
          highlight-current-row
          :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 'bold' }"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="img" label="课程封面" >
          <template v-slot="scope">
            <el-image style="width:70px; height:50px; border-radius: 5px; display: block "
                      :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported ></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="课程名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="name-icon"><Bell /></el-icon>
              <span class="name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="课程学分" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.credit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.teacherName }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
                v-if="data.user.role === 'TEACHER'"
                type="primary"
                link
                :icon="Edit"
                @click="handleEdit(row)"
            ></el-button>
            <el-divider direction="vertical" />
            <el-button
                type="danger"
                link
                :icon="Delete"
                @click="handleDelete(row.id)"
            ></el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无课程数据" />
    </div>

    <!-- 分页区域 -->
    <div class="pagination-card" v-if="data.total">
      <el-pagination
          v-model:current-page="data.pageNum"
          v-model:page-size="data.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="data.total"
          @size-change="load"
          @current-change="load"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        :title="data.form.id ? '编辑课程' : '新增课程'"
        v-model="data.formVisible"
        width="600px"
        destroy-on-close
        :close-on-click-modal="false"
        class="course-dialog"
    >
      <el-form
          ref="formRef"
          :model="data.form"
          :rules="rules"
          label-width="80px"
          class="course-form"
      >
        <el-form-item prop="name" label="课程标题">
          <el-input
              v-model="data.form.name"
              placeholder="请输入课程标题"
              maxlength="100"
              show-word-limit
              :prefix-icon="Document"
          />
        </el-form-item>
        <el-form-item prop="img" label="课程封面" >
          <el-upload
              :action="baseUrl + '/files/upload'"
              :on-success="handleFileUpload"
              list-type="picture"
          >
            <el-button type="primary">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item prop="credit" label="课程内容">
          <el-input
              v-model="data.form.credit"
              placeholder="请输入课程学分..."
          />
        </el-form-item>
<!--        <el-form-item prop="teacherName" label="授课教师">-->
<!--          <el-input-->
<!--              v-model="data.form.teacherName"-->
<!--              placeholder="请输入授课教师..."-->
<!--          />-->
<!--        </el-form-item>-->

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" @click="save" :loading="data.saving">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import request from "@/utils/request.js"
import { ElMessage, ElMessageBox } from "element-plus"
import {
  Delete,
  Edit,
  Plus,
  Search,
  RefreshRight,
  Refresh,
  Bell,
  Clock,
  Document
} from "@element-plus/icons-vue"
import dayjs from 'dayjs'

const formRef = ref()
const baseUrl = import.meta.env.VITE_BASE_URL


const handleFileUpload = (res) => {
  data.form.img = res.data
}
// 表单校验规则
const rules = {
  title: [
    { required: true, message: '请输入课程标题', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入课程内容', trigger: 'blur' },
    { min: 0, max: 500, message: '长度在 0 到 500 个字符', trigger: 'blur' }
  ]
}

const data = reactive({
  user:JSON.parse(localStorage.getItem('xm-user') || '{}'),
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: '',
  ids: [],
  loading: false,
  saving: false
})



// 加载数据
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
  ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<i class="el-icon" style="font-size: 48px; color: #f56c6c; margin-bottom: 16px;"><svg viewBox="0 0 1024 1024"><path fill="#f56c6c" d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm0 832a384 384 0 1 0 0-768 384 384 0 0 0 0 768zm48-384a48 48 0 1 1-96 0 48 48 0 0 1 96 0zm-48-208a48 48 0 0 1 48 48v176a48 48 0 0 1-96 0V352a48 48 0 0 1 48-48z"/></svg></i>' +
      '<div style="font-size: 16px; color: #303133; margin-bottom: 8px;">确定删除该课程吗？</div>' +
      '<div style="font-size: 13px; color: #909399;">删除后无法恢复，请谨慎操作</div>' +
      '</div>',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        dangerouslyUseHTMLString: true,
        showClose: false,
        center: true
      }
  ).then(() => {
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

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning('请选择要删除的数据')
    return
  }
  ElMessageBox.confirm(
      `确定删除选中的 <strong style="color: #f56c6c; font-size: 16px;">${data.ids.length}</strong> 条课程吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        dangerouslyUseHTMLString: true,
        type: 'warning'
      }
  ).then(() => {
    request.delete('/course/delete/batch', { data: data.ids }).then(res => {
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
  // data.form.teacherId = data.user.id
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

load()
</script>

<style scoped>
.course-container {
  padding: 20px;
  background-color: #fff0f54d;
  min-height: calc(100vh - 84px);
}

/* 搜索卡片 */
.search-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.search-input {
  width: 320px;
}

/* 工具栏卡片 */
.toolbar-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.batch-tag {
  margin-left: 4px;
  border-radius: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 16px;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: #89cff0;
  font-size: 16px;
}

.title-text {
  font-weight: 500;
  color: #303133;
}

.content-text {
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 分页卡片 */
.pagination-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

/* 弹窗内部样式 - 通过类名直接控制 */
.course-form {
  padding: 20px 10px;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
/* 改 el-button type="primary" 的颜色 */
.el-button--primary {
  background-color: #ffb7c5;
  border-color: #ffb7c5;
}

/* 改分页器颜色 */
.el-pagination.is-background .el-pager li:not(.is-disabled).is-active {
  background-color: #ffd1dc;
}

.el-pagination.is-background .el-pager li:not(.is-disabled):hover {
  color: #ffd1dc;
}

/* 改链接按钮颜色 */
.el-button--primary.is-link {
  color: #303133;
}

/* 改分页器当前页颜色 */
.el-pagination.is-background .el-pager li:not(.is-disabled).is-active {
  background-color: #ffb7c5;
  color: #fff;
}

/* 改分页器悬停颜色 */
.el-pagination.is-background .el-pager li:not(.is-disabled):hover {
  color: #ffb7c5;
}

/* 改分页器上一页/下一页按钮颜色 */
.el-pagination.is-background .btn-prev,
.el-pagination.is-background .btn-next {
  background-color: #f4f4f5;
}

.el-pagination.is-background .btn-prev:hover,
.el-pagination.is-background .btn-next:hover {
  color: #ffb7c5;
}

/* 响应式 */
@media screen and (max-width: 768px) {
  .search-card {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .toolbar-card {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }
}

</style>