<template>
  <div>
    <div class="card">欢迎使用本系统😁{{data.user?.name}}</div>

    <div class="notice-section">
      <div class="notice-title">
        <el-icon><Bell /></el-icon>
        <span>最新公告</span>
      </div>

      <el-timeline class="timeline-wrapper">
        <el-timeline-item
            v-for="(item, index) in data.noticeData"
            :key="item.id"
            :timestamp="item.time"
            placement="top"
            :type="index === 0 ? 'primary' : ''"
            :hollow="index !== 0"
        >
          <el-card class="notice-card" :class="{ 'latest': index === 0 }">
            <div class="notice-header">{{ item.title }}</div>
            <div class="notice-body">{{ item.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-if="!data.noticeData.length" description="暂无公告" />
    </div>
  </div>
</template>

<script setup>
import request from "@/utils/request.js";
import {reactive} from "vue";
import {ElMessage} from "element-plus";
import { Bell } from "@element-plus/icons-vue";

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  noticeData: [],
})

const loadNotice = () =>{
  request.get('/notice/selectAll').then(res =>{
    if(res.code === '200'){
      data.noticeData = res.data
    }else{
      ElMessage.error(res.msg)
    }
  })
}

loadNotice()

</script>

<style scoped>


.card {
  padding: 20px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  font-size: 18px;
  color: #303133;
}

.notice-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.notice-title {
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

.notice-title .el-icon {
  color: #a2d2ff;
  font-size: 20px;
}

.timeline-wrapper {
  max-width: 100%;
}

.notice-card {
  transition: all 0.3s;
}

.notice-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.notice-card.latest {
  border-left: 4px solid #89cff04d;
}

.notice-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.notice-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>