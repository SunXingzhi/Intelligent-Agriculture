import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLatestData, getAllData, getStatistics } from '@/api/environment'

export const useEnvironmentStore = defineStore('environment', () => {
  const latestData = ref(null)
  const allData = ref([])
  const statistics = ref(null)
  const loading = ref(false)

  async function fetchLatest() {
    loading.value = true
    try {
      const res = await getLatestData()
      latestData.value = res.data || res
    } catch (e) {
      console.error('获取最新数据失败', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchAll() {
    loading.value = true
    try {
      const res = await getAllData()
      allData.value = res.data || res || []
    } catch (e) {
      console.error('获取全部数据失败', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchStatistics() {
    try {
      const res = await getStatistics()
      statistics.value = res.data || res
    } catch (e) {
      console.error('获取统计数据失败', e)
    }
  }

  return { latestData, allData, statistics, loading, fetchLatest, fetchAll, fetchStatistics }
})
