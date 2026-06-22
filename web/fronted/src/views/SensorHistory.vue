<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllData, getDataByTemperature, deleteData } from '@/api/environment'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Delete, Refresh } from '@element-plus/icons-vue'

const allData = ref([])
const loading = ref(true)
const searchTempMin = ref('')
const searchTempMax = ref('')
const isFiltering = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(10)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return allData.value.slice(start, start + pageSize.value)
})

const totalItems = computed(() => allData.value.length)

// 表格列配置
const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'Temperature', label: '温度(°C)', width: 100 },
  { prop: 'AirHumidity', label: '湿度(%)', width: 100 },
  { prop: 'CarbonConcentration', label: 'CO₂(ppm)', width: 110 },
  { prop: 'LightIntensity', label: '光照(lux)', width: 120 },
  { prop: 'SoilHumidity', label: '土壤湿度(%)', width: 120 },
  { prop: 'PH', label: 'pH', width: 80 },
  { prop: 'recordTime', label: '采集时间', minWidth: 160 }
]

async function fetchAllData() {
  loading.value = true
  isFiltering.value = false
  try {
    const res = await getAllData()
    allData.value = res.data || res || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function filterByTemperature() {
  if (searchTempMin.value === '' || searchTempMax.value === '') {
    ElMessage.warning('请输入温度范围')
    return
  }
  loading.value = true
  isFiltering.value = true
  try {
    const res = await getDataByTemperature(searchTempMin.value, searchTempMax.value)
    allData.value = res.data || res || []
    currentPage.value = 1
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除 ID 为 ${row.id} 的数据吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteData(row.id)
    ElMessage.success('删除成功')
    fetchAllData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function handlePageChange(page) {
  currentPage.value = page
}

onMounted(fetchAllData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📋 传感器历史数据</h2>
      <p>查看、筛选和管理所有历史采集的环境传感器数据</p>
    </div>

    <!-- 搜索栏 -->
    <div class="stat-card" style="margin-bottom: 16px;">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="5">
          <el-input
            v-model.number="searchTempMin"
            placeholder="最低温度"
            type="number"
            clearable
          >
            <template #prefix>最低 °C</template>
          </el-input>
        </el-col>
        <el-col :xs="24" :sm="8" :md="5">
          <el-input
            v-model.number="searchTempMax"
            placeholder="最高温度"
            type="number"
            clearable
          >
            <template #prefix>最高 °C</template>
          </el-input>
        </el-col>
        <el-col :xs="24" :sm="8" :md="14">
          <el-button type="primary" :icon="Search" @click="filterByTemperature">
            温度筛选
          </el-button>
          <el-button :icon="Refresh" @click="fetchAllData">
            重置
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 数据表格 -->
    <div class="stat-card">
      <el-table
        :data="paginatedData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        max-height="520"
      >
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          sortable
        />

        <!-- 温度状态列 -->
        <el-table-column label="温度状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.temperature < 20 ? 'warning' : row.temperature > 30 ? 'danger' : 'success'"
              size="small"
            >
              {{ row.temperature < 20 ? '偏低' : row.temperature > 30 ? '超标' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              circle
              @click="handleDelete(row)"
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalItems"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
