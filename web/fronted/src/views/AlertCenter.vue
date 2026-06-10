<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllData, getLatestData } from '@/api/environment'
import { Bell, Warning, CircleCheck, InfoFilled } from '@element-plus/icons-vue'

const alerts = ref([])
const loading = ref(true)

// 告警阈值配置
const thresholds = {
  temperature: { min: 20, max: 30, label: '空气温度', unit: '°C' },
  humidity: { min: 60, max: 80, label: '空气湿度', unit: '%' },
  co2: { min: 400, max: 1000, label: 'CO₂浓度', unit: 'ppm' },
  light: { min: 20000, max: 40000, label: '光照强度', unit: 'lux' },
  soilMoisture: { min: 60, max: 80, label: '土壤湿度', unit: '%' },
  ph: { min: 6.0, max: 7.5, label: 'pH值', unit: '' }
}

function checkAlerts(dataList) {
  const result = []
  dataList.forEach(item => {
    const time = item.recordedAt || item.createdAt || ''
    Object.keys(thresholds).forEach(key => {
      const val = Number(item[key])
      const t = thresholds[key]
      if (isNaN(val)) return

      if (val < t.min) {
        result.push({
          id: `${item.id}-${key}-low`,
          dataId: item.id,
          type: key,
          label: t.label,
          level: val < t.min * 0.8 ? 'critical' : 'warning',
          message: `${t.label}偏低：${val}${t.unit}（正常范围 ${t.min}~${t.max}${t.unit}）`,
          value: val,
          threshold: `${t.min}~${t.max}`,
          time
        })
      } else if (val > t.max) {
        result.push({
          id: `${item.id}-${key}-high`,
          dataId: item.id,
          type: key,
          label: t.label,
          level: val > t.max * 1.2 ? 'critical' : 'warning',
          message: `${t.label}超标：${val}${t.unit}（正常范围 ${t.min}~${t.max}${t.unit}）`,
          value: val,
          threshold: `${t.min}~${t.max}`,
          time
        })
      }
    })
  })
  return result
}

const filteredAlerts = ref([])
const levelFilter = ref('')
const typeFilter = ref('')

const displayAlerts = computed(() => {
  let list = filteredAlerts.value
  if (levelFilter.value) {
    list = list.filter(a => a.level === levelFilter.value)
  }
  if (typeFilter.value) {
    list = list.filter(a => a.type === typeFilter.value)
  }
  return list
})

const alertStats = computed(() => {
  const all = filteredAlerts.value
  return {
    total: all.length,
    critical: all.filter(a => a.level === 'critical').length,
    warning: all.filter(a => a.level === 'warning').length
  }
})

function getTagType(level) {
  if (level === 'critical') return 'danger'
  if (level === 'warning') return 'warning'
  return 'info'
}

function getLevelText(level) {
  if (level === 'critical') return '严重'
  if (level === 'warning') return '警告'
  return '提示'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAllData()
    const dataList = res.data || res || []
    alerts.value = checkAlerts(dataList)
    filteredAlerts.value = alerts.value
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>🔔 告警中心</h2>
      <p>自动检测环境数据异常，及时发现并处理告警信息</p>
    </div>

    <!-- 告警统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="8">
        <div class="stat-card alert-stat">
          <el-icon :size="28" color="#909399"><Bell /></el-icon>
          <div class="alert-stat-info">
            <div class="alert-stat-num">{{ alertStats.total }}</div>
            <div class="alert-stat-label">告警总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="8">
        <div class="stat-card alert-stat">
          <el-icon :size="28" color="#f56c6c"><Warning /></el-icon>
          <div class="alert-stat-info">
            <div class="alert-stat-num" style="color: #f56c6c;">{{ alertStats.critical }}</div>
            <div class="alert-stat-label">严重告警</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="8">
        <div class="stat-card alert-stat">
          <el-icon :size="28" color="#e6a23c"><InfoFilled /></el-icon>
          <div class="alert-stat-info">
            <div class="alert-stat-num" style="color: #e6a23c;">{{ alertStats.warning }}</div>
            <div class="alert-stat-label">警告</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="stat-card" style="margin-bottom: 16px;">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8">
          <el-select v-model="levelFilter" placeholder="告警级别" clearable style="width: 100%;">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-select v-model="typeFilter" placeholder="告警类型" clearable style="width: 100%;">
            <el-option label="空气温度" value="temperature" />
            <el-option label="空气湿度" value="humidity" />
            <el-option label="CO₂浓度" value="co2" />
            <el-option label="光照强度" value="light" />
            <el-option label="土壤湿度" value="soilMoisture" />
            <el-option label="pH值" value="ph" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-button type="primary" @click="fetchData">刷新数据</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 告警列表 -->
    <div class="stat-card">
      <el-table
        :data="displayAlerts"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        max-height="480"
        :row-class-name="({ row }) => row.level === 'critical' ? 'critical-row' : ''"
      >
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-icon
              :size="20"
              :color="row.level === 'critical' ? '#f56c6c' : '#e6a23c'"
            >
              <Warning />
            </el-icon>
          </template>
        </el-table-column>

        <el-table-column prop="label" label="告警指标" width="120" />

        <el-table-column prop="message" label="告警描述" min-width="260" />

        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.level)" size="small">
              {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="value" label="当前值" width="100" align="center" />

        <el-table-column prop="threshold" label="正常范围" width="120" align="center" />

        <el-table-column prop="time" label="采集时间" width="160" />
      </el-table>

      <el-empty
        v-if="!loading && displayAlerts.length === 0"
        description="暂无告警信息，环境数据一切正常 ✅"
      />
    </div>
  </div>
</template>

<style scoped>
.alert-stat {
  display: flex;
  align-items: center;
  gap: 16px;
}

.alert-stat-info {
  flex: 1;
}

.alert-stat-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.alert-stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

:deep(.critical-row) {
  background-color: #fef0f0 !important;
}
</style>
