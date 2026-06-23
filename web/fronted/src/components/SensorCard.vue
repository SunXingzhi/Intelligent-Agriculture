<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: null },
  unit: { type: String, default: '' },
  icon: { type: String, default: 'iconfont icon-charts' },
  status: { type: Object, default: () => ({ text: '无数据', type: 'info' }) },
  color: { type: String, default: '#409eff' }
})
</script>

<template>
  <div class="sensor-card stat-card">
    <div class="sensor-header">
      <span class="sensor-icon">
        <i :class="icon"></i>
      </span>
      <el-tag :type="status.type" size="small" effect="plain">
        {{ status.text }}
      </el-tag>
    </div>

    <div class="sensor-value" :style="{ color }">
      {{ value != null ? value : '--' }}
      <span class="sensor-unit" v-if="unit && value != null">{{ unit }}</span>
    </div>

    <div class="sensor-label">{{ label }}</div>

    <!-- 进度条指示 -->
    <el-progress
      v-if="value != null"
      :percentage="100"
      :color="status.type === 'success' ? '#67c23a' : status.type === 'warning' ? '#e6a23c' : '#f56c6c'"
      :stroke-width="3"
      :show-text="false"
      style="margin-top: 12px;"
    />
  </div>
</template>

<style scoped>
.sensor-card {
  padding: 20px;
  margin-bottom: 16px;
  text-align: center;
}

.sensor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sensor-icon {
  font-size: 32px;
}

.sensor-icon .iconfont {
  font-size: inherit;
}

.sensor-value {
  font-size: 36px;
  font-weight: 700;
  line-height: 1.2;
}

.sensor-unit {
  font-size: 16px;
  font-weight: 400;
  opacity: 0.7;
}

.sensor-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 6px;
}
</style>
