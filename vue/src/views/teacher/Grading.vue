<template>
  <div>
    <div class="card">
      <div class="header">
        <h2>学生答卷管理</h2>
        <span class="subtitle">按课程查看和批改学生答卷</span>
      </div>
    </div>

    <!-- 课程选择区域 -->
    <div class="course-section" v-if="!selectedCourse">
      <div class="section-title">
        <el-icon><Reading /></el-icon>
        <span>选择课程</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="8" v-for="course in courseList" :key="course.courseId">
          <el-card class="course-card" shadow="hover" @click="selectCourse(course)">
            <div class="course-name">{{ course.courseName }}</div>
            <div class="course-info">
              <span class="credit">{{ course.credit }}学分</span>
              <span class="exam-count">
                <el-tag type="primary" size="small">{{ course.examCount }}份答卷</el-tag>
              </span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!courseList.length" description="暂无教授课程" />
    </div>

    <!-- 答卷列表区域 -->
    <div class="paper-section" v-else>
      <div class="section-header">
        <div class="back-btn" @click="selectedCourse = null">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回课程列表</span>
        </div>
        <div class="current-course">
          <span>当前课程：{{ selectedCourse.courseName }}</span>
        </div>
      </div>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="评分状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="待评分" value="waiting" />
          <el-option label="待评分" value="grading" />
