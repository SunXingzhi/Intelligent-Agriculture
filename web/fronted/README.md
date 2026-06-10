前端内容已全部生成完毕，项目构建成功 ✅

📁 生成的项目结构

web/fronted/src/
├── main.js                    # 入口文件 (Element Plus + Pinia + Router)
├── App.vue                    # 主布局 (侧边栏 + 顶栏 + 路由视图)
├── router/index.js            # 路由配置 (4 个页面)
├── api/
│   ├── index.js               # Axios 实例 (代理到后端 /api/environment)
│   └── environment.js         # 环境数据 API 封装
├── stores/environment.js      # Pinia 状态管理
├── views/
│   ├── Dashboard.vue          # 📊 实时监控面板
│   ├── SensorHistory.vue      # 📋 传感器历史数据
│   ├── Statistics.vue         # 📈 统计分析 (ECharts 图表)
│   └── AlertCenter.vue        # 🔔 告警中心
├── components/
│   ├── SensorCard.vue         # 传感器数据卡片组件
│   └── TrendChart.vue         # 趋势图组件
└── assets/
    ├── main.css               # 全局样式
    └── base.css               # 基础样式
🎯 四个页面功能
页面	功能
实时监控	6 个传感器指标卡片（温度/湿度/CO₂/光照/土壤湿度/pH），自动 30 秒刷新，状态判定（正常/偏低/超标）
传感器历史	完整数据表格，支持温度范围筛选、分页、删除操作
统计分析	温度/湿度/CO₂ 趋势折线图 + 柱状图 + 雷达图，统计概览卡片