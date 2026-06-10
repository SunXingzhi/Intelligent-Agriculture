import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api/environment',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败，请检查网络连接')
    return Promise.reject(error)
  }
)

export default api
