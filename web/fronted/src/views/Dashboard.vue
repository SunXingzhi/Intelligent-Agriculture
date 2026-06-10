<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getLatestData, getStatistics } from '@/api/environment'
import SensorCard from '@/components/SensorCard.vue'
import TrendChart from '@/components/TrendChart.vue'
import { ElMessage } from 'element-plus'

const latestData = ref(null)
const statistics = ref(null)
const loading = ref(true)
let timer = null

// 传感器指标配置
const sensorMetrics = [
  { key: 'temperature', label: '空气温度', unit: '°C', icon: '🌡️', min: 20, max: 30, color: '#f56c6c' },
  { key: 'humidity', label: '空气湿度', unit: '%', icon: '💧', min: 60, max: 80, color: '#409eff' },
  { key: 'co2', label: 'CO₂ 浓度', unit: 'ppm', icon: '🫁', min: 400, max: 1000, color: '#67c23a' },
  { key: 'light', label: '光照强度', unit: 'lux', icon: '☀️', min: 20000, max: 40000, color: '#e6a23c' },
  { key: 'soilMoisture', label: '土壤湿度', unit: '%', icon: '🌱', min: 60, max: 80, color: '#9b59b6' },
  { key: 'ph', label: 'pH 值', unit: '', icon: '⚗️', min: 6.0, max: 7.5, color: '#1abc9c' }
]

function getStatus(value, min, max) {
  if (value == null) return { text: '无数据', type: 'info' }
  const v = Number(value)
  if (v < min) return { text: '偏低', type: 'warning' }
  if (v > max) return { text: '超标', type: 'danger' }
  return { text: '正常', type: 'success' }
}

async function fetchData() {
  try {
    const [latestRes, statsRes] = await Promise.all([
      getLatestData(),
      getStatistics()
    ])
    latestData.value = latestRes.data || latestRes
    statistics.value = statsRes.data || statsRes
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
  // 每 30 秒自动刷新
  timer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📊 实时监控面板</h2>
      <p>实时展示番茄种植环境的传感器数据与系统状态</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <!-- 传感器数据卡片网格 -->
        <el-row :gutter="16" class="sensor-grid">
          <el-col
            v-for="metric in sensorMetrics"
            :key="metric.key"
            :xs="24" :sm="12" :md="8"
          >
            <SensorCard
              :label="metric.label"
              :value="latestData?.[metric.key]"
              :unit="metric.unit"
              :icon="metric.icon"
              :status="getStatus(latestData?.[metric.key], metric.min, metric.max)"
              :color="metric.color"
            />
          </el-col>
        </el-row>

        <!-- 统计概览 -->
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :xs="24" :md="12">
            <div class="stat-card">
              <h3 style="margin-bottom: 16px; font-size: 16px;">📈 数据统计概览</h3>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="温度均值">
                  {{ statistics?.avgTemperature ?? '--' }} °C
                </el-descriptions-item>
                <el-descriptions-item label="温度范围">
                  {{ statistics?.minTemperature ?? '--' }} ~ {{ statistics?.maxTemperature ?? '--' }} °C
                </el-descriptions-item>
                <el-descriptions-item label="湿度均值">
                  {{ statistics?.avgHumidity ?? '--' }} %
                </el-descriptions-item>
                <el-descriptions-item label="湿度范围">
                  {{ statistics?.minHumidity ?? '--' }} ~ {{ statistics?.maxHumidity ?? '--' }} %
                </el-descriptions-item>
                <el-descriptions-item label="CO₂ 均值">
                  {{ statistics?.avgCo2 ?? '--' }} ppm
                </el-descriptions-item>
                <el-descriptions-item label="记录总数">
                  {{ statistics?.totalRecords ?? '--' }} 条
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>

          <el-col :xs="24" :md="12">
            <div class="stat-card">
              <h3 style="margin-bottom: 16px; font-size: 16px;">🕐 最新采集信息</h3>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="数据ID">
                  {{ latestData?.id ?? '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="采集时间">
                  {{ latestData?.recordedAt || latestData?.createdAt || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="数据状态">
                  <el-tag type="success" size="small">有效</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
        </el-row>

        <!-- 趋势图 -->
        <div class="stat-card" style="margin-top: 20px;">
          <TrendChart />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.sensor-grid {
  margin-bottom: 0;
}
</style>
