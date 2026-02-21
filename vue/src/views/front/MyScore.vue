<template>
  <div class="score-container">
    <div class="page-header">
      <h2>我的成绩</h2>
      <p class="subtitle">查看考试记录和成绩详情</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e3f2fd;">
              <el-icon style="color: #2196f3;"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalExams }}</div>
              <div class="stat-label">考试次数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e8f5e9;">
              <el-icon style="color: #4caf50;"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.avgScore }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fff3e0;">
              <el-icon style="color: #ff9800;"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.maxScore }}</div>
              <div class="stat-label">最高分</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: #fce4ec;">
              <el-icon style="color: #e91e63;"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.passRate }}%</div>
              <div class="stat-label">及格率</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 最近考试 -->
    <div class="section" v-if="recentScores.length > 0">
      <div class="section-title">
        <el-icon><Timer /></el-icon>
        <span>最近考试</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="8" v-for="exam in recentScores" :key="exam.scoreId">
          <el-card class="exam-card" shadow="hover" @click="viewDetail(exam)">
            <div class="exam-header">
              <span class="exam-name">{{ exam.examName }}</span>
              <el-tag :type="getStatusType(exam.status)" size="small">
                {{ getStatusText(exam.status) }}
              </el-tag>
            </div>
            <div class="exam-info">
              <div class="info-row">
                <span class="label">考试时间：</span>
                <span class="value">{{ exam.submitTime || '未提交' }}</span>
              </div>
              <div class="info-row">
                <span class="label">得分：</span>
                <span class="score" :class="getScoreClass(exam.score, exam.totalScore)">
                  {{ exam.score !== undefined && exam.score !== null ? exam.score : '-' }}
                </span>
                <span v-if="exam.totalScore" class="total">/{{ exam.totalScore }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 成绩列表 -->
    <div class="section">
      <div class="section-title">
        <el-icon><List /></el-icon>
        <span>成绩列表</span>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="考试状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="已完成" value="finished" />
          <el-option label="评分中" value="grading" />
        </el-select>
        <el-input 
          v-model="searchExam" 
          placeholder="搜索考试名称" 
          clearable 
          style="width: 200px; margin-left: 10px"
        />
        <el-button type="primary" @click="loadScoreList" style="margin-left: 10px">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <!-- 成绩表格 -->
      <el-table :data="filteredScoreList" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="80" align="center" />
        <el-table-column prop="examName" label="考试名称" min-width="200">
          <template #default="{ row }">
            <span class="exam-name-text">{{ row.examName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column prop="score" label="得分" width="120" align="center">
          <template #default="{ row }">
            <span :class="['score-text', getScoreClass(row.score, row.totalScore)]">
              {{ row.score !== undefined && row.score !== null ? row.score : '-' }}
            </span>
            <span v-if="row.totalScore" class="total-text">/{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="viewDetail(row)"
              :disabled="row.status === 'ongoing'"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!filteredScoreList.length" description="暂无考试记录">
        <template #description>
          <p>暂无考试记录</p>
          <p class="empty-tip">快去选课并参加考试吧！</p>
        </template>
      </el-empty>
    </div>

    <!-- 成绩详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="成绩详情"
      width="800px"
      class="detail-dialog"
    >
      <div v-if="currentDetail" class="detail-content">
        <!-- 考试信息 -->
        <div class="detail-header">
          <div class="exam-title">{{ currentDetail.paperName }}</div>
          <div class="exam-meta">
            <span class="course">{{ currentDetail.courseName }}</span>
            <span class="time">提交时间：{{ currentDetail.submitTime }}</span>
          </div>
        </div>

        <!-- 总分卡片 -->
        <div class="total-score-card">
          <div class="score-circle" :class="getTotalScoreClass(currentDetail.totalScore, currentDetail.maxScore)">
            <div class="score-number">{{ currentDetail.totalScore || 0 }}</div>
            <div class="score-total">/{{ currentDetail.maxScore || 100 }}</div>
          </div>
          <div class="score-info">
            <div class="score-label">考试得分</div>
            <div class="score-desc">{{ getScoreDescription(currentDetail.totalScore, currentDetail.maxScore) }}</div>
          </div>
        </div>

        <!-- 题目详情 -->
        <div class="question-detail-list">
          <div 
            v-for="(answer, index) in currentDetail.answers" 
            :key="answer.answerId"
            class="question-detail-item"
            :class="{ 
              'correct': isCorrect(answer),
              'wrong': !isCorrect(answer) && answer.finalScore !== null,
              'pending': answer.finalScore === null
            }"
          >
            <div class="question-header">
              <div class="left">
                <span class="no">{{ index + 1 }}</span>
                <el-tag size="small" :type="getQuestionTypeColor(answer.questionTypeId)">
                  {{ getQuestionTypeName(answer.questionTypeId) }}
                </el-tag>
              </div>
              <div class="right">
                <span class="score">{{ answer.finalScore || answer.aiScore || 0 }}/{{ answer.maxScore }}分</span>
                <el-icon v-if="isCorrect(answer)" class="status-icon correct"><CircleCheck /></el-icon>
                <el-icon v-else-if="answer.finalScore !== null" class="status-icon wrong"><CircleClose /></el-icon>
                <el-icon v-else class="status-icon pending"><Timer /></el-icon>
              </div>
            </div>

            <div class="question-body">
              <div class="question-text">{{ answer.questionName }}</div>
              
              <!-- 学生答案 -->
              <div class="answer-row">
                <span class="label">你的答案：</span>
                <span class="value student" :class="{ 'empty': !answer.studentAnswer }">
                  {{ answer.studentAnswer || '未作答' }}
                </span>
              </div>

              <!-- 正确答案（选择题） -->
              <div v-if="answer.correctAnswer" class="answer-row">
                <span class="label">正确答案：</span>
                <span class="value correct">{{ answer.correctAnswer }}</span>
              </div>

              <!-- 参考答案（主观题） -->
              <div v-if="answer.referenceAnswer" class="answer-row">
                <span class="label">参考答案：</span>
                <span class="value reference">{{ answer.referenceAnswer }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from "vue";
import request from "@/utils/request.js";
import { getCurrentUser } from "@/utils/userStorage.js";
import { ElMessage } from "element-plus";
import { 
  Document, 
  TrendCharts, 
  Trophy, 
  CircleCheck, 
  Timer, 
  List, 
  Search,
  CircleClose
} from "@element-plus/icons-vue";

const data = reactive({
  user: getCurrentUser(),
});

// 统计数据
const stats = reactive({
  totalExams: 0,
  avgScore: '-',
  maxScore: '-',
  passRate: '-'
});

// 最近考试
const recentScores = ref([]);

// 成绩列表
const scoreList = ref([]);
const filterStatus = ref('');
const searchExam = ref('');

// 详情弹窗
const detailDialogVisible = ref(false);
const currentDetail = ref(null);

// 过滤后的成绩列表
const filteredScoreList = computed(() => {
  return scoreList.value.filter(score => {
    const matchStatus = !filterStatus.value || score.status === filterStatus.value;
    const matchName = !searchExam.value || 
      (score.examName && score.examName.includes(searchExam.value));
    return matchStatus && matchName;
  });
});

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'finished': 'success',
    'grading': 'warning',
    'ai_graded': 'primary',
    'ongoing': 'info'
  };
  return types[status] || 'info';
};

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    'finished': '已完成',
    'grading': '评分中',
    'ai_graded': 'AI已评分',
    'ongoing': '进行中'
  };
  return texts[status] || status;
};

