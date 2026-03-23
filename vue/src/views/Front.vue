<template>
  <div>
    <div class="front-notice"><el-icon><Bell /> </el-icon> 公告:{{data.top}}</div>
    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/img/logo.png" alt="">
        <div class="title">在线考试系统学生端</div>
      </div>
      <div class="front-header-center">
        <el-menu :default-active="router.currentRoute.value.path" router mode="horizontal">
          <el-menu-item index="/front/home">首页</el-menu-item>
          <el-sub-menu index="/front/practice">
            <template #title>
              <span class="menu-title">模拟练习</span>
            </template>
            <el-menu-item index="/front/practice">刷题中心</el-menu-item>
            <el-menu-item index="/front/exam-practice">模拟考试</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/front/course">课程中心</el-menu-item>
          <el-menu-item index="/front/myCourse">我的课程</el-menu-item>
          <el-menu-item index="/front/examList">考试列表</el-menu-item>
          <el-menu-item index="/front/myScore">我的成绩</el-menu-item>
          <el-menu-item index="/front/ai">AI助手</el-menu-item>
          <el-menu-item index="/front/person">个人中心</el-menu-item>
        </el-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id">
          <el-button @click="router.push('/login')" type="primary">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
        <div v-else style="display: flex; align-items: center; gap: 10px;">
          <div style="cursor: pointer; height: 60px; display: flex; align-items: center;" @click="router.push('/front/person')">
            <img style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
            <span style="margin-left: 5px; color: #fff;">{{data.user.name}}</span>
          </div>
          <el-button size="small" @click="logout">退出登录</el-button>
        </div>
      </div>
    </div>
    <div class="main-body">
      <RouterView @updateUser="updateUser"/>
    </div>
  </div>
</template>

<script setup>
import router from "@/router/index.js";
import { reactive } from "vue";
import request from "@/utils/request.js";
import { getCurrentUser, clearCurrentUser, updateCurrentUser } from '@/utils/userStorage.js'


const logout = ()=>{
  clearCurrentUser()
  router.push('/login')
}

const data = reactive({
  user: getCurrentUser(),
  top:'',
  noticeData:[]
})

const loadNotice = ()=>{
  request.get('/notice/selectAll').then(res=>{
    data.noticeData = res.data
    let i = 0
    if(data.noticeData && data.noticeData.length){
      data.top = data.noticeData[i].title
      //定时任务，定时轮播公告
      setInterval(()=>{
        i++
        if(i >= data.noticeData.length){
          i=0
        }
        data.top = data.noticeData[i].title
      }, 2500)
    }
  })
}
loadNotice()

//是上面routerview的监听事件，监听到数据更新时，重新获取用户信息
const updateUser = ()=>{
  const newUser = getCurrentUser()
  Object.assign(data.user, newUser)
}
</script>

<style scoped>
@import "@/assets/css/front.css";
</style>