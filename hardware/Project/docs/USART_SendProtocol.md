# USART1 串口数据发送协议

## 1. 数据帧格式

采用 **帧头 + 数据长度 + 数据体 + 校验和** 格式：

```
┌──────┬──────┬──────┬──────────────────────────────┬──────┐
│ 0xAA │ 0x55 │ 0x12 │  data (18 bytes)             │ SUM  │
│ 帧头 │ 帧头 │ 长度 │  传感器数据                   │ 校验 │
└──────┴──────┴──────┴──────────────────────────────┴──────┘
```

总帧长度：2 + 1 + 18 + 1 = **22 字节**

---

## 2. 数据字段定义（小端序）

| 偏移 | 字节数 | 变量           | 类型      | 说明           |
|------|--------|----------------|-----------|----------------|
| 0    | 1      | 帧头1          | uint8_t   | 固定 0xAA      |
| 1    | 1      | 帧头2          | uint8_t   | 固定 0x55      |
| 2    | 1      | 数据长度       | uint8_t   | 固定 0x12 (18) |
| 3-6  | 4      | LightData_Hex  | uint32_t  | 光照强度 (lx)  |
| 7-8  | 2      | co2Data        | uint16_t  | CO2 浓度 (ppm) |
| 9-10 | 2      | TVOCData       | uint16_t  | TVOC (ppb)     |
| 11   | 1      | temperature    | uint8_t   | 温度 (°C)      |
| 12   | 1      | humidity       | uint8_t   | 湿度 (%)       |
| 13-14| 2      | moisture       | uint16_t  | 土壤湿度       |
| 15-16| 2      | N              | int16_t   | 氮含量         |
| 17-18| 2      | P              | int16_t   | 磷含量         |
| 19-20| 2      | K              | int16_t   | 钾含量         |
| 21   | 1      | SUM            | uint8_t   | 校验和         |

---

## 3. while 循环中的发送代码

```c
// ====== 帧头 + 长度 ======
aTXbuf[0] = 0xAA;
aTXbuf[1] = 0x55;
aTXbuf[2] = 0x12;  // 18字节数据

// ====== 光照 (4字节, 小端序) ======
aTXbuf[3]  = (uint8_t)(LightData_Hex & 0xFF);
aTXbuf[4]  = (uint8_t)((LightData_Hex >> 8) & 0xFF);
aTXbuf[5]  = (uint8_t)((LightData_Hex >> 16) & 0xFF);
aTXbuf[6]  = (uint8_t)((LightData_Hex >> 24) & 0xFF);

// ====== CO2 (2字节) ======
aTXbuf[7]  = (uint8_t)(co2Data & 0xFF);
aTXbuf[8]  = (uint8_t)((co2Data >> 8) & 0xFF);

// ====== TVOC (2字节) ======
aTXbuf[9]  = (uint8_t)(TVOCData & 0xFF);
aTXbuf[10] = (uint8_t)((TVOCData >> 8) & 0xFF);

// ====== 温度、湿度 ======
aTXbuf[11] = temperature;
aTXbuf[12] = humidity;

// ====== 土壤湿度 (2字节) ======
aTXbuf[13] = (uint8_t)(moisture & 0xFF);
aTXbuf[14] = (uint8_t)((moisture >> 8) & 0xFF);

// ====== N (2字节, int16_t 有符号) ======
aTXbuf[15] = (uint8_t)(N & 0xFF);
aTXbuf[16] = (uint8_t)((N >> 8) & 0xFF);

// ====== P (2字节) ======
aTXbuf[17] = (uint8_t)(P & 0xFF);
aTXbuf[18] = (uint8_t)((P >> 8) & 0xFF);

// ====== K (2字节) ======
aTXbuf[19] = (uint8_t)(K & 0xFF);
aTXbuf[20] = (uint8_t)((K >> 8) & 0xFF);

// ====== 校验和 (帧头 + 长度 + 数据 累加和的低8位) ======
uint8_t sum = 0;
for (int i = 0; i < 21; i++) {
    sum += aTXbuf[i];
}
aTXbuf[21] = sum;

// ====== 发送 ======
HAL_UART_Transmit(&huart1, aTXbuf, 22, HAL_MAX_DELAY);
```

---

## 4. 变量声明

```c
uint8_t   aTXbuf[32];       // 串口发送缓存 (实际用22字节，32留余量)
uint32_t  LightData_Hex;    // 光照强度 (lx)
uint16_t  co2Data;          // CO2 浓度 (ppm)
uint16_t  TVOCData;         // TVOC (ppb)
uint8_t   temperature;      // 温度 (°C)
uint8_t   humidity;         // 湿度 (%)
uint16_t  moisture;         // 土壤湿度
int16_t   N;                // 氮
int16_t   P;                // 磷
int16_t   K;                // 钾
```

---

## 5. 上位机解析逻辑

1. 扫描连续的 `0xAA 0x55` 作为帧头
2. 读取下一字节获取数据长度 `L`
3. 继续读取 `L` 字节数据体
4. 读取 1 字节校验和
5. 验证校验和：帧头 + 长度 + 数据体 累加和的低8位 == 校验和
6. 按偏移解析各传感器值（小端序）

---

## 6. 扩展说明

- 后续补充 N/P/K 驱动函数后，直接在读取位置调用，发送代码无需改动
- 若增加新传感器，修改长度字节并追加数据，同时扩大 `aTXbuf` 数组
