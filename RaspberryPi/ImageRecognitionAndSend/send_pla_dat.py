#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
图片及识别数据上传模块（支持内存图像）
"""

import requests
import base64
import json
from datetime import datetime
import cv2

BACKEND_URL = "http://172.16.100.54:8081/api/image/upload"

def image_to_base64(image):
    """转换图像为Base64：支持文件路径(str)或numpy数组(BGR)"""
    if isinstance(image, str):
        with open(image, "rb") as f:
            return base64.b64encode(f.read()).decode("utf-8")
    else:
        # numpy数组 -> JPEG字节流 -> Base64
        ret, jpeg = cv2.imencode('.jpg', image)
        if not ret:
            raise ValueError("图像编码为JPEG失败")
        return base64.b64encode(jpeg.tobytes()).decode("utf-8")

def send_front_view(image, tomato_list, device_alias="front_view", image_index=None):
    """
    发送前视图（番茄检测结果）
    :param image: 文件路径或numpy数组
    :param tomato_list: [{"ripeness":"fully_ripened","confidence":0.92}, ...]
    :param device_alias: 必须以 "front" 开头
    :param image_index: 照片序号 (可选)
    """
    img_b64 = image_to_base64(image)
    payload = {
        "deviceAlias": device_alias,
        "imageData": img_b64,
        "timestamp": datetime.now().isoformat(),
        "tomatoList": tomato_list
    }
    if image_index is not None:
        payload["imageIndex"] = image_index

    print(f"----- 发送前视图 -----")
    print(f"  设备别名: {device_alias}")
    if image_index is not None:
        print(f"  序号: {image_index}")
    print(f"  番茄个数: {len(tomato_list)}")
    try:
        resp = requests.post(BACKEND_URL, json=payload, timeout=10)
        resp.raise_for_status()
        result = resp.json()
        print("✅ 服务器响应:", json.dumps(result, indent=2, ensure_ascii=False))
        return result
    except requests.exceptions.RequestException as e:
        print("❌ 前视图发送失败:", e)
        return None

def send_top_view(image, stem_diameter, device_alias="top_view", image_index=None):
    """
    发送俯视图（茎粗检测结果）
    :param image: 文件路径或numpy数组
    :param stem_diameter: 植株直径 (单位mm)
    :param device_alias: 必须以 "top" 开头
    :param image_index: 照片序号 (可选)
    """
    img_b64 = image_to_base64(image)
    payload = {
        "deviceAlias": device_alias,
        "imageData": img_b64,
        "timestamp": datetime.now().isoformat(),
        "stemDiameter": stem_diameter
    }
    if image_index is not None:
        payload["imageIndex"] = image_index

    print(f"----- 发送俯视图 -----")
    print(f"  设备别名: {device_alias}")
    if image_index is not None:
        print(f"  序号: {image_index}")
    print(f"  茎粗: {stem_diameter} mm")
    try:
        resp = requests.post(BACKEND_URL, json=payload, timeout=10)
        resp.raise_for_status()
        result = resp.json()
        print("✅ 服务器响应:", json.dumps(result, indent=2, ensure_ascii=False))
        return result
    except requests.exceptions.RequestException as e:
        print("❌ 俯视图发送失败:", e)
        return None

if __name__ == "__main__":
    # 测试（需front.jpg、top.jpg存在）
    tomato_list = [{"ripeness":"fully_ripened","confidence":0.95}]
    send_front_view("front.jpg", tomato_list, image_index=1)
    send_top_view("top.jpg", 13.2, image_index=1)