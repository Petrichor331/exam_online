<template>
  <div class="question-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>题库管理</h2>
      <p>按课程管理您的试题题目</p>
    </div>

    <!-- 选择课程区域 -->
    <div class="course-section" v-if="!selectedCourse">
      <div class="section-title">
        <el-icon><Reading /></el-icon>
        <span>选择课程</span>
      </div>
      
      <el-row :gutter="20" v-loading="courseLoading">
        <el-col :span="8" v-for="course in courseList" :key="course.id">
          <el-card class="course-card" shadow="hover" @click="selectCourse(course)">
            <div class="course-name">{{ course.name }}</div>
            <div class="course-info">
              <span class="question-count">
                <el-tag type="primary" size="small">{{ course.questionCount || 0 }} 道题目</el-tag>
              </span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!courseLoading && !courseList.length" description="暂无课程数据" />
    </div>

    <!-- 题目列表区域 -->
    <div class="paper-section" v-else>
      <div class="section-header">
        <div class="back-btn" @click="selectedCourse = null">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回课程列表</span>
        </div>
        <div class="current-course">
          <span>当前课程：{{ selectedCourse.name }}</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd" style="margin-left: 16px">
            新增题目
          </el-button>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-input
          v-model="searchName"
          placeholder="搜索题目名称..."
          clearable
          style="width: 300px;"
          @keyup.enter="loadQuestions"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="loadQuestions">搜索</el-button>
        <el-button @click="searchName = ''; loadQuestions()">重置</el-button>
      </div>

      <!-- 题目列表 -->
      <div class="question-grid" v-loading="data.loading">
        <el-empty v-if="!data.tableData.length && !data.loading" description="暂无题目数据" />

        <div
          v-for="question in data.tableData"
          :key="question.id"
          class="question-card"
        >
          <!-- 题目头部 -->
          <div class="question-header">
            <div class="question-type-tag">
              <el-tag :type="getDifficultyType(question.difficulty)" size="small">
                {{ difficultyLabels[question.difficulty] || question.difficulty }}
              </el-tag>
              <el-tag type="info" size="small">{{ question.typeName }}</el-tag>
            </div>
            <div class="question-score">{{ question.score }}分</div>
          </div>

          <!-- 题目内容 -->
          <div class="question-content">
            <div class="question-title">{{ question.name }}</div>
          </div>

          <!-- 题目信息 -->
          <div class="question-info">
            <div class="info-item">
              <span class="info-label">答案</span>
              <span class="info-value">{{ question.referenceAnswer || '-' }}</span>
            </div>
            <div class="info-item" v-if="question.knowledgePoint">
              <span class="info-label">知识点</span>
              <span class="info-value">{{ question.knowledgePoint }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="question-actions">
            <el-button type="primary" link @click="handleEdit(question)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="danger" link @click="handleDelete(question.id)">
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
          @size-change="loadQuestions"
          @current-change="loadQuestions"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="data.form.id ? '编辑题目' : '新增题目'"
      v-model="data.formVisible"
      width="700px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="90px">
        <el-form-item label="题目题型" prop="typeId">
          <el-select v-model="data.form.typeId" placeholder="请选择题型" style="width: 100%">
            <el-option v-for="t in typeList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="name">
          <el-input v-model="data.form.name" type="textarea" :rows="4" placeholder="请输入题目内容" />
        </el-form-item>
        <el-form-item label="题目分值" prop="score">
          <el-input-number v-model="data.form.score" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="题目难度" prop="difficulty">
          <el-select v-model="data.form.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option v-for="(label, value) in difficultyLabels" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点" prop="knowledgePoint">
          <el-input v-model="data.form.knowledgePoint" placeholder="请输入知识点" />
        </el-form-item>
        <el-form-item label="参考答案" prop="referenceAnswer">
          <el-input v-model="data.form.referenceAnswer" placeholder="请输入参考答案" />
        </el-form-item>
        <el-form-item v-if="data.form.typeId === 1 || data.form.typeId === 2" label="选项内容">
          <div class="options-editor">
            <div v-for="(opt, index) in data.form.options" :key="index" class="option-row">
              <el-checkbox v-model="opt.isCorrect" :true-label="opt.optionLabel" :false-label="''">
                <el-input v-model="opt.optionContent" placeholder="选项内容" style="width: 300px" />
              </el-checkbox>
              <el-button type="danger" link @click="removeOption(index)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addOption">+ 添加选项</el-button>
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
import { Delete, Edit, Plus, Search, Reading, ArrowLeft } from "@element-plus/icons-vue"

const formRef = ref()

const difficultyLabels = {
  1: '非常简单',
  2: '简单',
  3: '中等',
  4: '困难',
  5: '非常困难'
}

const getDifficultyType = (val) => {
  const types = { 1: 'success', 2: 'success', 3: 'warning', 4: 'danger', 5: 'danger' }
  return types[val] || 'info'
}

const rules = {
  typeId: [{ required: true, message: '请选择题型', trigger: 'change' }],
  name: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const user = getCurrentUser()
const selectedCourse = ref(null)
const courseLoading = ref(false)
const courseList = ref([])
const typeList = ref([])
const searchName = ref('')

const data = reactive({
  formVisible: false,
  form: {
    id: null,
    courseId: null,
    typeId: null,
    name: '',
    score: 5,
    difficulty: 3,
    knowledgePoint: '',
    referenceAnswer: '',
    options: []
  },
  tableData: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  loading: false,
  saving: false
})

// 加载课程列表
const loadCourses = async () => {
  courseLoading.value = true
  try {
    const res = await request.get('/course/selectPage',{
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
      }
    })
    if (res.code === '200') {
      courseList.value = res.data?.list || []

      //为每个课程查询题目数量
      for(const course of courseList.value){
        const countRes = await request.get('/question/selectPage',{
          params:{
            pageNum:1,
            pageSize:1,
            courseId:course.id
          }
        })
        if(countRes.code==='200'){
          course.questionCount = countRes.data?.total || 0
        }
      }
    }
  } catch (error) {
    ElMessage.error('加载课程失败')
  } finally {
    courseLoading.value = false
  }
}

// 加载题型
const loadTypes = async () => {
  const res = await request.get('/questionType/selectAll')
  if (res.code === '200') typeList.value = res.data || []
}

// 选择课程
const selectCourse = (course) => {
  selectedCourse.value = course
  data.pageNum = 1
  searchName.value = ''
  loadQuestions()
}

// 加载题目列表
const loadQuestions = async () => {
  if (!selectedCourse.value) return
  data.loading = true
  try {
    const res = await request.get('/question/selectPage', {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        name: searchName.value,
        courseId: selectedCourse.value.id
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
  data.form = {
    id: null,
    courseId: selectedCourse.value.id,
    typeId: null,
    name: '',
    score: 5,
    difficulty: 3,
    knowledgePoint: '',
    referenceAnswer: '',
    options: [
      { optionLabel: 'A', optionContent: '', isCorrect: '' },
      { optionLabel: 'B', optionContent: '', isCorrect: '' },
      { optionLabel: 'C', optionContent: '', isCorrect: '' },
      { optionLabel: 'D', optionContent: '', isCorrect: '' }
    ]
  }
  data.formVisible = true
}

const handleEdit = async (row) => {
  try {
    const res = await request.get('/question/selectById/' + row.id)
    if (res.code === '200') {
      const q = res.data
      data.form = {
        id: q.id,
        courseId: q.courseId,
        typeId: q.typeId,
        name: q.name,
        score: q.score,
        difficulty: q.difficulty,
        knowledgePoint: q.knowledgePoint,
        referenceAnswer: q.referenceAnswer,
        options: q.options || []
      }
      data.formVisible = true
    }
  } catch (error) {
    ElMessage.error('加载题目数据失败')
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该题目吗？', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/question/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        loadQuestions()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const add = async () => {
  data.saving = true
  try {
    const res = await request.post('/question/add', data.form)
    if (res.code === '200') {
      ElMessage.success('新增成功')
      data.formVisible = false
      loadQuestions()
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
      loadQuestions()
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

const addOption = () => {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
  const nextLabel = labels[data.form.options.length] || String(data.form.options.length + 1)
  data.form.options.push({ optionLabel: nextLabel, optionContent: '', isCorrect: '' })
}

const removeOption = (index) => {
  data.form.options.splice(index, 1)
}

loadCourses()
loadTypes()
</script>

<style scoped>
.question-container {
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

/* 选择课程区域 */
.course-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e4e7ed;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.course-card:hover {
  transform: translateY(-2px);
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.course-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 题目列表区域 */
.paper-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e4e7ed;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #66b1ff;
}

.current-course {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

/* 题目卡片 */
.question-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 300px;
}

/* 题目卡片 */
.question-card {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s;
}

.question-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.question-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fc;
  border-bottom: 1px solid #f0f0f0;
}

.question-type-tag {
  display: flex;
  gap: 8px;
}

.question-score {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
}

.question-content {
  flex: 1;
  padding: 16px;
  min-height: 80px;
  overflow: hidden;
}

.question-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-info {
  flex-shrink: 0;
  padding: 0 16px;
  min-height: 40px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 12px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #606266;
}

.question-actions {
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
}

.options-editor {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

@media (max-width: 1200px) {
  .question-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .question-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .question-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>
