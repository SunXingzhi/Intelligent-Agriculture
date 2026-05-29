#include "sgp30.h"

//**********************************操作函数****************************

//初始化SGP30（使用HAL库I2C2）
void SGP30_Init(void)
{
	SGP30_ad_write(0x20, 0x03);
}

//向SGP30写入两个字节（使用HAL库）
void SGP30_ad_write(uint8_t a, uint8_t b)
{
	uint8_t data[2];
	data[0] = a;
	data[1] = b;

	HAL_I2C_Master_Transmit(&hi2c2, SGP30_ADDR << 1, data, 2, HAL_MAX_DELAY);
	HAL_Delay(100);
}

//从SGP30读取数据（使用HAL库）
uint32_t SGP30_ad_read(void)
{
	uint32_t dat;
	uint8_t buf[5];

	HAL_I2C_Master_Receive(&hi2c2, SGP30_ADDR << 1, buf, 5, HAL_MAX_DELAY);

	// 前两个字节是数据，第三个是CRC，后两个是数据
	dat = (uint32_t)buf[0] << 24;
	dat |= (uint32_t)buf[1] << 16;
	// buf[2]是CRC，跳过
	dat |= (uint32_t)buf[3] << 8;
	dat |= (uint32_t)buf[4];

	return dat;
}
