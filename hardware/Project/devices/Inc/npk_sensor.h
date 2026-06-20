//
// Created by Levi on 2026/6/6.
//
#ifndef NPK_SENSOR_H
#define NPK_SENSOR_H

#include "stdint.h"

// 硬件配置
#define NPK_USART           &huart2     // 使用 USART2
#define NPK_RE_DE_GPIO_PORT GPIOA
#define NPK_RE_DE_PIN       GPIO_PIN_5  // 485 方向控制引脚
#define NPK_DEVICE_ADDR     0x01        // 传感器默认地址
#define NPK_BUFF_SIZE       20          // 缓冲区大小

// 状态码
typedef enum {
    NPK_OK = 0,
    NPK_ERR_TIMEOUT,
    NPK_ERR_CRC,
    NPK_ERR_FRAME,
    NPK_ERR_ADDR,
    NPK_ERR_FUNC
} NPK_StatusTypeDef;

// 读取氮磷钾暂存值（这里返回电导率估算值）
NPK_StatusTypeDef NPK_ReadNPK(int16_t *N, int16_t *P, int16_t *K);

// 写入国标测量值（写单个寄存器，功能码 0x06）
NPK_StatusTypeDef NPK_WriteN(uint16_t value);
NPK_StatusTypeDef NPK_WriteP(uint16_t value);
NPK_StatusTypeDef NPK_WriteK(uint16_t value);

#endif