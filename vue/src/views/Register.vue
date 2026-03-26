<template>
  <div class="login-container">
    <div class="login-left">
      <div class="left-content">
        <div class="logo">
          <el-icon><School /></el-icon>
        </div>
        <h1>在线考试系统</h1>
        <p>加入我们，开启学习之旅</p>
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
          <h2>欢迎注册</h2>
          <p>{{ stepTitle }}</p>
        </div>
        
        <!-- 步骤指示器 -->
        <div class="step-indicator">
          <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
            <div class="step-number">1</div>
            <div class="step-label">邮箱验证</div>
          </div>
          <div class="step-line" :class="{ active: currentStep > 1 }"></div>
          <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
            <div class="step-number">2</div>
            <div class="step-label">账号设置</div>
          </div>
          <div class="step-line" :class="{ active: currentStep > 2 }"></div>
          <div class="step" :class="{ active: currentStep >= 3 }">
            <div class="step-number">3</div>
            <div class="step-label">完善信息</div>
          </div>
        </div>
        
        <!-- 第一步：邮箱验证 -->
        <div v-if="currentStep === 1">
          <el-form ref="formRef1" :model="step1.form" :rules="step1.rules">
            <el-form-item prop="email">
              <el-input 
                :prefix-icon="Message" 
                v-model="step1.form.email" 
                placeholder="请输入邮箱"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="captchaCode">
              <div class="captcha-wrapper">
                <el-input 
                  v-model="step1.form.captchaCode" 
                  placeholder="请输入图形验证码"
                  size="large"
                  style="flex: 1"
                />
                <img 
                  class="captcha-code" 
                  :src="captchaImage" 
                  @click="refreshCaptcha" 
                  title="点击刷新"
                />
              </div>
            </el-form-item>
            
            <el-form-item prop="emailCaptcha">
              <div class="captcha-row">
                <el-input 
                  v-model="step1.form.emailCaptcha" 
                  placeholder="请输入验证码"
                  size="large"
                  style="flex: 1"
                />
                <el-button 
                  v-if="!sendingCaptcha" 
                  :disabled="!step1.form.email || sendingCaptcha" 
                  size="small" 
                  @click="sendEmailCaptcha"
                  class="captcha-btn"
                >
                  {{ captchaButtonText }}
                </el-button>
                <el-button 
                  v-else 
                  size="small" 
                  disabled
                  class="captcha-btn sending"
                >
                  {{ captchaButtonText }}
                </el-button>
              </div>
            </el-form-item>
            
            <el-form-item prop="role">
              <el-select v-model="step1.form.role" placeholder="请选择角色" size="large" style="width: 100%">
                <el-option label="学生" value="STUDENT" />
                <el-option label="教师" value="TEACHER" />
              </el-select>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" size="large" style="width: 100%" @click="nextStep1" :loading="loading">
                下一步
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 第二步：设置用户名和密码 -->
        <div v-if="currentStep === 2">
          <el-form ref="formRef2" :model="step2.form" :rules="step2.rules">
            <el-form-item prop="username">
              <el-input
                :prefix-icon="User"
                autocomplete="off"
                v-model="step2.form.username" 
                placeholder="请输入用户名（唯一）"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input 
                show-password
                autocomplete="new-password"
                :prefix-icon="Lock" 
                v-model="step2.form.password" 
                placeholder="请输入密码"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="confirmPassword">
              <el-input 
                show-password 
                :prefix-icon="Lock" 
                v-model="step2.form.confirmPassword" 
                placeholder="请确认密码"
                size="large"
              />
            </el-form-item>
            
            <el-form-item>
              <div class="button-row">
                <el-button size="large" @click="prevStep">上一步</el-button>
                <el-button type="primary" size="large" @click="nextStep2" :loading="loading">下一步</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 第三步：完善用户信息 -->
        <div v-if="currentStep === 3">
          <el-form ref="formRef3" :model="step3.form" :rules="step3.rules">
            <el-form-item prop="name">
              <el-input 
                :prefix-icon="User" 
                v-model="step3.form.name" 
                placeholder="请输入昵称"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="phone">
              <el-input 
                :prefix-icon="Phone" 
                v-model="step3.form.phone" 
                placeholder="请输入手机号（可选）"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="avatar">
              <div class="avatar-upload">
                <el-upload
                  class="avatar-uploader"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
                  action="/files/upload"
                >
                  <img v-if="step3.form.avatar" :src="step3.form.avatar" class="avatar" />
                  <div v-else class="avatar-placeholder">
                    <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                    <span>上传头像</span>
                  </div>
                </el-upload>
                <div class="avatar-tip">点击上传头像（可选）</div>
              </div>
            </el-form-item>
            
            <el-form-item>
              <div class="button-row">
                <el-button size="large" @click="prevStep">上一步</el-button>
                <el-button type="primary" size="large" @click="completeRegister" :loading="loading">完成注册</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
        
        <div class="login-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from "vue"
import { Lock, User, School, Message, Phone, Plus } from "@element-plus/icons-vue"
import request from "@/utils/request.js"
import { ElMessage } from "element-plus"
import router from "@/router/index.js"
import { getCurrentUser } from '@/utils/userStorage.js'

const currentStep = ref(1)
const accountId = ref(null)
const loading = ref(false)
const sendingCaptcha = ref(false)
const captchaButtonText = ref('发送验证码')
const captchaCode = ref('')
const captchaId = ref('')
const captchaImage = ref('')

