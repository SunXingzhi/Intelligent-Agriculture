<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllData, getStatistics } from '@/api/environment'
import * as echarts from 'echarts'

const allData = ref([])
const statistics = ref(null)
const loading = ref(true)

// 图表引用
const tempChartRef = ref(null)
const humidityChartRef = ref(null)
const co2ChartRef = ref(null)
const overviewChartRef = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const [allRes, statsRes] = await Promise.all([
      getAllData(),
      getStatistics()
    ])
    allData.value = allRes.data || allRes || []
    statistics.value = statsRes.data || statsRes
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (!allData.value.length) return

  const sortedData = [...allData.value].sort((a, b) =>
    new Date(a.recordedAt || a.createdAt) - new Date(b.recordedAt || b.createdAt)
  )

  const timeLabels = sortedData.map(d => {
    const t = d.recordedAt || d.createdAt || ''
    return t.length > 16 ? t.substring(5, 16) : t
  })

  // 温度趋势图
  if (tempChartRef.value) {
    const chart = echarts.init(tempChartRef.value)
    chart.setOption({
      title: { text: '温度变化趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timeLabels, axisLabel: { rotate: 30, fontSize: 10 } },
      yAxis: { type: 'value', name: '°C', min: 15, max: 40 },
      series: [{
        data: sortedData.map(d => d.temperature),
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(245, 108, 108, 0.15)' },
        lineStyle: { color: '#f56c6c', width: 2 },
        itemStyle: { color: '#f56c6c' },
        markLine: {
          data: [
            { yAxis: 20, name: '下限', lineStyle: { color: '#e6a23c', type: 'dashed' } },
            { yAxis: 30, name: '上限', lineStyle: { color: '#f56c6c', type: 'dashed' } }
          ]
        }
      }],
      grid: { left: 50, right: 20, bottom: 50, top: 50 }
    })
  }

  // 湿度趋势图
  if (humidityChartRef.value) {
    const chart = echarts.init(humidityChartRef.value)
    chart.setOption({
      title: { text: '湿度变化趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timeLabels, axisLabel: { rotate: 30, fontSize: 10 } },
      yAxis: { type: 'value', name: '%', min: 40, max: 100 },
      series: [{
        data: sortedData.map(d => d.humidity),
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        markLine: {
          data: [
            { yAxis: 60, name: '下限', lineStyle: { color: '#e6a23c', type: 'dashed' } },
            { yAxis: 80, name: '上限', lineStyle: { color: '#f56c6c', type: 'dashed' } }
          ]
        }
      }],
      grid: { left: 50, right: 20, bottom: 50, top: 50 }
    })
  }

  // CO2 趋势图
  if (co2ChartRef.value) {
    const chart = echarts.init(co2ChartRef.value)
    chart.setOption({
      title: { text: 'CO₂ 浓度趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timeLabels, axisLabel: { rotate: 30, fontSize: 10 } },
      yAxis: { type: 'value', name: 'ppm' },
      series: [{
        data: sortedData.map(d => d.co2),
        type: 'bar',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67c23a' },
            { offset: 1, color: '#95d475' }
          ])
        }
      }],
      grid: { left: 60, right: 20, bottom: 50, top: 50 }
    })
  }

  // 综合指标雷达图
  if (overviewChartRef.value && statistics.value) {
    const chart = echarts.init(overviewChartRef.value)
    const stats = statistics.value
    chart.setOption({
      title: { text: '环境综合评分', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {},
      radar: {
        indicator: [
          { name: '温度', max: 40 },
          { name: '湿度', max: 100 },
          { name: 'CO₂', max: 1500 },
          { name: '光照', max: 50000 },
          { name: '土壤湿度', max: 100 }
        ]
      },
      series: [{
        type: 'radar',
        data: [{
          value: [
            stats.avgTemperature || 0,
            stats.avgHumidity || 0,
            stats.avgCo2 || 0,
            stats.avgLight || 0,
            stats.avgSoilMoisture || 0
          ],
          name: '日均值',
          areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
          lineStyle: { color: '#409eff' },
          itemStyle: { color: '#409eff' }
        }]
      }]
    })
  }
}

onMounted(async () => {
  await fetchData()
  // 等 DOM 渲染完成
  setTimeout(initCharts, 100)
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📈 统计分析</h2>
      <p>环境数据的统计汇总与趋势可视化分析</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <!-- 统计卡片 -->
        <el-row :gutter="16" style="margin-bottom: 20px;">
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-highlight">
              <div class="stat-label">温度均值</div>
              <div class="stat-value" style="color: #f56c6c;">
                {{ statistics?.avgTemperature ?? '--' }} °C
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-highlight">
              <div class="stat-label">湿度均值</div>
              <div class="stat-value" style="color: #409eff;">
                {{ statistics?.avgHumidity ?? '--' }} %
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-highlight">
              <div class="stat-label">CO₂ 均值</div>
              <div class="stat-value" style="color: #67c23a;">
                {{ statistics?.avgCo2 ?? '--' }} ppm
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-highlight">
              <div class="stat-label">记录总数</div>
              <div class="stat-value" style="color: #e6a23c;">
                {{ statistics?.totalRecords ?? '--' }}
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 图表区域 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <div class="stat-card chart-card">
              <div ref="tempChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :xs="24" :md="12">
            <div class="stat-card chart-card">
              <div ref="humidityChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top: 16px;">
          <el-col :xs="24" :md="12">
            <div class="stat-card chart-card">
              <div ref="co2ChartRef" class="chart-container"></div>
            </div>
          </el-col>
          <el-col :xs="24" :md="12">
            <div class="stat-card chart-card">
              <div ref="overviewChartRef" class="chart-container"></div>
            </div>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.stat-highlight {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.chart-card {
  padding: 16px;
}

.chart-container {
  width: 100%;
  height: 320px;
}
</style>
