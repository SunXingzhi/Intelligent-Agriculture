<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Monitor,
  DataLine,
  PieChart,
  Bell,
  Fold,
  Expand,
  View
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const isCollapsed = ref(false)

const menuItems = [
  { path: '/dashboard', icon: Monitor, title: '实时监控' },
  { path: '/detection', icon: View, title: '番茄长势检测' },
  { path: '/sensor-history', icon: DataLine, title: '传感器历史' },
  { path: '/statistics', icon: PieChart, title: '统计分析' },
]

function navigateTo(path) {
  router.push(path)
}

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}
</script>

<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="app-sidebar">
      <div class="sidebar-header">
        <img src="@/assets/logo.svg" alt="logo" class="sidebar-logo" />
        <span v-show="!isCollapsed" class="sidebar-title">番茄监测系统</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        :collapse-transition="false"
        router
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
          @click="navigateTo(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="app-main-container">
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <span class="header-title">{{ route.meta.title || '实时监控' }}</span>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="plain" size="small">
            <el-icon><Monitor /></el-icon>
            系统运行中
          </el-tag>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.app-sidebar {
  background-color: #1d1e2c;
  transition: width 0.3s;
  overflow: hidden;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-logo {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
}

.app-sidebar .el-menu {
  border-right: none;
}

.app-main-container {
  flex: 1;
  overflow: hidden;
}

.app-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.app-main {
  background-color: #f0f2f5;
  overflow-y: auto;
  padding: 0;
}
</style>
