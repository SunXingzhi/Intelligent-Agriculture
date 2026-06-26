import serial
import struct
import sys
import time

# 帧长度常量
FRAME_LEN	= 21	# 总长度（头2 + 长度1 + 数据17 + 校验1)
DATA_LEN	= 17	# 数据区长度

def calc_checksum(frame_bytes):
	"""
	计算校验和(累加前20个字节,取低8位)
	frame_bytes: 完整的21字节帧(或前20字节)
	"""
	total = sum(frame_bytes[:20]) & 0xFF
	return total

def read_sensor_frame(ser, timeout_ms):
	"""
	从串口读取并解析一帧数据
  	ser		pyserial.Serial 对象
	timeout_ms	每个字节读取的超时时间
	return		成功返回字典,失败返回 None
	"""
	# 设置串口超时(单位秒)
	ser.timeout = timeout_ms / 1000.0

	# 同步帧头 0xAA 0x55
	found = False
	max_try = 200	# 防止无限循环
	while max_try > 0:
		byte = ser.read(1)	# 读一个字节
		# 超时或空数据
		if not byte:
			return None
		if byte[0] == 0xAA:
			# 读下一个字节确认帧头
			next_byte = ser.read(1)
			if not next_byte:
				return None
			if next_byte[0] == 0x55:
				found = True
				break
		# 不是 0x55,则丢弃 next_byte,继续找下一个 0xAA
		max_try -= 1

	if not found:
		return None

	# 读取剩余 19 个字节
	buf = bytearray(FRAME_LEN)
	buf[0] = 0xAA
	buf[1] = 0x55
	offset = 2
	while offset < FRAME_LEN:
		byte = ser.read(1)
		if not byte:
			return None
		buf[offset] = byte[0]
		offset += 1

	# 验证长度字段
	if buf[2] != 0x11:          # 固定为 17
		return None

	#  验证校验和
	if calc_checksum(buf) != buf[20]:
		return None

	#  解析数据区（小端序)
	data = bytes(buf[3:20])     # 17 字节数据区

	# 使用 struct 按小端解包
	light = struct.unpack('<I', data[0:4])[0]	# uint32_t
	co2   = struct.unpack('<H', data[4:6])[0]	# uint16_t
	tvoc  = struct.unpack('<H', data[6:8])[0]	# uint16_t
	temp  = data[8]					# uint8_t
	humi  = data[9]					# uint8_t
	soil  = data[10]				# uint8_t
	n     = struct.unpack('<h', data[11:13])[0]	# int16_t
	p     = struct.unpack('<h', data[13:15])[0]	# int16_t
	k     = struct.unpack('<h', data[15:17])[0]	# int16_t

	# 返回字典
	return {
		'light': light,
		'co2': co2,
		'tvoc': tvoc,
		'temp': temp,
		'humi': humi,
		'soil': soil,
		'n': n,
		'p': p,
		'k': k
	}

def main():
	PORT = 'COM3'
	BAUDRATE = 115200
	TIMEOUT_MS = 100

	try:
		ser = serial.Serial(PORT, BAUDRATE, timeout=0.1)
		print(f"已打开串口 {PORT}，波特率 {BAUDRATE}")
	except Exception as e:
		print(f"打开串口失败: {e}")
		sys.exit(1)

	print("开始循环接收数据，按 Ctrl+C 退出...")
	try:
		while True:
			data = read_sensor_frame(ser, TIMEOUT_MS)
			if data:
				print(data)   # 直接打印字典
			else:
				time.sleep(0.01)   # 避免CPU空转
	except KeyboardInterrupt:
		print("\n用户中断,程序退出")
	finally:
		ser.close()
		print("串口已关闭")


if __name__ == "__main__":
    main()