<!--          <el-option label="AI已评分" value="ai_graded" />-->
          <el-option label="已完成" value="finished" />
        </el-select>
        <el-input 
          v-model="searchStudent" 
          placeholder="搜索学生姓名" 
          clearable 
          style="width: 200px; margin-left: 10px"
        />
        <el-button type="primary" @click="loadPaperList" style="margin-left: 10px">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <!-- 答卷表格 -->
      <el-table :data="filteredPaperList" stripe style="width: 100%">
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="paperName" label="试卷名称" min-width="200" />
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column prop="totalScore" label="得分" width="100">
          <template #default="{ row }">
            <span :class="getScoreClass(row.totalScore, row.maxScore)">
              {{ row.totalScore || '-' }}
            </span>
            <span v-if="row.maxScore" class="max-score">/{{ row.maxScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleGrade(row)"
            >
              {{ row.status === 'finished' ? '查看' : '批改' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!paperList.length" description="暂无答卷" />
    </div>

    <!-- 改卷详情弹窗 -->
    <el-dialog
      v-model="gradeDialogVisible"
      :title="gradeDialogTitle"
      width="900px"
      :close-on-click-modal="false"
      class="grade-dialog"
    >
      <div v-if="currentPaper" class="grade-content">
        <!-- 试卷信息 -->
        <div class="paper-info">
          <div class="info-item">
            <span class="label">学生：</span>
            <span class="value">{{ currentPaper.studentName }}</span>
          </div>
          <div class="info-item">
            <span class="label">试卷：</span>
            <span class="value">{{ currentPaper.paperName }}</span>
          </div>
          <div class="info-item">
            <span class="label">总分：</span>
            <span class="value score">{{ currentPaper.totalScore || 0 }} / {{ currentPaper.maxScore || 100 }}</span>
          </div>
        </div>

        <!-- 题目列表 -->
        <div class="question-list">
          <div 
            v-for="(answer, index) in currentPaper.answers" 
            :key="answer.answerId"
            class="question-item"
            :class="{ 'objective': isObjective(answer.questionTypeId) }"
          >
            <div class="question-header">
              <span class="question-no">题目 {{ index + 1 }}</span>
              <el-tag size="small" :type="getQuestionTypeColor(answer.questionTypeId)">
                {{ getQuestionTypeName(answer.questionTypeId) }}
              </el-tag>
              <span class="question-score">
                满分：{{ answer.maxScore }}分
              </span>
            </div>

            <div class="question-content">
              <div class="question-text">{{ answer.questionName }}</div>
              
              <!-- 标准答案（主观题显示） -->
              <div v-if="answer.referenceAnswer" class="reference-answer">
                <span class="label">参考答案：</span>
                <span class="content">{{ answer.referenceAnswer }}</span>
              </div>

              <!-- 学生答案 -->
              <div class="student-answer">
                <span class="label">学生答案：</span>
                <span class="content" :class="{ 'empty': !answer.studentAnswer }">
                  {{ answer.studentAnswer || '未作答' }}
                </span>
              </div>

              <!-- 选择题显示正确答案 -->
              <div v-if="answer.correctAnswer" class="correct-answer">
                <span class="label">正确答案：</span>
                <span class="content">{{ answer.correctAnswer }}</span>
              </div>
            </div>

            <!-- 评分区域 -->
            <div class="grading-area">
              <!-- AI评分显示（简答题才有） -->
              <div class="ai-score" v-if="answer.questionTypeId === 5 && answer.aiScore !== null && answer.aiScore !== undefined">
                <span class="label">AI评分：</span>
                <span class="score">{{ answer.aiScore }}分</span>
                <span class="similarity" v-if="answer.similarity">
                  （相似度：{{ (answer.similarity * 100).toFixed(1) }}%）
                </span>
              </div>
              
              <div class="final-score">
                <span class="label">最终得分：</span>
                <el-input-number 
                  v-model="answer.tempScore" 
                  :min="0" 
                  :max="answer.maxScore"
                  :step="0.5"
                  :precision="1"
                  size="small"
                  style="width: 120px"
                  :disabled="isObjective(answer.questionTypeId)"
                />
                <!-- 客观题显示系统判定分数 -->
                <span v-if="isObjective(answer.questionTypeId)" class="auto-graded-hint">
                  （系统判定：{{ answer.finalScore !== null && answer.finalScore !== undefined ? answer.finalScore : '待评分' }}分，不可修改）
                </span>
                <!-- 简答题显示AI评分参考按钮 -->
                <el-tooltip 
                  v-if="answer.questionTypeId === 5 && answer.aiScore !== null" 
                  content="点击将AI评分填入输入框作为参考，您仍可修改" 
                  placement="top"
                >
                  <el-button 
                    type="info" 
                    size="small" 
                    @click="acceptAiScore(answer)"
                    style="margin-left: 10px"
                    :disabled="answer.tempScore === answer.aiScore"
                  >
                    <el-icon><View /></el-icon>
                    参考AI评分
                  </el-button>
                </el-tooltip>
                <!-- 填空题提示 -->
                <span v-if="answer.questionTypeId === 4" class="manual-grade-hint">
                  （请教师手动评分）
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="total-score-preview">
            预计总分：<span class="score">{{ calculateTempTotal }}</span> / {{ currentPaper?.maxScore || 100 }}
          </div>
          <div class="actions">
            <el-button @click="gradeDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitGrading">保存评分</el-button>
            <el-button type="success" @click="confirmAiGrading" v-if="hasAiGradedQuestions">
              一键确认AI评分
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from "vue";
import request from "@/utils/request.js";
import { getCurrentUser } from "@/utils/userStorage.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Reading, ArrowLeft, Search, View } from "@element-plus/icons-vue";

const data = reactive({
  user: getCurrentUser(),
});

// 课程列表
const courseList = ref([]);
const selectedCourse = ref(null);

// 答卷列表
const paperList = ref([]);
const filterStatus = ref('');
const searchStudent = ref('');

// 改卷弹窗
const gradeDialogVisible = ref(false);
const gradeDialogTitle = ref('批改答卷');
const currentPaper = ref(null);

// 加载课程列表
const loadCourseList = () => {
  request.get('/score/courses', {
    params: { teacherId: data.user.id }
  }).then(res => {
    if (res.code === '200') {
      courseList.value = res.data || [];
    } else {
      ElMessage.error(res.msg || '加载课程列表失败');
    }
  }).catch(err => {
    ElMessage.error('网络错误，请稍后重试');
  });
};

// 选择课程
const selectCourse = (course) => {
  selectedCourse.value = course;
  loadPaperList();
};

// 加载答卷列表
const loadPaperList = () => {
  if (!selectedCourse.value) return;
  
  request.get('/score/list', {
    params: {
      teacherId: data.user.id,
      courseId: selectedCourse.value.courseId
    }
  }).then(res => {
    if (res.code === '200') {
      paperList.value = res.data || [];
    } else {
      ElMessage.error(res.msg || '加载答卷列表失败');
    }
  }).catch(err => {
    ElMessage.error('网络错误，请稍后重试');
  });
};

// 过滤后的答卷列表
const filteredPaperList = computed(() => {
  return paperList.value.filter(paper => {
    const matchStatus = !filterStatus.value || paper.status === filterStatus.value;
    const matchStudent = !searchStudent.value || 
      (paper.studentName && paper.studentName.includes(searchStudent.value));
    return matchStatus && matchStudent;
  });
});

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'grading': 'warning',
    'ai_graded': 'primary',
    'finished': 'success'
  };
  return types[status] || 'info';
};

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    'grading': '待评分',
    'finished': '已完成',
    'waiting': '待考试',

  };
  return texts[status] || status;
};

