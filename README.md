# 智慧农业 — 番茄长势实时监测系统

> 基于物联网 + 深度学习的番茄种植环境监控与长势分析平台

---

## 一、系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        硬件采集层                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ 温湿度   │  │ CO2传感器 │  │ 土壤传感器│  │ USB摄像头     │  │
│  │ DHT22    │  │ MH-Z19B  │  │ 土壤湿度  │  │ OV5647/IMX219 │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬────────┘  │
│       └──────────────┴─────────────┴───────────────┘            │
│                         ▼  I2C/UART/USB                         │
│              ┌──────────────────────┐                           │
│              │  Jetson Nano / RPi5  │  ← 主控平台               │
│              │  YOLOv8n 推理        │                           │
│              └──────────┬───────────┘                           │
└─────────────────────────┼───────────────────────────────────────┘
                          │  HTTP POST (JSON + Base64 Image)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     服务端 (Spring Boot 3.x)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Sensor   │  │Detection │  │Statistics│  │   Alert       │  │
│  │Controller│  │Controller│  │Controller│  │  Controller    │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬───────┘  │
│       └──────────────┴─────────────┴────────────────┘           │
│                         ▼  MyBatis                               │
│              ┌──────────────────────┐                           │
│              │      MySQL 8.x       │                           │
│              └──────────────────────┘                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │  REST API (JSON)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     前端 (Vue 3 + Vite)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ 实时监控 │  │ 数据统计 │  │ 历史记录 │  │  告警中心     │  │
│  │  Dashboard│  │ Charts  │  │  History │  │   Alerts      │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、核心功能需求

### 1. 实时检测

#### 传感器数据采集
| 传感器 | 指标 | 正常范围 | 采集频率 |
|--------|------|----------|----------|
| DHT22 | 空气温度 | 20~30°C | 每 5 分钟 |
| DHT22 | 空气湿度 | 60~80% | 每 5 分钟 |
| MH-Z19B | CO2 浓度 | 400~1000 ppm | 每 5 分钟 |
| BH1750 | 光照强度 | 20000~40000 lux | 每 5 分钟 |
| 土壤湿度传感器 | 土壤湿度 | 60~80% | 每 5 分钟 |
| 土壤NPK传感器 | 土壤养分 | N:150~200mg/kg | 每 30 分钟 |

状态判定：`正常` / `偏低` / `超标` — 根据阈值自动判断

#### 实景图像 (番茄长势)
- Web 页面独立窗口显示实时检测画面 (含 YOLO 标注框)
- 检测类别：`green`(青果) / `breaker`(转色) / `red`(成熟)
- 检测频率：每 30 秒一帧

### 2. 统计分析

#### 日统计 (每日 24:00 自动执行)
- 温湿度、CO2 日均值 / 最大值 / 最小值
- 番茄各成熟度数量变化趋势
- 环境适宜度综合评分

#### 周统计 (可选)
- 各指标周变化曲线
- 番茄生长阶段识别 (苗期 → 开花 → 坐果 → 转色 → 成熟)

### 3. 告警推送
- 传感器数据异常时自动生成告警
- 告警级别：`info`(提示) / `warning`(警告) / `critical`(严重)

---

## 三、数据库设计

> 数据库名称：`tomato_monitor`

### 3.1 ER 关系图

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│   device     │       │   sensor_data    │       │ daily_statistics │
│ (设备信息表)  │──1:N──│  (传感器数据表)   │──1:N──│  (日统计表)       │
└──────────────┘       └──────────────────┘       └──────────────────┘
       │
       │ 1:N
       ▼
