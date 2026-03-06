<template>
  <div class="password-container">
    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      
      <el-form ref="formRef" :rules="rules" :model="form" label-width="80px">
        <el-form-item label="原密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入原密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" placeholder="请输入新密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" placeholder="请确认新密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="updatePassword" :loading="loading" style="width: 200px;">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'
import { getCurrentUser, clearCurrentUser } from '@/utils/userStorage.js'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  password: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入确认密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  password: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validatePass, trigger: 'blur' }]
}

const updatePassword = () => {
  formRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      request.put('/updatePassword', {
        password: form.password,
        newPassword: form.newPassword
      }).then(res => {
        if (res.code === '200') {
          ElMessage.success('修改成功，请重新登录')
          logout()
        } else {
          ElMessage.error(res.msg || '修改失败')
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

const logout = () => {
  clearCurrentUser()
  router.push('/login')
}
</script>

<style scoped>
.password-container {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.password-card {
  width: 100%;
  max-width: 500px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

:deep(.el-card__header) {
  background: #333;
  border-bottom: 1px solid #444;
}

:deep(.el-card__body) {
  background: #2a2a2a;
  padding: 30px;
}

:deep(.el-form-item__label) {
  color: #ccc;
}

:deep(.el-input__wrapper) {
  background: #333;
  box-shadow: none;
  border: 1px solid #444;
}

:deep(.el-input__inner) {
  color: #fff;
}

:deep(.el-input__inner)::placeholder {
  color: #666;
}

:deep(.el-button--primary) {
  background: #3584e4;
  border-color: #3584e4;
}

:deep(.el-button--primary:hover) {
  background: #1a5fb4;
  border-color: #1a5fb4;
}
</style>