// 获取分数样式类
const getScoreClass = (score, maxScore) => {
  if (score === undefined || score === null) return '';
  const ratio = score / (maxScore || 100);
  if (ratio >= 0.9) return 'excellent';
  if (ratio >= 0.8) return 'good';
  if (ratio >= 0.6) return 'pass';
  return 'fail';
};

// 获取总分样式类
const getTotalScoreClass = (score, maxScore) => {
  if (score === undefined || score === null) return '';
  const ratio = score / (maxScore || 100);
  if (ratio >= 0.9) return 'excellent';
  if (ratio >= 0.8) return 'good';
  if (ratio >= 0.6) return 'pass';
  return 'fail';
};

// 获取分数描述
const getScoreDescription = (score, maxScore) => {
  if (score === undefined || score === null) return '暂无成绩';
  const ratio = score / (maxScore || 100);
  if (ratio >= 0.9) return '优秀！继续保持！';
  if (ratio >= 0.8) return '良好，还有进步空间';
  if (ratio >= 0.6) return '及格，需要继续努力';
  return '不及格，请认真复习';
};

// 获取题型名称
const getQuestionTypeName = (typeId) => {
  const names = {
    1: '单选',
    2: '多选',
    3: '判断',
    4: '填空',
    5: '简答'
  };
  return names[typeId] || '未知';
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

// 判断是否正确
const isCorrect = (answer) => {
  return answer.finalScore !== null && answer.finalScore === answer.maxScore;
};

// 加载统计数据
const loadStats = () => {
  // 从成绩列表计算统计
  if (scoreList.value.length === 0) return;
  
  const finishedScores = scoreList.value.filter(s => s.score !== undefined && s.score !== null);
  stats.totalExams = scoreList.value.length;
  
  if (finishedScores.length > 0) {
    const scores = finishedScores.map(s => s.score);
    const total = scores.reduce((a, b) => a + b, 0);
    const max = Math.max(...scores);
    const passCount = scores.filter(s => s >= 60).length;
    
    stats.avgScore = (total / scores.length).toFixed(1);
    stats.maxScore = max;
    stats.passRate = ((passCount / scores.length) * 100).toFixed(0);
  }
};

// 加载成绩列表
const loadScoreList = () => {
  request.get('/score/myScores', {
    params: { studentId: data.user.id }
  }).then(res => {
    if (res.code === '200') {
      scoreList.value = res.data || [];
      recentScores.value = scoreList.value.slice(0, 3);
      loadStats();
    } else {
      ElMessage.error(res.msg || '加载成绩失败');
    }
  }).catch(err => {
    console.error('加载成绩失败:', err);
    // 使用模拟数据演示
    useMockData();
  });
};

// 模拟数据（用于演示）
const useMockData = () => {
  scoreList.value = [
    {
      scoreId: 1,
      examName: 'Java程序设计期中考试',
      submitTime: '2024-03-15 14:30:00',
      score: 85,
      totalScore: 100,
      status: 'finished'
    },
    {
      scoreId: 2,
      examName: '数据库原理期末考试',
      submitTime: '2024-03-10 16:00:00',
      score: 92,
      totalScore: 100,
      status: 'finished'
    }
  ];
  recentScores.value = scoreList.value.slice(0, 3);
  loadStats();
};

// 查看详情
const viewDetail = (row) => {
  request.get(`/score/detail/${row.scoreId}`).then(res => {
    if (res.code === '200') {
      currentDetail.value = res.data;
      detailDialogVisible.value = true;
    } else {
      ElMessage.error(res.msg || '加载详情失败');
    }
  }).catch(err => {
    ElMessage.error('网络错误，请稍后重试');
  });
};

// 初始化
loadScoreList();
</script>

<style scoped>
.score-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  color: #909399;
  font-size: 14px;
}

