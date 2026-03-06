<template>
  <div class="profile-page">
    <div class="sidebar">
      <div class="menu-item" @click="activeMenu='profile'">个人信息</div>
    </div>
    <div class="content">
      <div v-if="activeMenu==='profile'">
        <div class="card user-card">
          <el-form ref="user" :model="data.user" label-width="70px" style="padding: 20px">
            <div style="text-align: center; margin-bottom: 20px" class="avatar-wrapper" >
              <el-upload
                  :action="baseUrl + '/files/upload'"
                  :headers="{token:data.user?.token || ''}"
                  :on-success="handleFileUpload"
                  show-file-list="false"
                  list-type="picture"
                  class="avatar-uploader"
              >
                <img v-if="data.user.avatar" :src="data.user.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </div>
            <el-form-item prop="username" label="用户名">
              <el-input disabled="true" v-model="data.user.username" placeholder="请输入用户名"></el-input>
            </el-form-item>

            <el-form-item prop="name" label="姓名">
              <el-input  v-model="data.user.name" placeholder="请输入姓名"></el-input>
            </el-form-item>
            <el-form-item prop="phone" label="电话">
              <el-input v-model="data.user.phone" placeholder="请输入电话"></el-input>
            </el-form-item>
            <el-form-item prop="email" label="邮箱">
              <el-input v-model="data.user.email" placeholder="请输入邮箱"></el-input>
            </el-form-item>
            <div style="text-align: center" class="action-bar">
              <el-button type="primary" @click="update">保存</el-button>
              <el-button  @click="router.push('/front/home')">取消</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
  <div>hhh</div>
</template>

<script setup>
import {reactive, ref} from "vue";
import request from "@/utils/request.js";
import { getCurrentUser, setCurrentUser } from "@/utils/userStorage.js";
import {ElMessage} from "element-plus";
import {useRouter} from "vue-router";
const router = useRouter()
const baseUrl = import.meta.env.VITE_BASE_URL


const activeMenu = ref('profile')
const emit = defineEmits(['updateUser'])
const update = ()=>{
  if(data.user.role === 'ADMIN'){
    request.put('/admin/update', data.user).then(res=>{
      if(res.code === '200'){
        ElMessage.success('保存成功')
        console.log('update 返回的 user:', res.data)
        setCurrentUser(data.user)
        emit('updateUser')
        router.push('/front/home')
      }else{
        ElMessage.error(res.msg)
      }
    })
  }
  if(data.user.role === 'TEACHER'){
    request.put('/teacher/update', data.user).then(res=>{
      if(res.code === '200'){
        ElMessage.success('保存成功')
        console.log('update 返回的 user:', res.data)
        setCurrentUser(data.user)
        emit('updateUser')
        router.push('/front/home')
      }else{
        ElMessage.error(res.msg)
      }
    })
  }
  if(data.user.role === 'STUDENT'){
    request.put('/student/update', data.user).then(res=>{
      if(res.code === '200'){
        ElMessage.success('保存成功')
        console.log('update 返回的 user:', res.data)
        setCurrentUser(data.user)
        emit('updateUser')
        router.push('/front/home')
      }else{
        ElMessage.error(res.msg)
      }
    })
  }


}

const data = reactive({
  user: getCurrentUser()
})

const handleFileUpload = (res) => {
  data.user.avatar = res.data
}

</script>

<style>
.profile-page{
  display: flex;
  min-height: 100vh;
  padding: 40px;

}
.sidebar{
  width: 200px;
  background: #ffffff;
  border-right: 10px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}
.action-bar{
  text-align: center;
  padding-top: 10px;
}

.el-form-item{
  margin-bottom: 25px;
}

.avatar-wrapper{
  text-align: center;
  padding: 30px 0 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}
.content{
  flex: 1;
}

.avatar-uploader .avatar {
  width: 135px;
  height: 135px;
  display: block;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 70%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
}

.user-card{
  width: 100%;
  max-width: 800px;
  margin: 40px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
</style>