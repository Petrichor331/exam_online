<template>
  <div class="ai-container">
    <!-- 头部 -->
    <div class="ai-header">
      <div class="header-left">
        <div class="ai-logo">
          <div class="logo-inner">AI</div>
        </div>
        <div class="header-info">
          <h2>AI 学习助手</h2>
          <div class="status">
            <span class="status-dot"></span>
            <span>在线</span>
          </div>
        </div>
      </div>
      <el-button class="clear-btn" @click="clearChat" text>
        <el-icon><Delete /></el-icon>
        <span>清空对话</span>
      </el-button>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesRef">
      <!-- 欢迎页 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-card">
          <div class="welcome-ai">
            <span>AI</span>
          </div>
          <h3>你好！我是你的 AI 学习助手</h3>
          <p>我可以帮你解答问题、分析题目、提供学习建议</p>
        </div>
        
        <div class="quick-section">
          <div class="quick-title">快速开始</div>
          <div class="quick-grid">
            <div class="quick-card" @click="sendQuick('帮我分析一下这道题的解题思路')">
              <div class="quick-icon">
                <el-icon><Aim /></el-icon>
              </div>
              <div class="quick-text">
                <div class="quick-name">分析解题思路</div>
                <div class="quick-desc">帮你梳理解题方法</div>
              </div>
            </div>
            <div class="quick-card" @click="sendQuick('什么是面向对象编程？')">
              <div class="quick-icon">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="quick-text">
                <div class="quick-name">解释概念</div>
                <div class="quick-desc">用通俗语言讲解</div>
              </div>
            </div>
            <div class="quick-card" @click="sendQuick('如何准备期末考试？')">
              <div class="quick-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="quick-text">
                <div class="quick-name">学习建议</div>
                <div class="quick-desc">制定学习计划</div>
              </div>
            </div>
            <div class="quick-card" @click="sendQuick('帮我复习Java基础知识点')">
              <div class="quick-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="quick-text">
                <div class="quick-name">知识复习</div>
                <div class="quick-desc">梳理重点知识</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="msg.role"
      >
        <div class="message-avatar">
          <div v-if="msg.role === 'assistant'" class="avatar-ai">
            <span>AI</span>
          </div>
          <div v-else class="avatar-user">
            <el-icon><User /></el-icon>
          </div>
        </div>
        <div class="message-body">
          <div class="message-bubble" v-html="formatMessage(msg.content)"></div>
          <div class="message-time">{{ getCurrentTime() }}</div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="message-row assistant">
        <div class="message-avatar">
          <div class="avatar-ai">
            <span>AI</span>
          </div>
        </div>
        <div class="message-body">
          <div class="typing-bubble">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你的问题..."
          @keydown.enter.ctrl="sendMessage"
          @keydown.enter.meta="sendMessage"
          :disabled="loading"
          resize="none"
        />
        <el-button
          class="send-btn"
          @click="sendMessage"
          :loading="loading"
          :disabled="!inputMessage.trim()"
        >
          <el-icon v-if="!loading"><Position /></el-icon>
        </el-button>
      </div>
      <div class="input-hint">按 <kbd>Ctrl</kbd> + <kbd>Enter</kbd> 发送</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { Delete, User, Aim, Reading, Calendar, Document, Position } from '@element-plus/icons-vue'

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref(null)

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'user', content })
  inputMessage.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await request.post('/ai/chat', {
      message: content,
      history: messages.value.map(m => ({ role: m.role, content: m.content }))
    })

    if (res.code === '200') {
      messages.value.push({ role: 'assistant', content: res.data })
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉，我暂时无法回答这个问题，请稍后再试。' })
    }
  } catch (error) {
    messages.value.push({ role: 'assistant', content: '网络错误，请检查连接后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const sendQuick = (text) => {
  inputMessage.value = text
  sendMessage()
}

const clearChat = () => {
  messages.value = []
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const getCurrentTime = () => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const formatMessage = (text) => {
  if (!text) return ''
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
}
</script>

<style scoped>
.ai-container {
  max-width: 880px;
  margin: 0 auto;
  height: calc(100vh - 130px);
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* 头部 */
.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  background: #000;
  border-bottom: 1px solid #222;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ai-logo {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-inner {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #fff;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 1px;
}

.header-info h2 {
  margin: 0;
  font-size: 17px;
  color: #fff;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
}

.clear-btn {
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  transition: all 0.2s;
}

.clear-btn:hover {
  color: #fff;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
  scroll-behavior: smooth;
}

/* 欢迎页 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 40px;
}

.welcome-card {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-ai {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
}

.welcome-ai span {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: #000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 24px;
  letter-spacing: 2px;
}

.welcome h3 {
  margin: 0 0 10px;
  font-size: 21px;
  color: #111;
  font-weight: 500;
}

.welcome p {
  margin: 0;
  color: #888;
  font-size: 14px;
}

.quick-section {
  width: 100%;
  max-width: 560px;
}

.quick-title {
  font-size: 12px;
  color: #aaa;
  margin-bottom: 14px;
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-card:hover {
  border-color: #000;
  background: #fff;
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #333;
  flex-shrink: 0;
}

.quick-card:hover .quick-icon {
  background: #000;
  color: #fff;
}

.quick-text {
  text-align: left;
}

.quick-name {
  font-size: 13px;
  font-weight: 500;
  color: #111;
  margin-bottom: 2px;
}

.quick-desc {
  font-size: 11px;
  color: #999;
}

/* 消息行 */
.message-row {
  display: flex;
  gap: 14px;
  margin-bottom: 22px;
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-ai {
  width: 38px;
  height: 38px;
}

.avatar-ai span {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 1px;
}

.avatar-user {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #333;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.message-body {
  max-width: 72%;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.message-row.assistant .message-bubble {
  background: #fff;
  color: #222;
  border: 1px solid #e8e8e8;
  border-top-left-radius: 4px;
}

.message-row.user .message-bubble {
  background: #111;
  color: #fff;
  border-top-right-radius: 4px;
}

.message-bubble :deep(strong) {
  font-weight: 600;
}

.message-row.assistant .message-bubble :deep(strong) {
  color: #000;
}

.message-bubble :deep(code) {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.message-row.user .message-bubble :deep(code) {
  background: rgba(255, 255, 255, 0.15);
}

.message-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 6px;
  padding: 0 4px;
}

.message-row.user .message-time {
  text-align: right;
}

/* 打字动画 */
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  border-top-left-radius: 4px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #000;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(2) { animation-delay: 0.15s; }
.typing-dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.2; }
  30% { opacity: 1; }
}

/* 输入区域 */
.input-area {
  padding: 16px 24px 14px;
  background: #fff;
  border-top: 1px solid #eee;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-radius: 14px;
  padding: 8px 8px 8px 16px;
  transition: all 0.2s;
}

.input-wrapper:focus-within {
  border-color: #000;
  background: #fff;
}

.input-wrapper :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  color: #111;
}

.input-wrapper :deep(.el-textarea__inner)::placeholder {
  color: #aaa;
}

.input-wrapper :deep(.el-textarea__inner):focus {
  box-shadow: none;
}

.send-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #000;
  border: none;
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #222;
}

.send-btn:disabled {
  background: #e5e5e5;
  color: #bbb;
}

.input-hint {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
  color: #bbb;
}

.input-hint kbd {
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 10px;
  font-family: inherit;
  color: #888;
}

/* 滚动条 */
.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}
</style>
