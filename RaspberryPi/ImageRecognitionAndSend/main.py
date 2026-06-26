#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
番茄监测主程序 - 双摄像头实时采集版
依次执行：传感器数据 → 俯视图采集/处理/发送 → 前视图采集/处理/发送
"""

import serial
import cv2
from parse_data import read_sensor_frame
from send_env_dat import send_environment_data
from send_pla_dat import send_front_view, send_top_view
from DetectLength import process_top_view
from DetectTomato import detect_tomatoes

# ============= 配置区 =============
SERIAL_PORT = 'COM3'                # 传感器串口（Windows例），Linux如 '/dev/ttyUSB0'
SERIAL_BAUDRATE = 9600
TIMEOUT_MS = 2000

# 两个USB摄像头索引（通常先插为0，后插为1，请根据实际调整）
TOP_CAM_INDEX = 0                   # 俯视图摄像头
FRONT_CAM_INDEX = 1                 # 前视图摄像头

# 摄像头分辨率（可选，设为None则使用默认）
FRAME_WIDTH = None
FRAME_HEIGHT = None

# 全局序号（如需持久化可保存至文件）
image_counter = 124

def capture_from_camera(cam_index, width=None, height=None):
    """
    从指定摄像头捕获一帧图像
    返回: numpy数组 (BGR) 或 None
    """
    cap = cv2.VideoCapture(cam_index)
    if not cap.isOpened():
        print(f"❌ 无法打开摄像头 {cam_index}")
        return None

    # 设置分辨率（如果指定）
    if width and height:
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

    # 预热：丢弃头几帧
    for _ in range(5):
        cap.read()

    ret, frame = cap.read()
    cap.release()

    if not ret:
        print(f"❌ 摄像头 {cam_index} 捕获画面失败")
        return None
    print(f"📸 摄像头 {cam_index} 捕获一帧，尺寸: {frame.shape[1]}x{frame.shape[0]}")
    return frame

if __name__ == "__main__":
    print("=" * 50)
    print("番茄监测系统启动（双摄像头模式）")
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
    print("\n🔷 采集俯视图...")
    top_frame = capture_from_camera(TOP_CAM_INDEX, FRAME_WIDTH, FRAME_HEIGHT)
    if top_frame is not None:
        top_img, stem_mm = process_top_view(top_frame)   # 直接传入数组
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
    else:
        print("❌ 俯视图摄像头采集失败，跳过")

    # ---- 3. 前视图（番茄） ----
    print("\n🔶 采集前视图...")
    front_frame = capture_from_camera(FRONT_CAM_INDEX, FRAME_WIDTH, FRAME_HEIGHT)
    if front_frame is not None:
        front_img, tomato_list = detect_tomatoes(front_frame)
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
    else:
        print("❌ 前视图摄像头采集失败，跳过")

    print("\n" + "=" * 50)
    print("所有任务完成")