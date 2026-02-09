<template>
  <div class="testPaper-container">
    <!-- 搜索区域 -->
    <div class="search-card">
      <div class="search-left">
        <el-input
            v-model="data.name"
            :prefix-icon="Search"
            placeholder="请输入试卷名称搜索..."
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增试卷</el-button>
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
        <el-table-column type="selection" width="40" align="center"/>
        <el-table-column prop="paperName" label="试卷名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <el-icon class="name-icon">
                <Document/>
              </el-icon>
              <span class="name-text">{{ row.paperName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.courseName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="120" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.teacherName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.startTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="160" align="center">
          <template #default="{ row }">
            <span class="content-text">{{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="考试时长" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.duration }}分钟</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="出卷方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'manual' ? 'success' : 'warning'" size="small">
              {{ row.type === 'manual' ? '手动组卷' : '自动组卷' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
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
      <el-empty v-if="!data.tableData.length && !data.loading" description="暂无试卷数据"/>
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
        :title="data.form.id ? '编辑试卷' : '新增试卷'"
        v-model="data.formVisible"
        width="800px"
        destroy-on-close
        :close-on-click-modal="false"
    >
      <el-form
          ref="formRef"
          :model="data.form"
          :rules="rules"
          label-width="100px"
      >
        <!-- 所属课程 -->
        <el-form-item label="所属课程" prop="courseId">
          <el-select
              v-model="data.form.courseId"
              placeholder="请选择试卷所属课程"
              filterable
              style="width: 100%"
          >
            <el-option
                v-for="c in data.courseList"
                :key="c.id"
                :label="c.name"
                :value="c.id"
            />
          </el-select>
        </el-form-item>

        <!-- 试卷名称 -->
        <el-form-item label="试卷名称" prop="paperName">
          <el-input
              v-model="data.form.paperName"
              placeholder="请输入试卷名称"
              show-word-limit
              maxlength="100"
          />
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
          <el-input-number
              v-model="data.form.duration"
              :min="1"
              :max="300"
              placeholder="请输入考试时长"
              style="width: 100%"
          >
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
        <el-form-item 
            v-if="data.form.generateType === 'manual'" 
            label="选择题目"
            prop="questionIds"
        >
          <div class="question-select-area">
            <!-- 已选题目列表 -->
            <div class="selected-questions" v-if="data.selectedQuestions.length > 0">
              <div class="section-title">
                已选题目（{{ data.selectedQuestions.length }}道）
                <el-button type="primary" link @click="showQuestionSelectDialog">
                  + 添加题目
                </el-button>
              </div>
              <div class="question-list">
                <div 
                    v-for="(q, index) in data.selectedQuestions" 
                    :key="q.id"
                    class="question-item"
                >
                  <div class="question-info">
                    <el-tag size="small" type="info">{{ index + 1 }}</el-tag>
                    <span class="question-name" :title="q.name">{{ q.name }}</span>
                    <el-tag size="small" type="warning">{{ q.score }}分</el-tag>
                  </div>
                  <el-button 
                      type="danger" 
                      link 
                      :icon="Delete"
                      @click="removeQuestion(index)"
                  />
                </div>
              </div>
            </div>
            <!-- 空状态 -->
            <el-empty 
                v-else 
                description="暂未选择题目" 
                :image-size="80"
            >
              <el-button type="primary" @click="showQuestionSelectDialog">
                选择题目
              </el-button>
            </el-empty>
          </div>
        </el-form-item>

        <!-- 自动组卷：难度选择，知识点选择 -->
        <el-form-item
            v-if="data.form.generateType === 'auto'"
            label="难度"
            prop="difficulty"
        >
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
            <el-tag :type="getDifficultyType(data.form.difficulty)" size="small" class="difficulty-tag">
              {{ difficultyLabels[data.form.difficulty] || '请选择' }}
            </el-tag>
          </div>
        </el-form-item>
        <!-- 知识点选择 -->
        <el-form-item
            v-if="data.form.generateType === 'auto'"
            label="知识点要求（可选）"
            prop="knowledgePoints"
        >
            <div class="knowledgePoint-area">
              <!-- 已选知识点标签 -->
              <div v-if="data.selectedKnowledgePoints.length > 0" class="selected-tags">
                <el-tag
                    v-for="(kp, index) in data.selectedKnowledgePoints"
                    :key="index"
                    closable
                    @close="removeKnowledgePoint(index)"
                    size="small"
                    type="success"
                    style="margin-right: 8px; margin-bottom: 8px;"
                >
                  {{ kp }}
                </el-tag>
                <el-button type="danger" link size="small" @click="clearKnowledgePoints">
                  清空
                </el-button>
              </div>
              <el-button type="primary" link @click="showKnowledgePointDialog">
                + 选择知识点
              </el-button>
            </div>

        </el-form-item>


      </el-form>

      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="data.saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目选择弹窗 -->
    <el-dialog
        title="选择题目"
        v-model="data.questionSelectVisible"
        width="900px"
        destroy-on-close
        :close-on-click-modal="false"
    >
      <div class="question-select-dialog">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
              v-model="data.questionSearch.name"
              placeholder="搜索题目名称..."
              clearable
              style="width: 200px"
          />
          <el-select 
              v-model="data.questionSearch.typeId" 
              placeholder="题型"
              clearable
              style="width: 120px"
          >
            <el-option 
                v-for="t in data.typeList" 
                :key="t.id" 
                :label="t.name" 
                :value="t.id" 
            />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadQuestions">搜索</el-button>
          <el-button @click="resetQuestionSearch">重置</el-button>
        </div>

        <!-- 题目列表 -->
        <el-table
            v-loading="data.questionLoading"
            :data="data.questionList"
            @selection-change="handleQuestionSelectionChange"
            height="400"
            ref="questionTableRef"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="题目名称" min-width="250" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.name }}
            </template>
          </el-table-column>
          <el-table-column label="题型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.typeName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="80" align="center">
            <template #default="{ row }">
              {{ row.score }}分
            </template>
          </el-table-column>
          <el-table-column label="难度" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.difficulty)" size="small">
                {{ difficultyLabels[row.difficulty] }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="dialog-pagination">
          <el-pagination
              v-model:current-page="data.questionPageNum"
              v-model:page-size="data.questionPageSize"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="data.questionTotal"
              @size-change="loadQuestions"
              @current-change="loadQuestions"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="data.questionSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmQuestionSelect">
          确认选择（{{ data.tempSelectedQuestions.length }}道）
        </el-button>
      </template>
    </el-dialog>

    <!-- 知识点选择弹窗 -->
    <el-dialog
        title="选择知识点"
        v-model="data.knowledgePointSelectVisible"
        width="600px"
        destroy-on-close
        :close-on-click-modal="false"
    >
      <div class="knowledgePoint-dialog">
        <!-- 搜索 -->
        <el-input
            v-model="data.knowledgePointSearch"
            placeholder="搜索知识点..."
            clearable
            style="margin-bottom: 16px;"
        >
          <template #append>
            <el-button :icon="Search" @click="loadKnowledgePoints"></el-button>
          </template>
        </el-input>

        <!-- 知识点列表 -->
        <div v-if="data.knowledgePointList.length > 0" class="knowledgePoint-list">
          <el-checkbox-group v-model="data.tempSelectedKnowledgePoints">
            <el-checkbox
                v-for="kp in data.knowledgePointList"
                :key="kp"
                :label="kp"
                class="knowledgePoint-item"
            >
              {{ kp }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <el-empty v-else description="暂无知识点数据" :image-size="80" />
      </div>

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
import {reactive, ref, nextTick} from "vue"
import request from "@/utils/request.js"
import {ElMessage, ElMessageBox} from "element-plus"
import {
  Delete,
  Edit,
  Plus,
  Search,
  RefreshRight,
  Refresh,
  Document
} from "@element-plus/icons-vue"
import dayjs from 'dayjs'

const formRef = ref()
const questionTableRef = ref()
const baseUrl = import.meta.env.VITE_BASE_URL

// 难度标签映射
const difficultyLabels = {
  1: '非常简单',
  2: '简单', 
  3: '中等',
  4: '困难',
  5: '非常困难'
}

// 获取难度标签颜色
const getDifficultyType = (val) => {
  const types = { 1: 'success', 2: 'success', 3: 'warning', 4: 'danger', 5: 'danger' }
  return types[val] || 'info'
}

// 获取状态标签颜色
const getStatusType = (status) => {
  const types = { '未开始': 'info', '进行中': 'success', '已结束': 'danger' }
  return types[status] || 'info'
}

// 表单校验规则
const rules = {
  paperName: [
    {required: true, message: '请输入试卷名称', trigger: 'blur'},
    {min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur'}
  ],
  courseId: [
    {required: true, message: '请选择所属课程', trigger: 'change'}
  ],
  startTime: [
    {required: true, message: '请选择开始时间', trigger: 'change'}
  ],
  endTime: [
    {required: true, message: '请选择结束时间', trigger: 'change'}
  ],
  duration: [
    {required: true, message: '请输入考试时长', trigger: 'blur'}
  ],
  generateType: [
    {required: true, message: '请选择出卷方式', trigger: 'change'}
  ],
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
  typeList:[],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: '',
  ids: [],
  loading: false,
  saving: false,
  // 题目选择相关
  questionSelectVisible: false,
  questionList: [],
  questionLoading: false,
  questionPageNum: 1,
  questionPageSize: 10,
  questionTotal: 0,
  questionSearch: {
    name: '',
    typeId: null
  },
  selectedQuestions: [],  // 已选题目
  tempSelectedQuestions: [],  // 临时选择的题目
  // 知识点选择相关
  knowledgePointSelectVisible: false,
  knowledgePointList: [],
  selectedKnowledgePoints: [],
  tempSelectedKnowledgePoints: [],
  knowledgePointSearch: ''
})


// 加载数据
const load = async () => {
  data.loading = true
  try {
    const res = await request.get('/testPaper/add', {
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
  // 获取试卷详情
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
  ElMessageBox.confirm(
      '<div style="text-align: center; padding: 20px 0;">' +
      '<i class="el-icon" style="font-size: 48px; color: #f56c6c; margin-bottom: 16px;"><svg viewBox="0 0 1024 1024"><path fill="#f56c6c" d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm0 832a384 384 0 1 0 0-768 384 384 0 0 0 0 768zm48-384a48 48 0 1 1-96 0 48 48 0 0 1 96 0zm-48-208a48 48 0 0 1 48 48v176a48 48 0 0 1-96 0V352a48 48 0 0 1 48-48z"/></svg></i>' +
      '<div style="font-size: 16px; color: #303133; margin-bottom: 8px;">确定删除该试卷吗？</div>' +
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
  ElMessageBox.confirm(
      `确定删除选中的 <strong style="color: #f56c6c; font-size: 16px;">${data.ids.length}</strong> 条试卷吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        dangerouslyUseHTMLString: true,
        type: 'warning'
      }
  ).then(() => {
    request.delete('/testPaper/delete/batch', {data: data.ids}).then(res => {
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
  // 手动组卷时校验是否选择了题目
  if (data.form.generateType === 'manual' && data.selectedQuestions.length === 0) {
    ElMessage.warning('手动组卷需要至少选择一道题目')
    return
  }
  
  data.saving = true
  try {
    // 构造提交的数据
    const submitData = {
      ...data.form,
      questionIds: data.form.generateType === 'manual' ? data.selectedQuestions.map(q => q.id) : [],
      knowledgePoints: data.form.generateType === 'auto' ? data.selectedKnowledgePoints : []
    }
    const res = await request.post('/testPaper/add', submitData)
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

// ================= 题目选择相关方法 =================

// 显示题目选择弹窗
const showQuestionSelectDialog = () => {
  data.questionSelectVisible = true
  data.tempSelectedQuestions = []  // 清空本次选择，使用累加模式
  data.questionSearch = { name: '', typeId: null }
  data.questionPageNum = 1
  loadQuestions()
}

// 加载题目列表
const loadQuestions = async () => {
  data.questionLoading = true
  try {
    const res = await request.get('/question/selectPage', {
      params: {
        pageNum: data.questionPageNum,
        pageSize: data.questionPageSize,
        name: data.questionSearch.name,
        typeId: data.questionSearch.typeId,
        courseId: data.form.courseId,
      }
    })
    if (res.code === '200') {
      data.questionList = res.data?.list || []
      data.questionTotal = res.data?.total || 0
      
      // 恢复已选中的题目状态（包括之前已选的）
      await nextTick()
      data.questionList.forEach(row => {
        // 如果该题目已在已选列表中，或者在本弹窗中已选
        const isSelected = data.selectedQuestions.some(q => q.id === row.id) || 
                           data.tempSelectedQuestions.some(q => q.id === row.id)
        questionTableRef.value?.toggleRowSelection(row, isSelected)
        
        // 如果在已选列表中但不在 temp 中，添加到 temp
        if (data.selectedQuestions.some(q => q.id === row.id) && 
            !data.tempSelectedQuestions.some(q => q.id === row.id)) {
          data.tempSelectedQuestions.push(row)
        }
      })
    }
  } catch (error) {
    ElMessage.error('加载题目失败')
  } finally {
    data.questionLoading = false
  }
}

// 重置题目搜索
const resetQuestionSearch = () => {
  data.questionSearch = { name: '', typeId: null }
  data.questionPageNum = 1
  loadQuestions()
}

// 题目选择变化 - 累加模式
const handleQuestionSelectionChange = (rows) => {
  // 获取当前页所有行的ID
  const currentPageIds = data.questionList.map(q => q.id)
  
  // 从 tempSelectedQuestions 中移除当前页已取消选择的题目
  data.tempSelectedQuestions = data.tempSelectedQuestions.filter(q => {
    // 如果不在当前页，保留；如果在当前页但被取消选择，移除
    if (!currentPageIds.includes(q.id)) return true
    return rows.some(r => r.id === q.id)
  })
  
  // 添加新选中的题目（去重）
  rows.forEach(row => {
    if (!data.tempSelectedQuestions.some(q => q.id === row.id)) {
      data.tempSelectedQuestions.push(row)
    }
  })
}

// 确认选择题目
const confirmQuestionSelect = () => {
  if (data.tempSelectedQuestions.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  // 将本次选择累加到已选列表（去重）
  data.tempSelectedQuestions.forEach(q => {
    if (!data.selectedQuestions.some(sq => sq.id === q.id)) {
      data.selectedQuestions.push(q)
    }
  })
  // 更新 form.questionIds
  data.form.questionIds = data.selectedQuestions.map(q => q.id)
  data.questionSelectVisible = false
  ElMessage.success(`已选择 ${data.selectedQuestions.length} 道题目`)
}

// ================= 知识点选择相关方法 =================

// 显示知识点选择弹窗
const showKnowledgePointDialog = () => {
  data.knowledgePointSelectVisible = true
  data.tempSelectedKnowledgePoints = [...data.selectedKnowledgePoints]
  loadKnowledgePoints()
}

// 加载知识点列表
const loadKnowledgePoints = async () => {
  try {
    const res = await request.get('/question/getKnowledgePoints', {
      params: {
        courseId: data.form.courseId,
        search: data.knowledgePointSearch
      }
    })
    if (res.code === '200') {
      data.knowledgePointList = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载知识点失败')
  }
}

// 确认选择知识点
const confirmKnowledgePointSelect = () => {
  data.selectedKnowledgePoints = [...data.tempSelectedKnowledgePoints]
  data.knowledgePointSelectVisible = false
}

// 移除单个知识点
const removeKnowledgePoint = (index) => {
  data.selectedKnowledgePoints.splice(index, 1)
}

// 清空所有知识点
const clearKnowledgePoints = () => {
  data.selectedKnowledgePoints = []
}

// 移除已选题目
const removeQuestion = (index) => {
  data.selectedQuestions.splice(index, 1)
  data.form.questionIds = data.selectedQuestions.map(q => q.id)
}

//移除已选知识点
const removeKnowledge = (index) => {
  data.selectedKnowledgePoint.splice(index, 1)
}

// 页面初始化时加载
loadTypes()
loadCourses()

load()
</script>

<style scoped>
.testPaper-container {
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
.testPaper-form {
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

/* 题目选择区域样式 */
.question-select-area {
  width: 100%;
}

.selected-questions {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background-color: #f5f7fa;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.question-list {
  max-height: 300px;
  overflow-y: auto;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 8px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}

/* 题目选择弹窗样式 */
.question-select-dialog {
  padding: 10px 0;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}



.section-label {
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.knowledgePoint-area {
  margin-top: 8px;
}

.selected-tags {
  margin-bottom: 12px;
  padding: 8px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

/* 知识点选择弹窗样式 */
.knowledgePoint-dialog {
  padding: 10px 0;
}

.knowledgePoint-list {
  max-height: 350px;
  overflow-y: auto;
  padding: 8px;
}

.knowledgePoint-item {
  display: block;
  padding: 10px 16px;
  margin-bottom: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.3s;
}

.knowledgePoint-item:hover {
  border-color: #409eff;
  background-color: #f5f7fa;
}

.knowledgePoint-item.is-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

/* 难度滑块样式 */
.difficulty-wrapper {
  display: flex;
  align-items: center;
  width: 90%;
}

.difficulty-wrapper .el-slider {
  flex: 1;
  margin-right: 15px;
}

.difficulty-tag {
  min-width: 80px;
  text-align: center;
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