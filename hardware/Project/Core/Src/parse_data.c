#include <stdint.h>
#include <sys/select.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>

#include "parse_data.h"

// 校验与计算
static uint8_t calc_checksum(const sensor_frame_t *frame){
	const uint8_t	*p = (const uint8_t*)frame;
	uint32_t	sum = 0;
	// 只计算前20个字节（header1+header2+length+data，不包含checksum本身）
	for (int i = 0; i < 20; i++) {
		sum += p[i];
	}
	return (uint8_t)(sum & 0xFF);
}

// 带超时的单字节读取
static int read_byte_with_timeout(int fd, uint8_t *byte, int timeout_ms) {
	fd_set 		set;
	struct timeval 	tv;

	FD_ZERO(&set);
	FD_SET(fd, &set);
	tv.tv_sec  = timeout_ms / 1000;
	tv.tv_usec = (timeout_ms % 1000) * 1000;

	int ret = select(fd + 1, &set, NULL, NULL, &tv);
	if (ret < 0) {
		return -1;	// select 出错
	} else if (ret == 0) {
		return 0;	// 超时
	} else {
		if (read(fd, byte, 1) == 1) {
			return 1；	// 成功读取一个字节
		} else {
			return -1;	// read 失败
		}
	}
}

/* 参数：
fd          已打开的串口文件描述符
frame       输出参数，存放解析后的完整帧
timeout_ms  整个帧接收的超时时间
返回值：0 成功，-1 失败
*/
int read_sensor_frame(int fd, sensor_frame_t *frame, int timeout_ms){
	uint8_t     buf[sizeof(sensor_frame_t)];
	int         offset = 0;
	int         total_len = sizeof(sensor_frame_t);   // 21字节
	uint8_t     byte;
	int         found = 0;
	int         start_time = 0;  // 这里简化为每次重试重置超时，实际可记录开始时间
	int         max_try = 200;   // 尝试最多200个字节，防止无限循环
	int         ret;

	while (max_try-- > 0) {
		ret = read_byte_with_timeout(fd, &byte, timeout_ms);
		if (ret <= 0) {
			return -1;   // 超时或出错
		}
		// 寻找帧头0xAA 0x55
		if (byte == 0xAA) {
			// 再读下一个看是不是0x55
			ret = read_byte_with_timeout(fd, &byte, timeout_ms);
			if (ret <= 0) {
				return -1;
			}
			else if (byte == 0x55) {
				found = 1;
				break;
			}
			continue;
		}
	}

	if (!found) {
		return -1; // 未找到帧头
	}

	// 帧头放在buf前两个位置
	buf[0] = 0xAA;
	buf[1] = 0x55;
	offset = 2;

	// 读取剩余的 19 个字节（长度1 + 数据17 + 校验和1）
	while (offset < total_len) {
		ret = read_byte_with_timeout(fd, &byte, timeout_ms);
		if (ret <= 0) {
			return -1;
		}
		buf[offset++] = byte;
	}

	// 解析到结构体
	memcpy(frame, buf, total_len);

	// 验证长度字段（应为0x11）
	if (frame->length != 0x11) {
		return -1;
	}

	// 验证校验和
	uint8_t calc = calc_checksum(frame);
	if (calc != frame->checksum) {
		return -1;
	}

	// 一切正常
	return 0;
}
