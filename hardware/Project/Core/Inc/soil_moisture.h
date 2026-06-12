//
// Created by Levi on 2026/6/6.
//
#ifndef SOIL_MOISTURE_H
#define SOIL_MOISTURE_H

#include "stdint.h"

// 标定值：需要在你的实际环境中测量获得
// 方法：传感器放在空气中读取 ADC 平均值 -> AIR_VALUE_ADC
// 传感器完全插入清水中（不超红线）读取 ADC 平均值 -> WATER_VALUE_ADC
#define AIR_VALUE_ADC       4095
#define WATER_VALUE_ADC     2450

// 读取土壤湿度（百分比 0~100%）
// 返回：0 = 非常干, 100 = 完全饱和
uint8_t Moisture_GetPercentage(void);

// 读取原始 ADC 值（便于调试标定）
uint16_t Moisture_GetRawADC(void);

#endif