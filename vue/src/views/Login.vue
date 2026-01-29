
<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size:24px; text-align: center; margin-bottom: 30px; color: #47afdc; letter-spacing: 5px">欢迎登录</div>
      <el-form ref="formRef" :model="data.form" :rules="data.rules">
        <el-form-item prop="username" style="margin-bottom: 20px">
          <el-input :prefix-icon ="User" v-model="data.form.username" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item prop="password" style="margin-bottom: 20px">
          <el-input show-password :prefix-icon ="Lock" v-model="data.form.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item prop="role" style="margin-bottom: 35px">
          <el-select v-model="data.form.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="学生" value="STUDENT"></el-option>
            <el-option label="教师" value="TEACHER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="large" type="primary" style="width: 100% " @click="login">登录</el-button>
        </el-form-item>
        <div style="text-align: right">还没有账号?请<a href="/register">前往注册</a> </div>

      </el-form>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from "vue";
import {Lock, User} from "@element-plus/icons-vue";
import axios from "axios";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const data = reactive({
  form: {},
  rules:{
    username:[{required:true, message:'请输入账号', trigger:'blur'}],
    password:[{required:true, message:'请输入密码', trigger:'blur'}],
  }
})

const formRef = ref()
const login = () => {
  formRef.value.validate(valid =>{
    //表示表单校验通过了，才会登录成功
    if(valid){
      request.post('/login',data.form).then(res =>{
        if(res.code === '200'){
          ElMessage.success('登录成功')
          /*登录成功后要存储信息到浏览器缓存*/
          localStorage.setItem('xm-user', JSON.stringify(res.data))
          router.push('/manager/home')
        }else{
          ElMessage.error(res.msg)
        }

      })
    }
  })
}
</script>


<style scoped>
.login-container{
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background:linear-gradient(to top, #ffffff,#47afdc,#e4e7ed);
}

.login-box{
  width: 350px;
  height: 340px;
  padding: 30px;
  border-radius:5px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
  background-color: rgba(255,255,255,0.7);
}
</style>