// 获取分数样式类
const getScoreClass = (score, maxScore) => {
  if (!score) return '';
  const ratio = score / (maxScore || 100);
  if (ratio >= 0.9) return 'excellent';
  if (ratio >= 0.8) return 'good';
  if (ratio >= 0.6) return 'pass';
  return 'fail';
};

// 是否为客观题
const isObjective = (typeId) => {
  return [1, 2, 3].includes(typeId); // 单选、多选、判断
};

// 获取题型名称
const getQuestionTypeName = (typeId) => {
  const names = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '填空题',
    5: '简答题'
  };
  return names[typeId] || '未知题型';
};

// 获取题型颜色
const getQuestionTypeColor = (typeId) => {
  const colors = {
    1: 'success',
    2: 'success',
    3: 'success',
    4: 'warning',
    5: 'danger'
  };
  return colors[typeId] || 'info';
};

// 打开改卷弹窗
const handleGrade = (row) => {
  request.get(`/score/detail/${row.scoreId}`).then(res => {
    if (res.code === '200') {
      currentPaper.value = res.data;
      // 为每道题初始化临时分数
      if (currentPaper.value.answers) {
        currentPaper.value.answers.forEach(answer => {
          // 优先使用finalScore，其次是aiScore，最后是0
          const score = answer.finalScore !== null && answer.finalScore !== undefined 
            ? answer.finalScore 
            : (answer.aiScore !== null && answer.aiScore !== undefined ? answer.aiScore : 0);
          answer.tempScore = score;
          console.log(`题目${answer.questionId}初始化分数:`, score, 'finalScore:', answer.finalScore, 'aiScore:', answer.aiScore);
        });
      }
      gradeDialogTitle.value = row.status === 'finished' ? '查看答卷' : '批改答卷';
      gradeDialogVisible.value = true;
    } else {
      ElMessage.error(res.msg || '加载答卷详情失败');
    }
  }).catch(err => {
    ElMessage.error('网络错误，请稍后重试');
  });
};

// 参考AI评分 - 将AI评分填充到输入框作为参考
const acceptAiScore = (answer) => {
  const oldScore = answer.tempScore;
  answer.tempScore = answer.aiScore;
  ElMessage.success(`已将AI评分 ${answer.aiScore} 分填充到输入框，您可以根据实际情况调整`);
};

// 计算临时总分
const calculateTempTotal = computed(() => {
  if (!currentPaper.value || !currentPaper.value.answers) return 0;
  return currentPaper.value.answers.reduce((sum, answer) => {
    return sum + (parseFloat(answer.tempScore) || 0);
  }, 0);
});

// 是否有AI评分的简答题（typeId=5）
const hasAiGradedQuestions = computed(() => {
  if (!currentPaper.value || !currentPaper.value.answers) return false;
  return currentPaper.value.answers.some(answer => 
    answer.questionTypeId === 5 && answer.aiScore !== null && answer.aiScore !== undefined
  );
});