┌──────────────────┐       ┌──────────────────┐
│detection_result  │       │      alert       │
│ (检测结果表)      │       │   (告警记录表)    │
└──────────────────┘       └──────────────────┘
```

### 3.2 表结构详细设计

#### 表1: `device` — 设备信息表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 设备ID |
| `device_code` | VARCHAR(64) | UNIQUE, NOT NULL | 设备编号 (如 `TOMATO-001`) |
| `device_name` | VARCHAR(128) | NOT NULL | 设备名称 |
| `location` | VARCHAR(255) | | 安装位置 |
| `status` | TINYINT | DEFAULT 1 | 0=离线, 1=在线 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

```sql
CREATE TABLE `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `device_name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '安装位置',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0=离线, 1=在线',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`device_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';
```

#### 表2: `sensor_data` — 传感器数据表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| `device_id` | BIGINT | FK → device.id, NOT NULL | 设备ID |
| `temperature` | DECIMAL(5,2) | | 空气温度 (°C) |
| `humidity` | DECIMAL(5,2) | | 空气湿度 (%) |
| `co2` | DECIMAL(8,2) | | CO2 浓度 (ppm) |
| `light` | DECIMAL(10,2) | | 光照强度 (lux) |
| `soil_moisture` | DECIMAL(5,2) | | 土壤湿度 (%) |
| `soil_nitrogen` | DECIMAL(8,2) | | 土壤氮含量 (mg/kg) |
| `soil_phosphorus` | DECIMAL(8,2) | | 土壤磷含量 (mg/kg) |
| `soil_potassium` | DECIMAL(8,2) | | 土壤钾含量 (mg/kg) |
| `recorded_at` | DATETIME | NOT NULL, INDEX | 采集时间 |

```sql
CREATE TABLE `sensor_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '空气温度(°C)',
  `humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '空气湿度(%)',
  `co2` DECIMAL(8,2) DEFAULT NULL COMMENT 'CO2浓度(ppm)',
  `light` DECIMAL(10,2) DEFAULT NULL COMMENT '光照强度(lux)',
  `soil_moisture` DECIMAL(5,2) DEFAULT NULL COMMENT '土壤湿度(%)',
  `soil_nitrogen` DECIMAL(8,2) DEFAULT NULL COMMENT '土壤氮含量(mg/kg)',
  `soil_phosphorus` DECIMAL(8,2) DEFAULT NULL COMMENT '土壤磷含量(mg/kg)',
  `soil_potassium` DECIMAL(8,2) DEFAULT NULL COMMENT '土壤钾含量(mg/kg)',
  `recorded_at` DATETIME NOT NULL COMMENT '采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `recorded_at`),
  KEY `idx_recorded_at` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器数据表';
```

#### 表3: `detection_result` — 番茄检测结果表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| `device_id` | BIGINT | FK → device.id, NOT NULL | 设备ID |
| `green_count` | INT | DEFAULT 0 | 青果数量 |
| `breaker_count` | INT | DEFAULT 0 | 转色数量 |
| `red_count` | INT | DEFAULT 0 | 成熟数量 |
| `total_count` | INT | DEFAULT 0 | 总检测数量 |
| `confidence_avg` | DECIMAL(5,4) | | 平均置信度 |
| `image_path` | VARCHAR(512) | | 标注后图片存储路径 |
| `image_base64` | LONGTEXT | | 图片Base64 (可选, 用于实时预览) |
| `recorded_at` | DATETIME | NOT NULL, INDEX | 检测时间 |

```sql
CREATE TABLE `detection_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `green_count` INT DEFAULT 0 COMMENT '青果数量',
  `breaker_count` INT DEFAULT 0 COMMENT '转色数量',
  `red_count` INT DEFAULT 0 COMMENT '成熟数量',
  `total_count` INT DEFAULT 0 COMMENT '总检测数量',
  `confidence_avg` DECIMAL(5,4) DEFAULT NULL COMMENT '平均置信度',
  `image_path` VARCHAR(512) DEFAULT NULL COMMENT '标注后图片路径',
  `image_base64` LONGTEXT DEFAULT NULL COMMENT '图片Base64编码',
  `recorded_at` DATETIME NOT NULL COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='番茄检测结果表';
```

#### 表4: `daily_statistics` — 日统计表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| `device_id` | BIGINT | FK → device.id, NOT NULL | 设备ID |
| `stat_date` | DATE | NOT NULL, UNIQUE per device | 统计日期 |
| `temp_avg` / `temp_max` / `temp_min` | DECIMAL(5,2) | | 温度统计 |
| `humidity_avg` / `humidity_max` / `humidity_min` | DECIMAL(5,2) | | 湿度统计 |
| `co2_avg` | DECIMAL(8,2) | | CO2 日均值 |
| `green_total` | INT | | 当日青果累计检测数 |
| `breaker_total` | INT | | 当日转色累计检测数 |
| `red_total` | INT | | 当日成熟累计检测数 |
| `env_score` | DECIMAL(5,2) | | 环境适宜度评分 (0~100) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | |

```sql
CREATE TABLE `daily_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `temp_avg` DECIMAL(5,2) DEFAULT NULL COMMENT '日均温度',
  `temp_max` DECIMAL(5,2) DEFAULT NULL COMMENT '最高温度',
  `temp_min` DECIMAL(5,2) DEFAULT NULL COMMENT '最低温度',
  `humidity_avg` DECIMAL(5,2) DEFAULT NULL COMMENT '日均湿度',
  `humidity_max` DECIMAL(5,2) DEFAULT NULL COMMENT '最高湿度',
  `humidity_min` DECIMAL(5,2) DEFAULT NULL COMMENT '最低湿度',
  `co2_avg` DECIMAL(8,2) DEFAULT NULL COMMENT '日均CO2浓度',
  `green_total` INT DEFAULT 0 COMMENT '青果累计检测数',
  `breaker_total` INT DEFAULT 0 COMMENT '转色累计检测数',
  `red_total` INT DEFAULT 0 COMMENT '成熟累计检测数',
  `env_score` DECIMAL(5,2) DEFAULT NULL COMMENT '环境适宜度评分(0-100)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_date` (`device_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日统计表';
