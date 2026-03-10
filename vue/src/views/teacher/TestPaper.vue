<template>
  <div class="testpaper-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>试卷管理</h2>
      <p>创建和管理您的试卷</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        v-model="data.name"
        placeholder="搜索试卷名称..."
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增试卷</el-button>
      </div>
    </div>

    <!-- 试卷列表 -->
    <div class="paper-grid" v-loading="data.loading">
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无试卷数据" />

      <div
        v-for="paper in data.tableData"
        :key="paper.id"
        class="paper-card"
      >
        <!-- 试卷头部 -->
        <div class="paper-header">
          <div class="paper-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="paper-title-wrap">
            <h3 class="paper-name">{{ paper.paperName }}</h3>
            <el-tag :type="getStatusType(paper.status)" size="small">{{ paper.status }}</el-tag>
          </div>
        </div>

        <!-- 试卷信息 -->
        <div class="paper-info">
          <div class="info-item">
            <span class="info-label">课程</span>
            <span class="info-value">{{ paper.courseName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">时长</span>
            <span class="info-value">{{ paper.duration }}分钟</span>
          </div>
          <div class="info-item">
            <span class="info-label">出卷</span>
            <span class="info-value">{{ paper.type === 'manual' ? '手动组卷' : '自动组卷' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">时间</span>
            <span class="info-value">{{ paper.startTime }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="paper-actions">
          <el-button type="primary" link @click="handleEdit(paper)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(paper.id)">
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
      :title="data.form.id ? '编辑试卷' : '新增试卷'"
      v-model="data.formVisible"
      width="800px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="100px">
        <!-- 所属课程 -->
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="data.form.courseId" placeholder="请选择试卷所属课程" filterable style="width: 100%">
            <el-option v-for="c in data.courseList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <!-- 试卷名称 -->
        <el-form-item label="试卷名称" prop="paperName">
          <el-input v-model="data.form.paperName" placeholder="请输入试卷名称" show-word-limit maxlength="100" />
        </el-form-item>

        <!-- 开始时间 -->
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="data.form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 结束时间 -->
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="data.form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 考试时长 -->
        <el-form-item label="考试时长" prop="duration">
          <el-input-number v-model="data.form.duration" :min="1" :max="300" style="width: 100%">
            <template #append>分钟</template>
          </el-input-number>
        </el-form-item>

        <!-- 出卷方式 -->
        <el-form-item label="出卷方式" prop="generateType">
          <el-radio-group v-model="data.form.generateType">
            <el-radio label="manual">手动组卷</el-radio>
            <el-radio label="auto">自动组卷</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 手动组卷：题目选择 -->
        <el-form-item v-if="data.form.generateType === 'manual'" label="选择题目" prop="questionIds">
          <div class="question-select-area">
            <div class="selected-questions" v-if="data.selectedQuestions.length > 0">
              <div class="section-title">
                已选题目（{{ data.selectedQuestions.length }}道）
                <el-button type="primary" link @click="showQuestionSelectDialog">+ 添加题目</el-button>
              </div>
              <div class="question-list">
                <div v-for="(q, index) in data.selectedQuestions" :key="q.id" class="question-item">
                  <div class="question-info">
                    <el-tag size="small" type="info">{{ index + 1 }}</el-tag>
                    <span class="question-name" :title="q.name">{{ q.name }}</span>
                    <el-tag size="small" type="warning">{{ q.score }}分</el-tag>
                  </div>
                  <el-button type="danger" link :icon="Delete" @click="removeQuestion(index)" />
                </div>
              </div>
            </div>
            <el-empty v-else description="暂未选择题目" :image-size="80">
              <el-button type="primary" @click="showQuestionSelectDialog">选择题目</el-button>
            </el-empty>
          </div>
        </el-form-item>

        <!-- 自动组卷：难度选择 -->
        <el-form-item v-if="data.form.generateType === 'auto'" label="难度" prop="difficulty">
          <div class="difficulty-wrapper">
            <el-slider
              v-model="data.form.difficulty"
              :min="1"
              :max="5"
              :step="1"
              show-stops
              show-tooltip
              :format-tooltip="(val) => difficultyLabels[val] || val"
              style="flex: 1; margin-right: 15px;"
            />
            <el-tag :type="getDifficultyType(data.form.difficulty)" size="small">
              {{ difficultyLabels[data.form.difficulty] || '请选择' }}
            </el-tag>
          </div>
        </el-form-item>

        <!-- 知识点选择 -->
        <el-form-item v-if="data.form.generateType === 'auto'" label="知识点" prop="knowledgePoints">
          <div class="knowledge-wrapper">
            <div class="selected-knowledge" v-if="data.selectedKnowledgePoints.length > 0">
              <el-tag
                v-for="(kp, index) in data.selectedKnowledgePoints"
                :key="kp"
                closable
                @close="removeKnowledgePoint(index)"
                class="knowledge-tag"
              >
                {{ kp }}
              </el-tag>
              <el-button type="primary" link @click="data.knowledgePointSelectVisible = true">
                + 添加知识点
              </el-button>
            </div>
            <el-button v-else type="primary" @click="data.knowledgePointSelectVisible = true">
              选择知识点
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="data.saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 题目选择弹窗 -->
    <el-dialog v-model="data.questionSelectVisible" title="选择题目" width="900px" destroy-on-close>
      <div class="question-search-bar">
        <el-input v-model="data.questionSearch.name" placeholder="搜索题目..." clearable style="width: 200px" />
        <el-select v-model="data.questionSearch.typeId" placeholder="选择题型" clearable style="width: 150px">
          <el-option v-for="t in data.typeList" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-button type="primary" @click="loadQuestions">搜索</el-button>
      </div>
      <el-table
        ref="questionTableRef"
        :data="data.questionList"
        @selection-change="handleQuestionSelectionChange"
        stripe
        max-height="400"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="name" label="题目内容" show-overflow-tooltip />
        <el-table-column prop="typeName" label="题型" width="80" />
        <el-table-column prop="score" label="分值" width="60" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="data.questionPageNum"
          v-model:page-size="data.questionPageSize"
          :total="data.questionTotal"
          layout="total, prev, pager, next"
          @current-change="loadQuestions"
        />
      </div>
      <template #footer>
        <el-button @click="data.questionSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmQuestionSelect">确认选择（{{ data.tempSelectedQuestions.length }}个）</el-button>
      </template>
    </el-dialog>

    <!-- 知识点选择弹窗 -->
    <el-dialog v-model="data.knowledgePointSelectVisible" title="选择知识点" width="600px">
      <div class="knowledge-search-bar">
        <el-input v-model="data.knowledgePointSearch" placeholder="搜索知识点..." clearable style="width: 200px" />
        <el-button type="primary" @click="loadKnowledgePoints">搜索</el-button>
      </div>
      <div class="knowledge-list" v-if="data.knowledgePointList.length > 0">
        <el-checkbox-group v-model="data.tempSelectedKnowledgePoints">
          <el-checkbox v-for="kp in data.knowledgePointList" :key="kp" :label="kp" class="knowledge-item">
            {{ kp }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <el-empty v-else description="暂无知识点数据" :image-size="80" />
      <template #footer>
        <el-button @click="data.knowledgePointSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmKnowledgePointSelect">
          确认选择（{{ data.tempSelectedKnowledgePoints.length }}个）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import request from "@/utils/request.js"
import { ElMessage, ElMessageBox } from "element-plus"
import { Delete, Edit, Plus, Search, Document } from "@element-plus/icons-vue"

const formRef = ref()
const questionTableRef = ref()
const baseUrl = import.meta.env.VITE_BASE_URL

const difficultyLabels = { 1: '非常简单', 2: '简单', 3: '中等', 4: '困难', 5: '非常困难' }

const getDifficultyType = (val) => {
  const types = { 1: 'success', 2: 'success', 3: 'warning', 4: 'danger', 5: 'danger' }
  return types[val] || 'info'
}

const getStatusType = (status) => {
  const types = { '未开始': 'info', '进行中': 'success', '已结束': 'danger' }
  return types[status] || 'info'
}

const rules = {
  paperName: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
  generateType: [{ required: true, message: '请选择出卷方式', trigger: 'change' }]
}

const data = reactive({
  formVisible: false,
  form: {
    id: null,
    paperName: '',
    startTime: '',
    endTime: '',
    duration: null,
    courseId: null,
    generateType: '',
    questionIds: [],
    difficulty: 3
  },
  courseList: [],
  tableData: [],
  typeList: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  name: '',
  ids: [],
  loading: false,
  saving: false,
  questionSelectVisible: false,
  questionList: [],
  questionLoading: false,
  questionPageNum: 1,
  questionPageSize: 10,
  questionTotal: 0,
  questionSearch: { name: '', typeId: null },
  selectedQuestions: [],
  tempSelectedQuestions: [],
  knowledgePointSelectVisible: false,
  knowledgePointList: [],
  selectedKnowledgePoints: [],
  tempSelectedKnowledgePoints: [],
  knowledgePointSearch: ''
})

const load = async () => {
  data.loading = true
  try {
    const res = await request.get('/testPaper/selectPage', {
      params: { pageNum: data.pageNum, pageSize: data.pageSize, name: data.name }
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

const loadCourses = async () => {
  const res = await request.get('/course/selectAll')
  if (res.code === '200') data.courseList = res.data || []
}

const loadTypes = async () => {
  const res = await request.get('/questionType/selectAll')
  if (res.code === '200') data.typeList = res.data || []
}

const handleAdd = () => {
  data.form = {
    id: null,
    paperName: '',
    startTime: '',
    endTime: '',
    duration: null,
    courseId: null,
    generateType: 'manual',
    questionIds: [],
    difficulty: 3
  }
  data.selectedQuestions = []
  data.selectedKnowledgePoints = []
  data.tempSelectedKnowledgePoints = []
  data.formVisible = true
}

const handleEdit = async (row) => {
  try {
    const res = await request.get('/testPaper/selectById/' + row.id)
    if (res.code === '200') {
      data.form = {
        id: res.data.id,
        paperName: res.data.name,
        courseId: res.data.courseId,
        startTime: res.data.start,
        endTime: res.data.end,
        duration: res.data.time,
        generateType: res.data.type,
        questionIds: []
      }
      data.formVisible = true
    }
  } catch (error) {
    ElMessage.error('加载试卷数据失败')
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该试卷吗？', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete('/testPaper/delete/' + id).then(res => {
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
  ElMessageBox.confirm(`确定删除选中的 <strong style="color: #f56c6c;">${data.ids.length}</strong> 条试卷吗？`, '批量删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    dangerouslyUseHTMLString: true
  }).then(() => {
    request.delete('/testPaper/delete/batch', { data: data.ids }).then(res => {
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
    const res = await request.post('/testPaper/add', data.form)
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
    const res = await request.put('/testPaper/update', data.form)
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

const showQuestionSelectDialog = () => {
  data.questionSelectVisible = true
  data.tempSelectedQuestions = [...data.selectedQuestions]
  loadQuestions()
}

const loadQuestions = async () => {
  data.questionLoading = true
  try {
    const res = await request.get('/question/selectPage', {
      params: {
        pageNum: data.questionPageNum,
        pageSize: data.questionPageSize,
        name: data.questionSearch.name,
        typeId: data.questionSearch.typeId
      }
    })
    if (res.code === '200') {
      data.questionList = res.data?.list || []
      data.questionTotal = res.data?.total || 0
    }
  } finally {
    data.questionLoading = false
  }
}

const handleQuestionSelectionChange = (rows) => {
  data.tempSelectedQuestions = rows
}

const confirmQuestionSelect = () => {
  data.selectedQuestions = [...data.tempSelectedQuestions]
  data.form.questionIds = data.selectedQuestions.map(q => q.id)
  data.questionSelectVisible = false
}

const removeQuestion = (index) => {
  data.selectedQuestions.splice(index, 1)
  data.form.questionIds = data.selectedQuestions.map(q => q.id)
}

const loadKnowledgePoints = async () => {
  const res = await request.get('/knowledgePoint/selectList', { params: { name: data.knowledgePointSearch } })
  if (res.code === '200') data.knowledgePointList = res.data || []
}

const showKnowledgePointSelectDialog = () => {
  data.tempSelectedKnowledgePoints = [...data.selectedKnowledgePoints]
  data.knowledgePointSelectVisible = true
  loadKnowledgePoints()
}

const confirmKnowledgePointSelect = () => {
  data.selectedKnowledgePoints = [...data.tempSelectedKnowledgePoints]
  data.knowledgePointSelectVisible = false
}

const removeKnowledgePoint = (index) => {
  data.selectedKnowledgePoints.splice(index, 1)
}

const removeKnowledge = (index) => {
  data.selectedKnowledgePoint.splice(index, 1)
}

loadTypes()
loadCourses()
load()
</script>

<style scoped>
.testpaper-container {
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

.paper-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 400px;
}

.paper-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s;
}

.paper-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.paper-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f8f9fc;
  border-bottom: 1px solid #f0f0f0;
}

.paper-icon {
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

.paper-title-wrap {
  flex: 1;
  min-width: 0;
}

.paper-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.paper-info {
  padding: 12px 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #606266;
}

.paper-actions {
  display: flex;
  justify-content: space-around;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
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

.question-select-area {
  width: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.question-list {
  max-height: 200px;
  overflow-y: auto;
}

.question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.question-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.difficulty-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
}

.knowledge-wrapper {
  width: 100%;
}

.selected-knowledge {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.knowledge-tag {
  margin-right: 8px;
}

.question-search-bar,
.knowledge-search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.knowledge-list {
  max-height: 300px;
  overflow-y: auto;
}

.knowledge-item {
  display: inline-block;
  width: 30%;
  margin-bottom: 8px;
}

@media (max-width: 1200px) {
  .paper-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .paper-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .paper-grid {
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