.stats-cards {
  margin-bottom: 30px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
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

.section-title .el-icon {
  color: #409eff;
  font-size: 20px;
}

.exam-card {
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.exam-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.exam-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.exam-info .info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.exam-info .label {
  color: #909399;
  font-size: 13px;
  width: 70px;
}

.exam-info .value {
  color: #606266;
  font-size: 13px;
}

.exam-info .score {
  font-size: 20px;
  font-weight: bold;
}

.exam-info .score.excellent { color: #67c23a; }
.exam-info .score.good { color: #409eff; }
.exam-info .score.pass { color: #e6a23c; }
.exam-info .score.fail { color: #f56c6c; }

.exam-info .total {
  color: #909399;
  font-size: 14px;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.exam-name-text {
  font-weight: 500;
  color: #303133;
}

.score-text {
  font-size: 16px;
  font-weight: bold;
}

.score-text.excellent { color: #67c23a; }
.score-text.good { color: #409eff; }
.score-text.pass { color: #e6a23c; }
.score-text.fail { color: #f56c6c; }

.total-text {
  color: #909399;
  font-size: 14px;
}

.empty-tip {
  color: #909399;
  font-size: 13px;
  margin-top: 5px;
}

/* 详情弹窗样式 */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 30px;
  text-align: center;
}

.exam-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
}

.exam-meta {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 14px;
  opacity: 0.9;
}

.total-score-card {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  background: #f5f7fa;
  gap: 30px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 4px solid;
  background: #fff;
}

.score-circle.excellent { border-color: #67c23a; color: #67c23a; }
.score-circle.good { border-color: #409eff; color: #409eff; }
.score-circle.pass { border-color: #e6a23c; color: #e6a23c; }
.score-circle.fail { border-color: #f56c6c; color: #f56c6c; }

.score-number {
  font-size: 36px;
  font-weight: bold;
  line-height: 1;
}

.score-total {
  font-size: 16px;
  margin-top: 5px;
}

.score-info {
  text-align: center;
}

.score-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.score-desc {
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.question-detail-list {
  padding: 20px;
}

.question-detail-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 15px;
  overflow: hidden;
  transition: all 0.3s;
}

.question-detail-item.correct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.question-detail-item.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}

.question-detail-item.pending {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.question-header .left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-header .no {
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.question-header .right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-header .score {
  font-weight: bold;
  color: #303133;
}

.status-icon {
  font-size: 20px;
}

.status-icon.correct {
  color: #67c23a;
}

.status-icon.wrong {
  color: #f56c6c;
}

.status-icon.pending {
  color: #e6a23c;
}

.question-body {
  padding: 15px;
  background: #fff;
}

.question-text {
  font-size: 15px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.6;
}

.answer-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.answer-row .label {
  color: #909399;
  font-size: 13px;
  width: 80px;
  flex-shrink: 0;
}

.answer-row .value {
  flex: 1;
  color: #303133;
  font-size: 14px;
}

.answer-row .value.student {
  color: #606266;
}

.answer-row .value.student.empty {
  color: #909399;
  font-style: italic;
}

.answer-row .value.correct {
  color: #67c23a;
  font-weight: 500;
}

.answer-row .value.reference {
  color: #409eff;
}
</style>
