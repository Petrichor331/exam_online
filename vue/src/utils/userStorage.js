/**
 * 用户存储工具
 * 支持多用户同时登录（在不同标签页）
 */

// 获取当前登录用户
export function getCurrentUser() {
  // 首先尝试获取当前用户标记
  const currentKey = sessionStorage.getItem('xm-user-current')
  if (currentKey) {
    const userData = sessionStorage.getItem(currentKey)
    if (userData) {
      return JSON.parse(userData)
    }
  }
  
  // 兼容旧版本：检查 localStorage
  const oldUser = localStorage.getItem('xm-user')
  if (oldUser) {
    // 迁移到新的存储方式
    const user = JSON.parse(oldUser)
    const newKey = `xm-user-${user.role}-${user.id}`
    sessionStorage.setItem(newKey, oldUser)
    sessionStorage.setItem('xm-user-current', newKey)
    return user
  }
  
  return {}
}

// 保存用户信息
export function setCurrentUser(user) {
  const userKey = `xm-user-${user.role}-${user.id}`
  sessionStorage.setItem(userKey, JSON.stringify(user))
  sessionStorage.setItem('xm-user-current', userKey)
}

// 清除当前用户
export function clearCurrentUser() {
  const currentKey = sessionStorage.getItem('xm-user-current')
  if (currentKey) {
    sessionStorage.removeItem(currentKey)
  }
  sessionStorage.removeItem('xm-user-current')
  
  // 清除旧版本的 localStorage
  localStorage.removeItem('xm-user')
}

// 更新用户信息
export function updateCurrentUser(user) {
  setCurrentUser(user)
}
