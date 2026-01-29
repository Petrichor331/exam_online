<template>
  <div style="width: 50%" class="card">
    <el-form ref="user" :model="data.user" label-width="70px" style="padding: 20px">
      <el-form-item prop="username" label="用户名">
        <el-input disabled="true" v-model="data.user.username" placeholder="请输入用户名"></el-input>
      </el-form-item>
      <el-form-item prop="avatar" label="头像">
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
      <div style="text-align: center">
        <el-button type="primary" @click="update">保存</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const baseUrl = import.meta.env.VITE_BASE_URL

const emit = defineEmits(['updateUser'])
const update = ()=>{
  if(data.user.role === 'ADMIN'){
    request.put('/admin/update', data.user).then(res=>{
      if(res.code === '200'){
        ElMessage.success('保存成功')
        console.log('update 返回的 user:', res.data)
        localStorage.setItem('xm-user', JSON.stringify(data.user))
        emit('updateUser')
        router.push('/manager/home')
      }else{
        ElMessage.error(res.msg)
      }
    })
  }
}

const data = reactive({
  user:JSON.parse(localStorage.getItem('xm-user') || '{}')
})

const handleFileUpload = (res) => {
  data.user.avatar = res.data
}

</script>




<style>

.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  display: block;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
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
</style>