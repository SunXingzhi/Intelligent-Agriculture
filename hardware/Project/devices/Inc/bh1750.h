#ifndef __BH1750_H
#define __BH1750_H

#include "main.h"
#include "i2c.h"

/* BH1750设备地址 */
#define BH1750_ADDR 0x46

/* BH1750指令定义 */
#define BH1750_POWER_ON     0x01
#define BH1750_POWER_OFF    0x00
#define BH1750_RESET        0x07
#define BH1750_CONT_H_RES   0x10  // 连续高分辨率模式
#define BH1750_CONT_H_RES2  0x11  // 连续高分辨率模式2
#define BH1750_CONT_L_RES   0x13  // 连续低分辨率模式
#define BH1750_ONE_H_RES    0x20  // 一次高分辨率模式
#define BH1750_ONE_H_RES2   0x21  // 一次高分辨率模式2
#define BH1750_ONE_L_RES    0x23  // 一次低分辨率模式

/* 函数声明 */
void BH1750_WriteReg(uint8_t reg_add, uint8_t reg_dat);
void BH1750_ReadData(uint8_t reg_add, uint8_t *Read, uint8_t num);
void BH1750_Init(void);
float BH1750_ReadLight(void);

#endif /* __BH1750_H */