const stepTitle = computed(() => {
  switch(currentStep.value) {
    case 1: return '第一步：邮箱验证'
    case 2: return '第二步：设置用户名和密码'
    case 3: return '第三步：完善个人信息'
    default: return ''
  }
})

// 第一步：邮箱验证
const step1 = reactive({
  form: {
    email: '',
    emailCaptcha: '',
    captchaCode: '',
    role: 'STUDENT'
  },
  rules: {
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
    ],
    captchaCode: [
      { required: true, message: '请输入图形验证码', trigger: 'blur' },
      { len: 4, message: '图形验证码必须为4位', trigger: 'blur' }
    ],
    emailCaptcha: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 6, message: '验证码必须为6位', trigger: 'blur' }
    ],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }]
  }
})

// 第二步：设置用户名和密码
const step2 = reactive({
  form: {
    username: '',
    password: '',
    confirmPassword: ''
  },
  rules: {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { validator: validatePassword, trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      { validator: validatePass, trigger: 'blur' }
    ]
  }
})

// 第三步：完善信息
const step3 = reactive({
  form: {
    name: '',
    phone: '',
    avatar: ''
  },
  rules: {
    name: [
      { required: true, message: '请输入昵称', trigger: 'blur' },
      { min: 2, max: 20, message: '昵称长度在2到20个字符', trigger: 'blur' }
    ],
    phone: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
    ]
  }
})

function validatePassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入密码'))
  } else {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/
    if (!regex.test(value)) {
      callback(new Error('密码必须包含大小写字母和数字，且至少8位'))
    }
    callback()
  }
}

function validatePass(rule, value, callback) {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== step2.form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRef1 = ref()
const formRef2 = ref()
const formRef3 = ref()

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
  refreshCaptcha()
})

const refreshCaptcha = () => {
  request.get('/captcha').then(res => {
    if (res.code === '200') {
      captchaId.value = res.data.captchaId
      captchaImage.value = res.data.captchaImage
    }
  })
}

const sendEmailCaptcha = () => {
  const email = step1.form.email
  if (!email) {
    ElMessage.error('请输入邮箱')
    return
  }
  if (!step1.form.captchaCode) {
    ElMessage.error('请输入图形验证码')
    return
  }
  
  sendingCaptcha.value = true
  captchaButtonText.value = '发送中...'
  
  request.post('/sendEmailCaptcha', { 
    email,
    captchaId: captchaId.value,
    captchaCode: step1.form.captchaCode
  })
    .then(res => {
      if (res.code === '200') {
        ElMessage.success('验证码已发送，请查收')
        refreshCaptcha()
        // 开始倒计时
        let timeLeft = 60
        const interval = setInterval(() => {
          timeLeft--
          captchaButtonText.value = `${timeLeft}s后重发`
          if (timeLeft <= 0) {
            clearInterval(interval)
            sendingCaptcha.value = false
            captchaButtonText.value = '发送验证码'
          }
        }, 1000)
      } else {
        ElMessage.error(res.msg || '验证码发送失败')
        refreshCaptcha()
        sendingCaptcha.value = false
        captchaButtonText.value = '发送验证码'
      }
    })
    .catch(err => {
      ElMessage.error('网络错误，请稍后重试')
      refreshCaptcha()
      sendingCaptcha.value = false
      captchaButtonText.value = '发送验证码'
    })
}

const nextStep1 = () => {
  formRef1.value.validate(valid => {
    if (valid) {
      loading.value = true
      request.post('/registerStep1', step1.form).then(res => {
        if (res.code === '200') {
          currentStep.value = 2
          ElMessage.success('邮箱验证成功，请设置用户名和密码')
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

const nextStep2 = () => {
  formRef2.value.validate(valid => {
    if (valid) {
      loading.value = true
      request.post('/registerStep2', {
        email: step1.form.email,
        username: step2.form.username,
        password: step2.form.password,
        role: step1.form.role
      }).then(res => {
        if (res.code === '200') {
          accountId.value = res.data
          currentStep.value = 3
          ElMessage.success('用户名和密码设置成功，请完善个人信息')
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

const completeRegister = () => {
  formRef3.value.validate(valid => {
    if (valid) {
      loading.value = true
      request.post('/registerStep3', {
        accountId: accountId.value,
        name: step3.form.name,
        phone: step3.form.phone,
        avatar: step3.form.avatar,
        role: step1.form.role
      }).then(res => {
        if (res.code === '200') {
          ElMessage.success('注册成功！')
          router.push('/login')
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const handleAvatarSuccess = (res, file) => {
  if (res.code === '200') {
    step3.form.avatar = res.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('头像上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是图片格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
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
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
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

/* 步骤指示器样式 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  padding: 0 10px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.step.active .step-number {
  background: #1890ff;
  color: white;
}

.step.completed .step-number {
  background: #52c41a;
  color: white;
}

.step-label {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.step.active .step-label {
  color: #1890ff;
  font-weight: 500;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #e5e7eb;
  margin: 0 8px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.step-line.active {
  background: #1890ff;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

/* 图形验证码 */
.captcha-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.captcha-wrapper .el-input {
  flex: 1;
}

.captcha-code {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.captcha-code:hover {
  border-color: #409eff;
}

.captcha-btn {
  min-width: 100px;
  height: 40px;
  font-size: 13px;
}

.captcha-btn.sending {
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  color: #999;
}

/* 按钮行 */
.button-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.button-row .el-button {
  flex: 1;
}

/* 头像上传 */
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #1890ff;
}

.avatar-placeholder {
  display: flex;
  flex-direction: 1column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 4px;
}

.avatar-placeholder span {
  font-size: 12px;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* 底部 */
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