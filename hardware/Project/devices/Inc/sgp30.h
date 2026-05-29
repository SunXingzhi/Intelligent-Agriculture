#ifndef __SGP30_H__
#define __SGP30_H__

#include "main.h"
#include "i2c.h"

// SGP30 I2C地址（7位地址格式，HAL库使用）
#define SGP30_ADDR  0x58  // 0xb0 >> 1 = 0x58

void SGP30_Init(void);
void SGP30_ad_write(uint8_t a, uint8_t b);
uint32_t SGP30_ad_read(void);

#endif
