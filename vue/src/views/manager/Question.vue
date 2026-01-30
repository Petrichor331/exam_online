<template>
  <div class="notice-container">
    <!-- 搜索区域 -->
    <div class="search-card">
      <div class="search-left">
        <el-input
            v-model="data.name"
            :prefix-icon="Search"
            placeholder="请输入题目名称搜索..."
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
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
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column prop="name" label="题干" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="name-icon">
                <Bell/>
              </el-icon>
              <span class="name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.courseName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="选项内容" min-width="260">
          <template #default="{ row }">
            <div v-if="row.optionsText && row.optionsText !== '非选择题'" class="options-content">
              {{ row.optionsText }}
            </div>
            <el-tag v-else type="info" size="small">
              {{ row.optionsText || '无选项' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="typeName" label="题型" width="180" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="referenceAnswer" label="参考答案" width="180" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.referenceAnswer }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="180" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
                type="primary"
                link
                :icon="Edit"
                @click="handleEdit(row)"
            ></el-button>
            <el-divider direction="vertical"/>
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
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无题目数据"/>
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
        :title="data.form.id ? '编辑题目' : '新增题目'"
        v-model="data.formVisible"
        width="700px"
        destroy-on-close
        :close-on-click-modal="false"
    >
      <el-form
          ref="formRef"
          :model="data.form"
          :rules="rules"
          label-width="90px"
      >

        <!-- 题型 -->
        <el-form-item label="所属课程" prop="courseId">
          <el-select
              v-model="data.form.courseId"
              placeholder="请选择题目所属课程"
              filterable
          >
            <el-option
                v-for="c in data.courseList"
                :key="c.id"
                :label="c.name"
                :value="c.id"
            />
          </el-select>
        </el-form-item>

        <!-- 题干 -->
        <el-form-item label="题干" prop="name">
          <el-input
              type="textarea"
              :rows="4"
              v-model="data.form.name"
              placeholder="请输入题目内容（题干）"
              show-word-limit
              maxlength="500"
          />
        </el-form-item>

        <!-- 题型 -->
        <el-form-item label="题型" prop="typeId">
          <el-select
              v-model="data.form.typeId"
              placeholder="请选择题型"
              @change="handleTypeChange"
          >
<!--            <el-option label="单选题" :value="1"/>-->
<!--            <el-option label="多选题" :value="2"/>-->
<!--            <el-option label="判断题" :value="3"/>-->
<!--            <el-option label="填空题" :value="4"/>-->
<!--            <el-option label="简答题" :value="5"/>-->
            <el-option
                v-for="type in data.typeList"
                :key="type.id"
                :label="type.name"
                :value="type.id"
            />
          </el-select>
        </el-form-item>

        <!-- 分值 -->
        <el-form-item label="分值" prop="score">
          <el-input-number
              v-model="data.form.score"
              :min="1"
              :max="100"
          />
        </el-form-item>

        <!-- ================= 选择题选项 ================= -->
        <el-form-item
            v-if="isChoiceQuestion"
            label="选项"
        >
          <div class="option-list">
            <div
                class="option-item"
                v-for="(opt, index) in data.form.options"
                :key="index"
            >
              <span class="option-key">{{ opt.optionLabel }}</span>
              <el-input
                  v-model="opt.optionContent"
                  placeholder="请输入选项内容"
              />
              <el-button
                  icon="Delete"
                  type="danger"
                  link
                  @click="removeOption(index)"
              />
            </div>

            <el-button type="primary" link @click="addOption">
              + 添加选项
            </el-button>
          </div>
        </el-form-item>

        <!-- ================= 标准答案 ================= -->
        <el-form-item label="标准答案" prop="referenceAnswer">

          <!-- 单选 -->
          <el-radio-group
              v-if="data.form.typeId === 1"
              v-model="data.form.referenceAnswer"
          >
            <el-radio
                v-for="opt in data.form.options"
                :key="opt.optionLabel"
                :label="opt.optionLabel"
            >
              {{ opt.optionLabel }}
            </el-radio>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group
              v-else-if="data.form.typeId === 2"
              v-model="data.form.referenceAnswer"
          >
            <el-checkbox
                v-for="opt in data.form.options"
                :key="opt.optionLabel"
                :label="opt.optionLabel"
            >
              {{ opt.optionLabel }}
            </el-checkbox>
          </el-checkbox-group>

          <!-- 判断 -->
          <el-radio-group
              v-else-if="data.form.typeId === 3"
              v-model="data.form.referenceAnswer"
          >
            <el-radio label="T">正确</el-radio>
            <el-radio label="F">错误</el-radio>
          </el-radio-group>

          <!-- 填空题 -->
          <el-input
              v-else-if="data.form.typeId === 4"
              v-model="data.form.referenceAnswer"
              placeholder="请输入填空题正确答案"
              clearable
          />

          <!-- 简答 -->
          <el-input
              v-else-if="data.form.typeId === 4"
              type="textarea"
              :rows="3"
              v-model="data.form.referenceAnswer"
              placeholder="请输入参考答案（用于评分）"
          />

        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {reactive, ref} from "vue"
import request from "@/utils/request.js"
import {ElMessage, ElMessageBox} from "element-plus"
import {computed} from 'vue'
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

// 表单校验规则
const rules = {
  name: [
    {required: true, message: '请输入题干', trigger: 'blur'},
    {min: 1, max: 500, message: '长度在 1 到 500 个字符', trigger: 'blur'}
  ],
  courseId: [
    {required: true, message: '请选择所属课程', trigger: 'change'}
  ],
  typeId: [
    {required: true, message: '请选择题型', trigger: 'change'}
  ],
  score: [
    {required: true, message: '请输入分值', trigger: 'blur'}
  ],
  referenceAnswer: [
    {required: true, message: '请输入标准答案', trigger: 'blur'}
  ],



}

const data = reactive({
  formVisible: false,
  form: {
    id: null,
    name: '',
    typeId: null,
    score: 0,
    referenceAnswer: '',
    options: [],
    courseId: null
  },
  courseList: [],
  tableData: [],
  typeList:[],
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
    const res = await request.get('/question/selectPage', {
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
  data.form = {
    id: null,
    name: '',
    typeId: null,
    score: 0,
    referenceAnswer: '',
    options: [],
    courseId: null
  }
  data.formVisible = true
}


const isChoiceQuestion = computed(() => {
  return data.form.typeId === 1 || data.form.typeId === 2
})

const handleTypeChange = () => {
  data.form.referenceAnswer = ''
  data.form.options = []

  if (isChoiceQuestion.value) {
    addOption()
    addOption()
    addOption()
    addOption()
  }
}

const addOption = () => {
  const key = String.fromCharCode(65 + data.form.options.length)
  data.form.options.push({
    optionLabel: key,
    optionContent: ''
  })
}

const removeOption = (index) => {
  data.form.options.splice(index, 1)
}


const handleEdit = async (row) => {
  // 获取题目详情，包含选项信息
  try {
    const res = await request.get('/question/selectById/' + row.id)
    if (res.code === '200') {
      data.form = {
        id: res.data.id,
        name: res.data.name,
        courseId: res.data.courseId,
        typeId: res.data.typeId,
        score: res.data.score,
        referenceAnswer: res.data.referenceAnswer,
        options: [] // 初始化空数组
      }
      
      // 如果是选择题，加载选项数据
      if (res.data.typeId === 1 || res.data.typeId === 2) {
        const optionsRes = await request.get('/questionOption/selectByQuestionId/' + row.id)
        if (optionsRes.code === '200') {
          data.form.options = optionsRes.data || []
        }
      }
      
      data.formVisible = true
    }
  } catch (error) {
    ElMessage.error('加载题目数据失败')
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<i class="el-icon" style="font-size: 48px; color: #f56c6c; margin-bottom: 16px;"><svg viewBox="0 0 1024 1024"><path fill="#f56c6c" d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm0 832a384 384 0 1 0 0-768 384 384 0 0 0 0 768zm48-384a48 48 0 1 1-96 0 48 48 0 0 1 96 0zm-48-208a48 48 0 0 1 48 48v176a48 48 0 0 1-96 0V352a48 48 0 0 1 48-48z"/></svg></i>' +
      '<div style="font-size: 16px; color: #303133; margin-bottom: 8px;">确定删除该题目吗？</div>' +
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
    request.delete('/question/delete/' + id).then(res => {
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
      `确定删除选中的 <strong style="color: #f56c6c; font-size: 16px;">${data.ids.length}</strong> 条题目吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        dangerouslyUseHTMLString: true,
        type: 'warning'
      }
  ).then(() => {
    request.delete('/question/delete/batch', {data: data.ids}).then(res => {
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
    const res = await request.post('/question/add', data.form)
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
    const res = await request.put('/question/update', data.form)
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
// 加载题型列表
const loadTypes = async () => {
  const res = await request.get('/questionType/selectAll')
  data.typeList = res.data
}

// 加载课程列表
const loadCourses = async () => {
  const res = await request.get('/course/selectAll')
  data.courseList = res.data
}

// 页面初始化时加载
loadTypes()
loadCourses()

load()
</script>

<style scoped>
.notice-container {
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

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-icon {
  color: #89cff0;
  font-size: 16px;
}

.name-text {
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

.options-content {
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
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
.notice-form {
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