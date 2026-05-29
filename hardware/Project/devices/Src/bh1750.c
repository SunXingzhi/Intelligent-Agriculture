#include "bh1750.h"

/**
 * @brief 向BH1750写入一个字节数据
 * @param reg_add: 寄存器地址
 * @param reg_dat: 要写入的数据
 * @retval None
 */
void BH1750_WriteReg(uint8_t reg_add, uint8_t reg_dat)
{
    HAL_I2C_Mem_Write(&hi2c1, BH1750_ADDR, reg_add, 1, &reg_dat, 1, 1000);
}

/**
 * @brief 从BH1750读取数据
 * @param reg_add: 寄存器地址
 * @param Read: 读取数据存储指针
 * @param num: 要读取的数据长度
 * @retval None
 */
void BH1750_ReadData(uint8_t reg_add, uint8_t *Read, uint8_t num)
{
    HAL_I2C_Mem_Read(&hi2c1, BH1750_ADDR, reg_add, 1, Read, num, 1000);
}

/**
 * @brief 初始化BH1750
 * @retval None
 */
void BH1750_Init(void)
{
    BH1750_WriteReg(BH1750_POWER_ON, 0x00);     // 上电
    BH1750_WriteReg(BH1750_CONT_H_RES, 0x00);   // 连续高分辨率模式
    HAL_Delay(180);                              // 等待测量完成
}

/**
 * @brief 读取光照强度
 * @retval 光照强度值，单位：勒克斯(lx)
 */
float BH1750_ReadLight(void)
{
    uint8_t DataBuff[2];
    float LightData;

    BH1750_WriteReg(BH1750_POWER_ON, 0x00);     // 上电
    BH1750_WriteReg(BH1750_CONT_H_RES, 0x00);   // 连续高分辨率模式
    HAL_Delay(180);                              // 等待测量完成
    BH1750_ReadData(0, DataBuff, 2);             // 读取数据
    LightData = ((DataBuff[0] << 8) + DataBuff[1]) / 1.2f; // 转换成光照强度

    return LightData;
}
