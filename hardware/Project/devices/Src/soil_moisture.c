//
// Created by Levi on 2026/6/6.
//
#include "soil_moisture.h"
#include "adc.h"
#include "stdio.h"

// 滑动平均滤波器（窗口 5）
#define FILTER_SIZE  5

static uint16_t filter_buf[FILTER_SIZE];
static uint8_t filter_index = 0;
static uint8_t filter_initialized = 0;

// 滑动平均滤波
static uint16_t Filter_AddValue(uint16_t new_value)
{
    uint32_t sum = 0;
    if (!filter_initialized) {
        for (int i = 0; i < FILTER_SIZE; i++) {
            filter_buf[i] = new_value;
        }
        filter_initialized = 1;
    }
    filter_buf[filter_index] = new_value;
    filter_index = (filter_index + 1) % FILTER_SIZE;
    for (int i = 0; i < FILTER_SIZE; i++) {
        sum += filter_buf[i];
    }
    return (uint16_t)(sum / FILTER_SIZE);
}

uint16_t Moisture_GetRawADC(void)
{
    uint16_t adc_val = 0;
    HAL_ADC_Start(&hadc1);
    if (HAL_ADC_PollForConversion(&hadc1, 100) == HAL_OK) {
        adc_val = HAL_ADC_GetValue(&hadc1);
    }
    HAL_ADC_Stop(&hadc1);
    return Filter_AddValue(adc_val);
}

uint8_t Moisture_GetPercentage(void)
{
    uint16_t raw = Moisture_GetRawADC();
    int16_t range = AIR_VALUE_ADC - WATER_VALUE_ADC;
    if (range <= 0) return 0;
    int16_t percent = (AIR_VALUE_ADC - raw) * 100 / range;
    if (percent < 0) percent = 0;
    if (percent > 100) percent = 100;
    return (uint8_t)percent;
}