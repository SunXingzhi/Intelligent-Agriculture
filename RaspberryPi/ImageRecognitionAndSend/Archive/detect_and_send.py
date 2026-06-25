#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
番茄监测系统 - 识别并发送模块（内存直传版）
原 final2.py + send_pic.py 合并优化：
1. 识别蓝色正方形与绿色物体，计算实际直径。
2. 绘制结果图像（内存中的 numpy 数组）。
3. 直接编码为 JPEG 字节流 → Base64 → HTTP 发送（无磁盘写入）。
4. 可选保存结果到硬盘（方便本地查看）。
"""

import cv2
import numpy as np
import requests
import base64
from datetime import datetime
import os
import sys

# ================== 全局配置 ==================
REAL_BLUE_SIDE_LENGTH = 0.2          # 蓝色正方形真实边长（米）
INPUT_IMAGE_NAME = "image.jpg"       # 待处理的俯视图输入文件
SAVE_RESULT = False                   # 是否将结果图存入磁盘（调试用）
RESULT_IMAGE_NAME = "top.jpg"        # 结果图文件名（仅当 SAVE_RESULT=True）
SERVER_IP = "10.135.164.87"         # 服务器 IP

# ================== 工具函数 ==================
def merge_contours(contours, distance_threshold=20):
    """合并距离较近的多个轮廓（用于绿色不规则物体）"""
    if len(contours) <= 1:
        return contours

    merged = []
    contour_info = []
    for cnt in contours:
        M = cv2.moments(cnt)
        if M["m00"] == 0:
            continue
        cx = int(M["m10"] / M["m00"])
        cy = int(M["m01"] / M["m00"])
        area = cv2.contourArea(cnt)
        contour_info.append((cnt, (cx, cy), area))

    contour_info.sort(key=lambda x: x[2], reverse=True)

    while contour_info:
        current_cnt, current_center, current_area = contour_info.pop(0)
        to_merge = [current_cnt]

        i = 0
        while i < len(contour_info):
            cnt, center, area = contour_info[i]
            distance = np.sqrt((current_center[0] - center[0])**2 +
                              (current_center[1] - center[1])**2)
            if distance < distance_threshold:
                to_merge.append(cnt)
                contour_info.pop(i)
            else:
                i += 1

        if len(to_merge) > 1:
            merged_cnt = np.concatenate(to_merge)
            merged.append(merged_cnt)
        else:
            merged.append(current_cnt)

    return merged

# ================== 蓝色正方形检测 ==================
def find_robust_blue_square(image):
    """鲁棒版蓝色正方形识别，返回 (w, h, box, contour, center) 或 None"""
    if image is None:
        return None

    height, width = image.shape[:2]

    # 预处理
    blurred = cv2.bilateralFilter(image, 9, 75, 75)
    hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
    h, s, v = cv2.split(hsv)
    v_eq = cv2.equalizeHist(v)
    hsv_eq = cv2.merge((h, s, v_eq))

    # 多段蓝色阈值
    lower_blue1 = np.array([85, 35, 35])
    upper_blue1 = np.array([105, 255, 255])
    lower_blue2 = np.array([100, 40, 40])
    upper_blue2 = np.array([130, 255, 255])
    lower_blue3 = np.array([125, 50, 30])
    upper_blue3 = np.array([145, 255, 200])
    mask1 = cv2.inRange(hsv, lower_blue1, upper_blue1)
    mask2 = cv2.inRange(hsv, lower_blue2, upper_blue2)
    mask3 = cv2.inRange(hsv, lower_blue3, upper_blue3)
    mask = cv2.bitwise_or(cv2.bitwise_or(mask1, mask2), mask3)

    # 形态学操作
    kernel_small = np.ones((3, 3), np.uint8)
    kernel_large = np.ones((7, 7), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel_small, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel_large, iterations=2)
    mask = cv2.dilate(mask, kernel_small, iterations=1)

    # 轮廓筛选
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        print("未检测到任何蓝色区域")
        return None

    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]
    best_square = None
    best_score = 0

    for contour in contours:
        area = cv2.contourArea(contour)
        min_area = 0.001 * height * width
        max_area = 0.9 * height * width
        if area < min_area or area > max_area:
            continue

        rect = cv2.minAreaRect(contour)
        (cx, cy), (w, h), angle = rect

        if w < h:
            w, h, angle = h, w, angle + 90
        angle = angle % 90

        rect_area = w * h
        solidity = area / rect_area
        if solidity < 0.4:
            continue

        aspect_ratio = w / h
        if aspect_ratio > 1.6:
            continue

        perimeter = cv2.arcLength(contour, True)
        if perimeter == 0:
            continue
        compactness = (perimeter ** 2) / (4 * np.pi * area)
        if compactness > 2.5:
            continue

        hull = cv2.convexHull(contour)
        hull_area = cv2.contourArea(hull)
        convexity = area / hull_area
        if convexity < 0.55:
            continue

        score = (1 - abs(aspect_ratio - 1) * 0.5) * solidity * convexity
        if score > best_score and score > 0.3:
            best_score = score
            best_square = (w, h, cx, cy, contour, rect)

    if best_square is None:
        print("未检测到符合条件的蓝色正方形")
        return None

    w, h, cx, cy, contour, rect = best_square
    box = cv2.boxPoints(rect)
    box = np.intp(box)
    print(f"✅ 成功识别蓝色正方形：宽 {w:.1f} px, 高 {h:.1f} px, 置信度 {best_score:.2f}")
    return (w, h, box, contour, (cx, cy))

# ================== 绿色不规则物体检测 ==================
def find_custom_green_object(image):
    """
    检测特定绿色不规则物体，使用最小外接圆包围并测量直径
    返回: (diameter, center, radius, contour) 或 None
    """
    if image is None:
        return None

    height, width = image.shape[:2]
    blurred = cv2.bilateralFilter(image, 9, 75, 75)
    hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)

    lower_green = np.array([30, 70, 110])
    upper_green = np.array([90, 185, 225])
    mask = cv2.inRange(hsv, lower_green, upper_green)

    kernel_small = np.ones((3, 3), np.uint8)
    kernel_large = np.ones((9, 9), np.uint8)
    mask = cv2.dilate(mask, kernel_large, iterations=3)
    mask = cv2.erode(mask, kernel_large, iterations=2)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel_large, iterations=2)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel_small, iterations=1)

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        print("未检测到任何绿色区域")
        return None

    contours = merge_contours(contours, distance_threshold=30)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    best_object = None
    best_score = 0

    for contour in contours:
        area = cv2.contourArea(contour)
        min_area = 0.004 * height * width
        max_area = 0.95 * height * width
        if area < min_area or area > max_area:
            continue

        (cx, cy), radius = cv2.minEnclosingCircle(contour)
        center = (int(cx), int(cy))
        radius = int(radius)
        diameter = 2 * radius

        circle_area = np.pi * radius ** 2
        solidity = area / circle_area
        if solidity < 0.15:
            continue

        contour_mask = np.zeros(mask.shape, np.uint8)
        cv2.drawContours(contour_mask, [contour], -1, 255, -1)
        green_pixels = cv2.countNonZero(cv2.bitwise_and(mask, contour_mask))
        green_ratio = green_pixels / cv2.countNonZero(contour_mask)
        if green_ratio < 0.4:
            continue

        hull = cv2.convexHull(contour)
        hull_area = cv2.contourArea(hull)
        convexity = area / hull_area
        if convexity < 0.25:
            continue

        score = green_ratio * 0.5 + solidity * 0.3 + convexity * 0.2
        if score > best_score and score > 0.3:
            best_score = score
            best_object = (diameter, center, radius, contour)

    if best_object is None:
        print("未检测到符合条件的绿色物体")
        return None

    diameter, center, radius, contour = best_object
    print(f"✅ 成功检测绿色物体：外接圆直径 {diameter} px, 置信度 {best_score:.2f}")
    return best_object

# ================== 图像处理主逻辑（返回内存图像 + 结果） ==================
def process_top_view(input_path):
    """
    处理俯视图，返回处理后的图像 (numpy BGR) 和真实直径 (米)。
    若失败，返回 (None, None)。
    """
    img = cv2.imread(input_path)
    if img is None:
        print(f"❌ 无法读取图片：{input_path}")
        return None, None

    # 1. 检测蓝色正方形
    blue_info = find_robust_blue_square(img)
    if blue_info is None:
        print("❌ 未能检测到蓝色正方形")
        return None, None
    w, h, blue_box, blue_contour, blue_center = blue_info

    pixel_side = (w + h) / 2
    scale = REAL_BLUE_SIDE_LENGTH / pixel_side
    print(f"比例尺：1 像素 = {scale:.6f} 米")

    # 2. 检测绿色物体
    green_info = find_custom_green_object(img)
    if green_info is None:
        print("❌ 未能检测到绿色物体")
        return None, None
    green_diameter, green_center, green_radius, green_contour = green_info

    real_diameter = green_diameter * scale
    print(f"🎯 绿色物体的真实直径：{real_diameter:.4f} 米 ({real_diameter*100:.2f} 厘米)")

    # 3. 在原图上绘制结果
    cv2.drawContours(img, [blue_box], 0, (255, 0, 0), 3)
    cv2.drawContours(img, [blue_contour], -1, (255, 255, 0), 1)
    cv2.circle(img, (int(blue_center[0]), int(blue_center[1])), 5, (0, 0, 255), -1)

    cv2.circle(img, green_center, green_radius, (0, 255, 0), 3)
    cv2.drawContours(img, [green_contour], -1, (0, 255, 255), 1)
    cv2.circle(img, green_center, 5, (255, 0, 0), -1)

    return img, real_diameter

# ================== HTTP 发送函数（支持内存图像） ==================
def send_image_from_bytes(image_bytes, image_type, server_ip="172.16.100.136"):
    """
    将内存中的 JPEG/PNG 字节串发送到服务器。
    image_bytes: 图像文件的二进制数据（如 cv2.imencode 结果）
    image_type: "front_view" 或 "top_view"
    server_ip: 服务器 IP
    """
    url = f"http://{server_ip}:8081/api/image/upload"

    # 转 Base64
    image_b64 = base64.b64encode(image_bytes).decode('utf-8')
    print(f"✅ 图像编码完成：{len(image_b64)} 字符")

    message = {
        "type": image_type,
        "imageData": image_b64,
        "timestamp": datetime.now().isoformat(),
        "deviceId": "raspberry_pi_001"
    }

    headers = {"Content-Type": "application/json"}
    print(f"📡 正在上传 {image_type} ...")

    try:
        response = requests.post(url, json=message, headers=headers, timeout=30)
        if response.status_code == 200:
            print(f"✅ 上传成功：{response.json()}")
            return True
        else:
            print(f"❌ 上传失败：HTTP {response.status_code} - {response.text}")
            return False
    except requests.exceptions.ConnectionError:
        print(f"❌ 无法连接服务器 {server_ip}，请检查网络或服务器状态")
        return False
    except requests.exceptions.Timeout:
        print("❌ 上传超时")
        return False
    except Exception as e:
        print(f"❌ 发送异常：{e}")
        return False

def send_image_from_file(image_path, image_type, server_ip="172.16.100.136"):
    """兼容旧接口：从文件读取并发送"""
    try:
        with open(image_path, "rb") as f:
            image_bytes = f.read()
        return send_image_from_bytes(image_bytes, image_type, server_ip)
    except Exception as e:
        print(f"❌ 读取文件失败：{image_path} - {e}")
        return False

# ================== 主流程 ==================
def main():
    print("=" * 50)
    print("番茄监测系统 - 图像识别与发送（内存直传模式）")
    print("=" * 50)

    # ---- 1. 处理俯视图 ----
    if not os.path.exists(INPUT_IMAGE_NAME):
        print(f"❌ 输入图像 {INPUT_IMAGE_NAME} 不存在，程序退出")
        sys.exit(1)

    processed_img, real_diameter = process_top_view(INPUT_IMAGE_NAME)
    if processed_img is None:
        print("❌ 图像处理失败，发送取消")
        sys.exit(1)

    # ---- 2. 可选保存到硬盘（调试用） ----
    if SAVE_RESULT:
        success = cv2.imwrite(RESULT_IMAGE_NAME, processed_img)
        if success:
            print(f"💾 结果图像已保存至：{RESULT_IMAGE_NAME}")
        else:
            print(f"❌ 保存图像失败")

    # ---- 3. 将结果图像编码为 JPEG 字节流（在内存中） ----
    ret, jpeg_bytes = cv2.imencode('.jpg', processed_img)
    if not ret:
        print("❌ 图像编码为 JPEG 失败")
        sys.exit(1)
    image_bytes = jpeg_bytes.tobytes()

    # ---- 4. 发送 top_view ----
    print("\n📷 发送俯视图 (top_view)...")
    send_image_from_bytes(image_bytes, "top_view", SERVER_IP)

    # ---- 5. 发送平视图 (front_view) ----
    # 这里根据你的实际情况：如果 front.jpg 已经存在，可直接从文件发送（仅读一次）；
    # 若未来平视图也由程序生成，可类似地用内存方式发送。
    front_image = "front.jpg"
    if os.path.exists(front_image):
        print("\n📷 发送平视图 (front_view)...")
        send_image_from_file(front_image, "front_view", SERVER_IP)
    else:
        print(f"⚠️ 未找到平视图 {front_image}，跳过发送")

    print("\n" + "=" * 50)
    print("✅ 所有任务完成")

if __name__ == "__main__":
    main()