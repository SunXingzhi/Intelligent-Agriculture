<script setup>
import { ref, onMounted } from 'vue'
import { getAllData } from '@/api/environment'
import * as echarts from 'echarts'

const chartRef = ref(null)

async function initChart() {
  try {
    const res = await getAllData()
    const dataList = res.data || res || []
    if (!dataList.length || !chartRef.value) return

    const sorted = [...dataList].sort((a, b) =>
      new Date(a.recordTime) - new Date(b.recordTime)
    )

    // 只取最近 20 条
    const recent = sorted.slice(-20)

    const timeLabels = recent.map(d => {
      const t = d.recordTime || ''
      return t.length > 16 ? t.substring(11, 16) : t
    })

    const chart = echarts.init(chartRef.value)

    chart.setOption({
      title: {
        text: '最近数据趋势',
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['温度(°C)', '湿度(%)', 'CO₂(ppm/10)'],
        bottom: 0,
        textStyle: { fontSize: 11 }
      },
      xAxis: {
        type: 'category',
        data: timeLabels,
        axisLabel: { fontSize: 10 }
      },
      yAxis: [
        {
          type: 'value',
          name: '温度/湿度',
          position: 'left',
          axisLabel: { fontSize: 10 }
        },
        {
          type: 'value',
          name: 'CO₂',
          position: 'right',
          axisLabel: { fontSize: 10 }
        }
      ],
      series: [
        {
          name: '温度(°C)',
          type: 'line',
          data: recent.map(d => d.Temperature),
          smooth: true,
          lineStyle: { color: '#f56c6c', width: 2 },
          itemStyle: { color: '#f56c6c' }
        },
        {
          name: '湿度(%)',
          type: 'line',
          data: recent.map(d => d.AirHumidity),
          smooth: true,
          lineStyle: { color: '#409eff', width: 2 },
          itemStyle: { color: '#409eff' }
        },
        {
          name: 'CO₂(ppm/10)',
          type: 'bar',
          yAxisIndex: 1,
          data: recent.map(d => d.CarbonConcentration ? (d.CarbonConcentration / 10).toFixed(1) : 0),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(103, 194, 58, 0.6)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.2)' }
            ])
          }
        }
      ],
      grid: { left: 50, right: 60, bottom: 40, top: 50 }
    })

    // 自适应窗口
    window.addEventListener('resize', () => chart.resize())
  } catch (e) {
    console.error('趋势图加载失败', e)
  }
}

onMounted(() => {
  setTimeout(initChart, 200)
})
</script>

<template>
  <div ref="chartRef" class="trend-chart"></div>
</template>

<style scoped>
.trend-chart {
  width: 100%;
  height: 350px;
}
</style>