```

#### 表5: `alert` — 告警记录表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 告警ID |
| `device_id` | BIGINT | FK → device.id, NOT NULL | 设备ID |
| `alert_type` | VARCHAR(32) | NOT NULL | 告警类型: `TEMPERATURE`/`HUMIDITY`/`CO2`/`SOIL`/`DEVICE` |
| `level` | TINYINT | NOT NULL | 1=info, 2=warning, 3=critical |
| `message` | VARCHAR(512) | NOT NULL | 告警描述 |
| `value` | DECIMAL(10,2) | | 触发告警的值 |
| `threshold_min` | DECIMAL(10,2) | | 正常范围下限 |
| `threshold_max` | DECIMAL(10,2) | | 正常范围上限 |
| `is_resolved` | TINYINT | DEFAULT 0 | 0=未处理, 1=已处理 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 告警时间 |
| `resolved_at` | DATETIME | | 处理时间 |

```sql
CREATE TABLE `alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `alert_type` VARCHAR(32) NOT NULL COMMENT '告警类型',
  `level` TINYINT NOT NULL COMMENT '告警级别: 1=info, 2=warning, 3=critical',
  `message` VARCHAR(512) NOT NULL COMMENT '告警描述',
  `value` DECIMAL(10,2) DEFAULT NULL COMMENT '触发值',
  `threshold_min` DECIMAL(10,2) DEFAULT NULL COMMENT '正常下限',
  `threshold_max` DECIMAL(10,2) DEFAULT NULL COMMENT '正常上限',
  `is_resolved` TINYINT DEFAULT 0 COMMENT '是否已处理: 0=否, 1=是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `created_at`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';
```

---

## 四、RESTful API 接口设计

> 基础路径: `/api/v1`
>
> 所有接口统一返回格式:
> ```json
> { "code": 200, "message": "success", "data": {} }
> ```

### 4.1 硬件端 → 服务器 (数据上报)

#### POST `/api/v1/sensor/upload` — 上传传感器数据

**请求体:**
```json
{
  "deviceCode": "TOMATO-001",
  "temperature": 25.6,
  "humidity": 72.3,
  "co2": 580.0,
  "light": 32000.0,
  "soilMoisture": 68.5,
  "soilNitrogen": 165.0,
  "soilPhosphorus": 45.0,
  "soilPotassium": 120.0
}
```

**响应:**
```json
{
  "code": 200,
  "message": "传感器数据上传成功",
  "data": { "id": 10001, "recordedAt": "2026-05-29 14:30:00" }
}
```

#### POST `/api/v1/detection/upload` — 上传检测结果

**请求体:**
```json
{
  "deviceCode": "TOMATO-001",
  "greenCount": 15,
  "breakerCount": 8,
  "redCount": 3,
  "totalCount": 26,
  "confidenceAvg": 0.8723,
  "imageBase64": "/9j/4AAQSkZJRg..."
}
```

**响应:**
```json
{
  "code": 200,
  "message": "检测结果上传成功",
  "data": { "id": 20001, "recordedAt": "2026-05-29 14:30:30" }
}
```

#### POST `/api/v1/device/heartbeat` — 设备心跳

**请求体:**
```json
{
  "deviceCode": "TOMATO-001",
  "status": 1
}
```

### 4.2 前端 → 服务器 (数据查询)

#### 传感器数据

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/sensor/latest?deviceCode=TOMATO-001` | 获取最新一条传感器数据 |
| `GET` | `/api/v1/sensor/list?deviceCode=TOMATO-001&startTime=&endTime=&page=1&size=20` | 分页查询历史数据 |
| `GET` | `/api/v1/sensor/trend?deviceCode=TOMATO-001&type=temperature&hours=24` | 获取指定指标趋势数据 |

#### 检测结果

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/detection/latest?deviceCode=TOMATO-001` | 获取最新检测结果 (含图片) |
| `GET` | `/api/v1/detection/list?deviceCode=TOMATO-001&startTime=&endTime=&page=1&size=20` | 分页查询检测历史 |
| `GET` | `/api/v1/detection/image/{id}` | 获取检测标注图片 |

