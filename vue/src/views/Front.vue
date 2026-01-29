<template>
  <div>
    <div class="front-notice"><el-icon><Bell /> </el-icon> 公告:{{data.top}}</div>
    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/img/logo.png" alt="">
        <div class="title">项目前台</div>
      </div>
      <div class="front-header-center">
        <el-menu :default-active="router.currentRoute.value.path" router mode="horizontal">
          <el-menu-item index="/front/home">首页</el-menu-item>
          <el-menu-item index="/front/person">个人中心</el-menu-item>
        </el-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id">
          <el-button @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
        <div v-else>
          <el-dropdown style="cursor: pointer; height: 60px">
            <div style="display: flex; align-items: center">
              <img style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
              <span style="margin-left: 5px;">{{data.user.name}}</span><el-icon><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click=logout>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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


const logout = ()=>{
  localStorage.removeItem('xm-user')
  router.push('/login')
}

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
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
  const newUser = JSON.parse(localStorage.getItem('xm-user') || '{}')
  Object.assign(data.user, newUser)
}
</script>

<style scoped>
@import "@/assets/css/front.css";
</style>