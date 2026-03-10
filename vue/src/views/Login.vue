<template>
  <div class="login-container">
    <div class="login-left">
      <div class="left-content">
        <div class="logo">
          <el-icon><School /></el-icon>
        </div>
        <h1>在线考试系统</h1>
        <p>高效 · 便捷 · 智能</p>
        <div class="decoration">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
    
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>请使用您的账号密码登录</p>
        </div>
        
        <el-form ref="formRef" :model="data.form" :rules="data.rules">
          <el-form-item prop="username">
            <el-input 
              :prefix-icon="User" 
              v-model="data.form.username" 
              placeholder="请输入账号"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input 
              show-password 
              :prefix-icon="Lock" 
              v-model="data.form.password" 
              placeholder="请输入密码"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="role">
            <el-select v-model="data.form.role" placeholder="请选择角色" size="large" style="width: 100%">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" @click="login">
              登 录
            </el-button>
          </el-form-item>
          
          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue"
import { Lock, User, School } from "@element-plus/icons-vue"
import request from "@/utils/request.js"
import { ElMessage } from "element-plus"
import { useRouter } from 'vue-router'
import { getCurrentUser, setCurrentUser } from '@/utils/userStorage.js'

const router = useRouter()

const data = reactive({
  form: {},
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
})

const formRef = ref()

onMounted(() => {
  const user = getCurrentUser()
  if (user && user.id) {
    if (user.role === 'STUDENT') {
      router.push('/front/home')
    } else if (user.role === 'TEACHER') {
      router.push('/teacher/home')
    } else if (user.role === 'ADMIN') {
      router.push('/manager/home')
    }
  }
})

const login = () => {
  formRef.value.validate(valid => {
    if (valid) {
      request.post('/login', data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('登录成功')
          setCurrentUser(res.data)
          if (res.data.role === 'STUDENT') {
            router.push('/front/home')
          } else if (res.data.role === 'TEACHER') {
            router.push('/teacher/home')
          } else {
            router.push('/manager/home')
          }
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
}

.login-left {
  flex: 1;
  background-image: url('@/assets/img/在线考试系统登录背景1.png');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-left::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
}

.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.03) 0%, transparent 50%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.left-content {
  text-align: center;
  color: white;
  position: relative;
  z-index: 1;
}

.logo {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  font-size: 40px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.left-content h1 {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.left-content p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 4px;
}

.decoration {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.decoration span {
  width: 40px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.decoration span:nth-child(1) { width: 60px; }
.decoration span:nth-child(2) { width: 40px; }
.decoration span:nth-child(3) { width: 20px; }

.login-right {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-box {
  width: 360px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #999;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}

.login-footer a {
  color: #333;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 500;
}

.login-footer a:hover {
  color: #000;
  text-decoration: underline;
}

@media (max-width: 900px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    width: 100%;
  }
}
</style>