**最新检测响应示例:**
```json
{
  "code": 200,
  "data": {
    "id": 20001,
    "deviceCode": "TOMATO-001",
    "greenCount": 15,
    "breakerCount": 8,
    "redCount": 3,
    "totalCount": 26,
    "confidenceAvg": 0.8723,
    "imageBase64": "data:image/jpeg;base64,...",
    "recordedAt": "2026-05-29 14:30:30"
  }
}
```

#### 统计分析

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/statistics/daily?deviceCode=TOMATO-001&date=2026-05-29` | 查询指定日期统计 |
| `GET` | `/api/v1/statistics/daily/list?deviceCode=TOMATO-001&startDate=&endDate=` | 查询日期范围统计 |
| `GET` | `/api/v1/statistics/weekly?deviceCode=TOMATO-001&startDate=` | 查询周统计 (可选) |

#### 告警管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/alert/list?deviceCode=&level=&isResolved=&page=1&size=20` | 分页查询告警 |
| `GET` | `/api/v1/alert/count?isResolved=0` | 获取未处理告警数量 |
| `PUT` | `/api/v1/alert/resolve/{id}` | 标记告警已处理 |

#### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/device/list` | 获取所有设备列表 |
| `GET` | `/api/v1/device/{deviceCode}` | 获取设备详情 |
| `POST` | `/api/v1/device` | 新增设备 |
| `PUT` | `/api/v1/device/{deviceCode}` | 更新设备信息 |

---

## 五、硬件平台选型分析

### 5.1 实时番茄检测的核心需求

| 需求 | 说明 |
|------|------|
| YOLOv8n 推理 | 输入 640×640, ~6.2 GFLOPs |
| 实时性 | 每 30 秒处理 1 帧即可 (非严格实时) |
| 传感器接入 | I2C / UART / ADC 接口 |
| 摄像头支持 | CSI 或 USB 摄像头 |
| 功耗 | 温室环境, 需考虑散热与供电 |

### 5.2 主流硬件平台对比

| 特性 | **Jetson Nano** | **树莓派 5** | **树莓派 4B** | **Orange Pi 5** |
|------|-----------------|-------------|--------------|----------------|
| CPU | 4× Cortex-A57 @ 1.43GHz | 4× Cortex-A76 @ 2.4GHz | 4× Cortex-A72 @ 1.5GHz | 4× Cortex-A76 + 4× A55 |
| GPU | **128-core Maxwell (CUDA)** | VideoCore VII | VideoCore VI | **Mali-G610 (NPU 6 TOPS)** |
| 内存 | 4GB LPDDR4 | 4/8GB LPDDR4X | 2/4/8GB LPDDR4 | 4/8/16GB LPDDR5 |
| YOLOv8n 推理速度 | **~150ms/帧 (GPU)** | ~800ms/帧 (CPU) | ~2000ms/帧 (CPU) | ~120ms/帧 (NPU) |
| CSI 摄像头 | ✅ 原生支持 | ✅ 原生支持 | ✅ 原生支持 | ✅ 支持 |
| GPIO / I2C / UART | ✅ 40-pin | ✅ 40-pin | ✅ 40-pin | ✅ 26-pin |
| 功耗 | 5~10W | 5~12W | 5~8W | 5~15W |
| 参考价格 | **¥450~600** | ¥400~550 | ¥300~450 | ¥350~500 |
| 生态 / 社区 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 深度学习支持 | PyTorch/TensorRT 原生 | ONNX Runtime | ONNX Runtime | RKNN-Toolkit |

### 5.3 推荐方案

#### ✅ 首选: Jetson Nano 4GB

**推荐理由:**

1. **CUDA 加速**: 内置 128 核 Maxwell GPU, YOLOv8n 可通过 TensorRT 优化后达到 **~50~80ms/帧**, 完全满足每 30 秒一帧的需求
2. **PyTorch 原生支持**: JetPack SDK 预装 CUDA + cuDNN, 可直接运行 `ultralytics` 的 YOLOv8, 无需额外适配
3. **CSI 摄像头**: 原生支持树莓派摄像头模块 (IMX219/IMX477), 低延迟采集
4. **40-pin GPIO**: 兼容树莓派生态的传感器扩展板, DHT22 / I2C 传感器可直接接入
5. **教育项目性价比高**: 二手/套件价格约 ¥450, 且 NVIDIA 官方文档完善

#### 备选方案

| 方案 | 适用场景 | 注意事项 |
|------|---------|---------|
| **树莓派 5 + Coral USB TPU** | 偏好树莓派生态 | USB TPU 额外 ¥300+, 总成本更高 |
| **Orange Pi 5 (RK3588S)** | 预算敏感, 可接受 NPU 适配工作 | 需使用 RKNN-Toolkit 转换模型, 调试成本高 |
| **树莓派 4B + 远程推理** | YOLO 推理放在服务器端 | 网络依赖, 适合有服务器资源的场景 |