// 提交评分
const submitGrading = () => {
  const scores = currentPaper.value.answers
    .filter(answer => !isObjective(answer.questionTypeId))
    .map(answer => ({
      answerId: answer.answerId,
      score: answer.tempScore
    }));

  request.post(`/score/batch/${currentPaper.value.scoreId}`, { scores })
    .then(res => {
      if (res.code === '200') {
        ElMessage.success('评分保存成功');
        gradeDialogVisible.value = false;
        loadPaperList();
      } else {
        ElMessage.error(res.msg || '保存失败');
      }
    })
    .catch(err => {
      ElMessage.error('网络错误，请稍后重试');
    });
};

// 一键确认AI评分（仅针对简答题）
const confirmAiGrading = () => {
  ElMessageBox.confirm('确认接受所有简答题的AI评分结果吗？填空题需要手动评分。', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 前端直接应用AI评分到简答题
    let count = 0;
    currentPaper.value.answers.forEach(answer => {
      if (answer.questionTypeId === 5 && answer.aiScore !== null) {
        answer.tempScore = answer.aiScore;
        count++;
      }
    });
    ElMessage.success(`已接受 ${count} 道简答题的AI评分`);
  }).catch(() => {});
};

// 初始化
loadCourseList();
</script>

<style scoped>
.card {
  padding: 20px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
}

.subtitle {
  color: #909399;
  font-size: 14px;
}

.course-section,
.paper-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.section-title .el-icon {
  color: #409eff;
  font-size: 20px;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.course-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.credit {
  color: #909399;
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #66b1ff;
}

.current-course {
  font-size: 16px;
  color: #303133;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.excellent { color: #67c23a; font-weight: bold; }
.good { color: #409eff; font-weight: bold; }
.pass { color: #e6a23c; }
.fail { color: #f56c6c; }
.max-score { color: #909399; font-size: 12px; }



.grade-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
}

.paper-info {
  display: flex;
  gap: 30px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-item .label {
  color: #909399;
  font-size: 14px;
}

.info-item .value {
  color: #303133;
  font-weight: 500;
}

.info-item .value.score {
  color: #409eff;
  font-size: 18px;
  font-weight: bold;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  background: #fff;
}

.question-item.objective {
  background: #f0f9ff;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.question-no {
  font-weight: 600;
  color: #303133;
}

.question-score {
  margin-left: auto;
  color: #909399;
  font-size: 14px;
}

.question-content {
  margin-bottom: 15px;
}

.question-text {
  font-size: 15px;
  color: #303133;
  margin-bottom: 10px;
  line-height: 1.6;
}

.reference-answer,
.student-answer,
.correct-answer {
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.reference-answer {
  background: #f0f9ff;
}

.reference-answer .label,
.student-answer .label,
.correct-answer .label {
  font-weight: 500;
  color: #606266;
  display: block;
  margin-bottom: 5px;
}

.student-answer {
  background: #f5f7fa;
}

.student-answer .content.empty {
  color: #909399;
  font-style: italic;
}

.correct-answer {
  background: #f0f9eb;
}

.correct-answer .content {
  color: #67c23a;
  font-weight: 500;
}

.grading-area {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding-top: 10px;
  border-top: 1px dashed #dcdfe6;
}

.ai-score {
  background: #ecf5ff;
  padding: 8px 12px;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.ai-score .label,
.final-score .label {
  color: #606266;
  font-size: 14px;
}

.ai-score .score {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;
}

.ai-score .similarity {
  color: #909399;
  font-size: 12px;
}

.final-score .score {
  color: #67c23a;
  font-weight: bold;
  font-size: 18px;
}

.final-score .auto-graded {
  color: #909399;
  font-size: 12px;
}

.final-score .auto-graded-hint {
  color: #67c23a;
  font-size: 12px;
  margin-left: 10px;
}

.final-score .manual-grade-hint {
  color: #e6a23c;
  font-size: 12px;
  margin-left: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-top: 1px solid #e4e7ed;
}

.total-score-preview {
  font-size: 16px;
  color: #303133;
}

.total-score-preview .score {
  color: #409eff;
  font-weight: bold;
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
