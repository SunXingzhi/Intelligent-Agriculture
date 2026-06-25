#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
番茄监测主程序 - 内存直传版本
调用顺序：传感器 → 俯视图处理+发送 → 前视图处理+发送
"""

import serial
from parse_data import read_sensor_frame
from send_env_dat import send_environment_data
from send_pla_dat import send_front_view, send_top_view
from DetectLength import process_top_view
from DetectTomato import detect_tomatoes

# ============= 配置区 =============
SERIAL_PORT = 'COM3'               # 串口（Windows）或 '/dev/ttyUSB0'（Linux）
SERIAL_BAUDRATE = 9600
TIMEOUT_MS = 2000

# 原始图像路径（根据实际文件修改）
TOP_INPUT_IMAGE = "image1.jpg"    # 俯视图原始文件
FRONT_INPUT_IMAGE = "image2.jpg"# 前视图原始文件

# 全局序号（可持久化到文件，此处每次启动从1开始）
image_counter = 1

if __name__ == "__main__":
    print("=" * 50)
    print("番茄监测系统启动")
    print("=" * 50)

    # ---- 1. 传感器数据 ----
    try:
        ser = serial.Serial(SERIAL_PORT, SERIAL_BAUDRATE, timeout=TIMEOUT_MS/1000.0)
        print(f"串口 {SERIAL_PORT} 已打开")
    except serial.SerialException as e:
        print(f"无法打开串口: {e}")
        ser = None

    if ser:
        sensor_data = read_sensor_frame(ser, TIMEOUT_MS)
        ser.close()
        if sensor_data:
            print("📊 传感器数据:", sensor_data)
            send_environment_data(sensor_data)
        else:
            print("⚠️ 未收到传感器数据，跳过发送")
    else:
        print("⚠️ 串口不可用，跳过环境数据")

    # ---- 2. 俯视图（茎粗） ----
    print("\n🔷 处理俯视图...")
    top_img, stem_mm = process_top_view(TOP_INPUT_IMAGE)
    if top_img is not None:
        print(f"茎粗: {stem_mm:.2f} mm")
        send_top_view(
            image=top_img,
            stem_diameter=stem_mm,
            image_index=image_counter
        )
        image_counter += 1
    else:
        print("❌ 俯视图处理失败，跳过发送")

    # ---- 3. 前视图（番茄） ----
    print("\n🔶 处理前视图...")
    front_img, tomato_list = detect_tomatoes(FRONT_INPUT_IMAGE)
    if front_img is not None:
        print(f"检测到 {len(tomato_list)} 个番茄")
        send_front_view(
            image=front_img,
            tomato_list=tomato_list,
            image_index=image_counter
        )
        image_counter += 1
    else:
        print("❌ 前视图处理失败，跳过发送")

    print("\n" + "=" * 50)
    print("所有任务完成")