#### 硬件清单建议

| 组件 | 型号 | 数量 | 参考价格 |
|------|------|------|----------|
| 主控板 | Jetson Nano 4GB (含散热风扇) | 1 | ¥500 |
| 摄像头 | IMX219 树莓派摄像头 (CSI) | 1 | ¥35 |
| 温湿度传感器 | DHT22 | 1 | ¥12 |
| CO2 传感器 | MH-Z19B (UART) | 1 | ¥85 |
| 光照传感器 | BH1750 (I2C) | 1 | ¥8 |
| 土壤湿度传感器 | 电容式土壤湿度传感器 (ADC) | 1 | ¥8 |
| 土壤NPK传感器 | RS485 NPK 三合一 | 1 | ¥150 |
| ADC 模块 | ADS1115 (I2C, 16-bit) | 1 | ¥12 |
| MicroSD 卡 | 64GB Class 10 | 1 | ¥40 |
| 电源 | 5V/4A DC 电源适配器 | 1 | ¥30 |
| **合计** | | | **~¥880** |

---

## 六、网站设计

### 技术栈选择

| 层 | 技术 | 版本 |
|----|------|------|
| 前端 | Vue 3 + Vite | 3.5.x / 8.x |
| UI 框架 | Element Plus (推荐) | 2.x |
| 图表 | ECharts | 5.x |
| HTTP 客户端 | Axios | 1.x |
| 后端 | Spring Boot + MyBatis | 3.0.2 / 3.0.0 |
| 数据库 | MySQL | 8.x |
| Java | OpenJDK | 25 (LTS) |

### 项目结构规划

```
web/
├── backend/src/main/java/tech/xuexinglab/demo/
│   ├── DemoApplication.java
│   ├── config/                  # 配置类
│   │   ├── CorsConfig.java      # 跨域配置
│   │   └── MyBatisConfig.java   # MyBatis 配置
│   ├── controller/              # 控制器
│   │   ├── SensorController.java
│   │   ├── DetectionController.java
│   │   ├── StatisticsController.java
│   │   ├── AlertController.java
│   │   └── DeviceController.java
│   ├── service/                 # 业务层
│   │   ├── SensorService.java
│   │   ├── DetectionService.java
│   │   ├── StatisticsService.java
│   │   ├── AlertService.java
│   │   └── DeviceService.java
│   ├── mapper/                  # MyBatis Mapper 接口
│   │   ├── SensorDataMapper.java
│   │   ├── DetectionResultMapper.java
│   │   ├── DailyStatisticsMapper.java
│   │   ├── AlertMapper.java
│   │   └── DeviceMapper.java
│   └── entity/                  # 实体类
│       ├── SensorData.java
│       ├── DetectionResult.java
│       ├── DailyStatistics.java
│       ├── Alert.java
│       └── Device.java
│
├── fronted/src/
│   ├── views/
│   │   ├── Dashboard.vue        # 实时监控面板
│   │   ├── SensorHistory.vue    # 传感器历史数据
│   │   ├── DetectionHistory.vue # 检测历史记录
│   │   ├── Statistics.vue       # 统计分析
│   │   └── AlertCenter.vue      # 告警中心
│   ├── components/
│   │   ├── SensorCard.vue       # 传感器数据卡片
│   │   ├── DetectionViewer.vue  # 检测结果查看器
│   │   ├── TrendChart.vue       # 趋势图表
│   │   └── AlertList.vue        # 告警列表
│   ├── api/                     # API 请求封装
│   │   ├── sensor.js
│   │   ├── detection.js
│   │   ├── statistics.js
│   │   └── alert.js
│   ├── router/
│   │   └── index.js
│   └── stores/                  # Pinia 状态管理
│       └── device.js
```

### 环境搭建

#### 前端环境

1. 安装 Node.js (18+)
2. 终端验证: `npm -v` / `node -v`
3. 创建项目: `npm create vue@latest` (项目名自定义, 其余选 No)
4. 安装依赖: `npm install element-plus axios echarts vue-router pinia`
5. 启动: `npm run dev`

#### 后端环境

1. 安装 JDK 25 + Maven
2. 导入 IDEA / VS Code, 等待依赖下载完成
3. 确保 MySQL 数据库 `tomato_monitor` 已创建, 执行上方建表 SQL
4. 修改 `application.properties` 中的数据库连接信息
5. 运行 `DemoApplication.java`
