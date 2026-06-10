import api from './index'

// 获取所有环境数据
export function getAllData() {
  return api.get('/all')
}

// 获取最新一条数据
export function getLatestData() {
  return api.get('/latest')
}

// 根据 ID 获取详情
export function getDataById(id) {
  return api.get(`/detail/${id}`)
}

// 添加一条数据
export function addData(data) {
  return api.post('/add', data)
}

// 批量添加数据
export function batchAddData(dataList) {
  return api.post('/batch-add', dataList)
}

// 更新数据
export function updateData(id, data) {
  return api.put(`/update/${id}`, data)
}

// 删除数据
export function deleteData(id) {
  return api.delete(`/delete/${id}`)
}

// 获取统计数据
export function getStatistics() {
  return api.get('/statistics')
}

// 按温度范围查询
export function getDataByTemperature(min, max) {
  return api.get('/temperature', { params: { min, max } })
}

// 健康检查
export function healthCheck() {
  return api.get('/health')
}
