<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const latestDetection = ref(null)
const detectionHistory = ref([])

// 模拟数据 - 实际项目中替换为 API 调用
const mockDetection = {
  id: 1,
  deviceCode: 'TOMATO-001',
  greenCount: 15,
  breakerCount: 8,
  redCount: 3,
  totalCount: 26,
  confidenceAvg: 0.8723,
  imageUrl: '',
  recordedAt: '2026-06-10 14:30:30'
}

async function fetchLatestDetection() {
  loading.value = true
  try {
    // TODO: 替换为实际 API 调用
    // const res = await getLatestDetection()
    // latestDetection.value = res.data

    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))
    latestDetection.value = mockDetection
  } catch (e) {
    console.error('获取检测数据失败', e)
    ElMessage.error('获取检测数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLatestDetection()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>🍅 番茄长势检测</h2>
      <p>基于 YOLOv8 的番茄成熟度实时检测与分析</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <!-- 检测结果概览 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="16">
            <div class="stat-card">
              <h3 style="margin-bottom: 16px; font-size: 16px;">📸 最新检测画面</h3>
              <div class="detection-cameras-row">
                <div class="camera-block">
                  <h4 style="margin-bottom: 12px; font-size: 14px; color: #606266;">俯视摄像头</h4>
                  <div class="detection-overlooking-image-container">
                    <div v-if="latestDetection?.imageUrl" class="detection-image">
                      <img :src="latestDetection.imageUrl" alt="俯视检测画面" />
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
                    <div v-if="latestDetection?.imageUrl" class="detection-image">
                      <img :src="latestDetection.imageUrl" alt="平视检测画面" />
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
                <el-descriptions-item label="平均置信度">
                  <el-progress
                    :percentage="Math.round((latestDetection?.confidenceAvg || 0) * 100)"
                    :stroke-width="10"
                    :text-inside="true"
                  />
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="stat-card" style="margin-top: 16px;">
              <h3 style="margin-bottom: 16px; font-size: 16px;">🍅 成熟度分布</h3>
              <div class="maturity-grid">
                <div class="maturity-item green">
                  <span class="maturity-label">青果</span>
                  <span class="maturity-count">{{ latestDetection?.greenCount ?? 0 }}</span>
                </div>
                <div class="maturity-item breaker">
                  <span class="maturity-label">转色</span>
                  <span class="maturity-count">{{ latestDetection?.breakerCount ?? 0 }}</span>
                </div>
                <div class="maturity-item red">
                  <span class="maturity-label">成熟</span>
                  <span class="maturity-count">{{ latestDetection?.redCount ?? 0 }}</span>
                </div>
              </div>
              <div class="total-count">
                总计: <strong>{{ latestDetection?.totalCount ?? 0 }}</strong> 个番茄
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 检测说明 -->
        <div class="stat-card" style="margin-top: 16px;">
          <h3 style="margin-bottom: 16px; font-size: 16px;">ℹ️ 检测说明</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="检测模型">YOLOv8n</el-descriptions-item>
            <el-descriptions-item label="输入尺寸">640 × 640</el-descriptions-item>
            <el-descriptions-item label="检测频率">每 30 秒一帧</el-descriptions-item>
            <el-descriptions-item label="检测类别">青果 / 转色 / 成熟</el-descriptions-item>
            <el-descriptions-item label="推理设备">Jetson Nano / 树莓派</el-descriptions-item>
            <el-descriptions-item label="数据上报">HTTP POST (JSON + Base64)</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

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
