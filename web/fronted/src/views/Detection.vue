<template>
  <div class="page-container">
    <div class="page-header">
      <h2> 番茄长势检测</h2>
      <p>基于 YOLOv8 的番茄成熟度实时检测与分析</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <!-- 检测结果概览 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="16">
            <div class="stat-card">
              <h3 style="margin-bottom: 16px; font-size: 16px;">最新检测画面</h3>
              <div class="detection-cameras-row">
                <div class="camera-block">
                  <h4 style="margin-bottom: 12px; font-size: 14px; color: #606266;">俯视摄像头</h4>
                  <div class="detection-overlooking-image-container">
                    <div v-if="topImage" class="detection-image">
                      <img :src="'data:image/jpeg;base64,' + topImage" alt="俯视检测画面" />
                    </div>
                    <div v-else class="detection-placeholder">
                      <el-icon :size="48" color="#c0c4cc"><Monitor /></el-icon>
                      <p>等待摄像头接入...</p>
                      <p class="placeholder-hint">俯视摄像头画面</p>
                    </div>
                  </div>
                </div>
                <div class="camera-block">
                  <h4 style="margin-bottom: 12px; font-size: 14px; color: #606266;">平视摄像头</h4>
                  <div class="detection-sidelooking-image-container">
                    <div v-if="frontImage" class="detection-image">
                      <img :src="'data:image/jpeg;base64,' + frontImage" alt="平视检测画面" />
                    </div>
                    <div v-else class="detection-placeholder">
                      <el-icon :size="48" color="#c0c4cc"><Monitor /></el-icon>
                      <p>等待摄像头接入...</p>
                      <p class="placeholder-hint">平视摄像头画面</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :md="8">
            <div class="stat-card">
              <h3 style="margin-bottom: 16px; font-size: 16px;"> 检测统计</h3>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="设备编号">
                  {{ latestDetection?.deviceCode || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="检测时间">
                  {{ latestDetection?.recordedAt || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="茎粗（俯视图）">
                  {{ latestDetection?.stemDiameter != null ? latestDetection.stemDiameter.toFixed(2) + ' mm' : '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="平均置信度">
                  <el-progress
                    :percentage="maturityStats.count > 0 ? Math.round((maturityStats.confidenceSum / maturityStats.count) * 100) : 0"
                    :stroke-width="10"
                    :text-inside="true"
                  />
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="stat-card" style="margin-top: 16px;">
              <h3 style="margin-bottom: 16px; font-size: 16px;"> 成熟度分布</h3>
              <div class="maturity-grid">
                <div class="maturity-item green">
                  <span class="maturity-label">青果</span>
                  <span class="maturity-count">{{ maturityStats.green }}</span>
                </div>
                <div class="maturity-item breaker">
                  <span class="maturity-label">转色</span>
                  <span class="maturity-count">{{ maturityStats.breaker }}</span>
                </div>
                <div class="maturity-item red">
                  <span class="maturity-label">成熟</span>
                  <span class="maturity-count">{{ maturityStats.red }}</span>
                </div>
              </div>
              <div class="total-count">
                总计: <strong>{{ latestDetection?.totalCount ?? maturityStats.count ?? 0 }}</strong> 个番茄
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 检测说明 -->
        <div class="stat-card" style="margin-top: 16px;">
          <h3 style="margin-bottom: 16px; font-size: 16px;">检测说明</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="检测模型">YOLOv8n</el-descriptions-item>
            <el-descriptions-item label="输入尺寸">1920*1080</el-descriptions-item>
            <el-descriptions-item label="检测频率">每 5 秒一帧</el-descriptions-item>
            <el-descriptions-item label="检测类别">青果 / 转色 / 成熟</el-descriptions-item>
            <el-descriptions-item label="推理设备">RaspberryPi 5</el-descriptions-item>
            <el-descriptions-item label="数据上报">HTTP POST (JSON + Base64)</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useWebSocket } from '../tools/websocket'
import { useImageStream } from '@/tools/useImageStream'

// 使用相对路径，由 Vite proxy 转发到后端 localhost:8081
const { messages } = useWebSocket(`ws://${location.host}/ws/sensor`)

const { topImage, frontImage } = useImageStream()

const loading = ref(false)
const latestDetection = ref(null)
const detectionHistory = ref([])

// 用 computed 提取最新消息，watch 此 computed 可靠触发
const latestMsg = computed(() => {
  if (messages.value.length === 0) return null
  return messages.value[messages.value.length - 1]
})

/**
 * 解析后端 tomatoList 格式字符串: "fully_ripened：0.95/green：0.88/half_ripened：0.76"
 * 返回成熟度分布计数
 */
const maturityStats = computed(() => {
  const list = latestDetection.value?.tomatoList
  if (!list || typeof list !== 'string') {
    return { green: 0, breaker: 0, red: 0, confidenceSum: 0, count: 0 }
  }
  let green = 0, breaker = 0, red = 0, confidenceSum = 0, count = 0
  for (const item of list.split('/')) {
    const parts = item.split('：')
    if (parts.length < 2) continue
    const ripeness = parts[0].trim().toLowerCase()
    const conf = parseFloat(parts[1]) || 0
    count++
    confidenceSum += conf
    if (ripeness === 'green') green++
    else if (ripeness === 'half_ripened') breaker++
    else if (ripeness === 'fully_ripened') red++
  }
  return { green, breaker, red, confidenceSum, count }
})

// 监听 WebSocket 推送（后端格式: { type, image: { deviceAlias, imageData, ... }, data: { ... } }）
watch(latestMsg, (raw) => {
  if (!raw) return
  try {
    const msg = JSON.parse(raw)
    const image = msg.image
    const data = msg.data
    if (!image || !image.imageData) return

    // 优先用 type 字段判断，兜底用 deviceAlias 前缀
    const viewType = msg.type || ''
    const alias = (image.deviceAlias || '').toLowerCase()
    if (viewType === 'top-view' || alias.startsWith('top')) {
      topImage.value = image.imageData
    } else if (viewType === 'front-view' || alias.startsWith('front')) {
      frontImage.value = image.imageData
    }

    // 合并统计信息供模板使用
    latestDetection.value = {
      deviceCode: image.deviceAlias,
      recordedAt: data?.recordTime || '',
      // 前视图统计
      totalCount: data?.tomatoCount ?? latestDetection.value?.totalCount,
      tomatoList: data?.tomatoList ?? latestDetection.value?.tomatoList,
      // 俯视图统计
      stemDiameter: data?.stemDiameter ?? latestDetection.value?.stemDiameter,
    }
    detectionHistory.value.unshift(msg)
  } catch (e) {
    console.error('解析检测数据失败', e)
  }
})

</script>


<style scoped>
/* 关键：父元素 flex 让两个子容器并排 */
.detection-cameras-row {
  display: flex;
  gap: 16px;
}

.camera-block {
  flex: 1;          /* 两个块各占一半 */
  min-width: 0;     /* 防止内容溢出 */
}

.detection-overlooking-image-container,
.detection-sidelooking-image-container {
  min-height: 300px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.detection-image img {
  width: 100%;
  height: auto;
  display: block;
}

.detection-placeholder {
  height: 100%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.detection-placeholder p {
  margin-top: 12px;
  font-size: 14px;
}

.placeholder-hint {
  font-size: 12px !important;
  color: #c0c4cc;
}

.maturity-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.maturity-item {
  text-align: center;
  padding: 16px 8px;
  border-radius: 8px;
}

.maturity-item.green {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.maturity-item.breaker {
  background: #fdf6ec;
  border: 1px solid #faecd8;
}

.maturity-item.red {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.maturity-label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.maturity-count {
  font-size: 28px;
  font-weight: 700;
}

.maturity-item.green .maturity-count {
  color: #67c23a;
}

.maturity-item.breaker .maturity-count {
  color: #e6a23c;
}

.maturity-item.red .maturity-count {
  color: #f56c6c;
}

.total-count {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 14px;
  color: #606266;
}

.total-count strong {
  font-size: 18px;
  color: #303133;
}
</style>
