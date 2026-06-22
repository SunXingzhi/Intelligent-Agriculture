#ifndef  PARSE_DATA
#define  PARSE_DATA

#include <stdint.h>

// 数据区结构体（17字节）
#pragma pack(push,1)
typedef struct {
    	uint32_t 	light;     // 光照强度，小端序，单位 lx
    	uint16_t 	co2;       // 二氧化碳浓度，小端序，单位 ppm
    	uint16_t 	tvoc;      // 总挥发性有机物，小端序，单位 ppb
    	uint8_t  	temp;      // 温度，单位 ℃
    	uint8_t  	humi;      // 湿度，单位 %
    	uint8_t  	soil;      // 土壤湿度，单位 %
    	int16_t  	n;         // 氮含量，小端序，单位 mg/kg
    	int16_t  	p;         // 磷含量，小端序，单位 mg/kg
	int16_t  	k;         // 钾含量，小端序，单位 mg/kg
} sensor_data_t;
#pragma pack(pop)

// 完整帧结构体（21字节）
#pragma pack(push, 1)
typedef struct {
   	uint8_t		header1;    // 0xAA
	uint8_t		header2;    // 0x55
	uint8_t         length;     // 固定为 0x11 (17)
	sensor_data_t   data;       // 数据区
	uint8_t         checksum;   // 校验和（低8位）
} sensor_frame_t;
#pragma pack(pop)

// 帧读取函数
int read_sensor_frame(int fd, sensor_frame_t *frame, int timeout_ms);

#endif