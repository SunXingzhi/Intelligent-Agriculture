import cv2
import numpy as np
import os
import sys

# ================== 全局配置 ==================
REAL_BLUE_SIDE_LENGTH = 0.5          # 蓝色正方形在现实中的边长（单位：米）
TARGET_IMAGE_NAME = "image.jpg"      # 待处理的图片文件名
OUTPUT_IMAGE_NAME = "top.jpg"  # 保存结果的文件名

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
def find_robust_blue_square(image_path):
    """
    鲁棒版蓝色正方形识别（支持任意角度旋转+遮挡+手机拍摄）
    返回: (w, h, box, contour, center) 或 None
        w, h: 旋转矩形的宽和高（像素）
        box: 旋转矩形的四个顶点坐标（用于绘制）
        contour: 原始轮廓
        center: 旋转矩形中心坐标 (cx, cy)
    """
    image = cv2.imread(image_path)
    if image is None:
        print(f"错误：无法读取图片文件 {image_path}")
        return None

    height, width = image.shape[:2]

    # 预处理
    blurred = cv2.bilateralFilter(image, 9, 75, 75)
    hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
    h, s, v = cv2.split(hsv)
    v_eq = cv2.equalizeHist(v)
    hsv_eq = cv2.merge((h, s, v_eq))

    # 蓝色掩码（多段阈值）
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

        # 统一 w >= h，角度0~90
        if w < h:
            w, h = h, w
            angle = angle + 90
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
    box = np.intp(box)  # 确保坐标为整数

    print(f"✅ 成功识别蓝色正方形：宽 {w:.1f} px, 高 {h:.1f} px, 置信度 {best_score:.2f}")
    return (w, h, box, contour, (cx, cy))

# ================== 绿色不规则物体检测 ==================
def find_custom_green_object(image_path):
    """
    检测特定绿色不规则物体，使用最小外接圆包围并测量直径
    返回: (diameter, center, radius, contour) 或 None
        diameter: 外接圆直径（像素）
        center: 圆心坐标 (cx, cy)
        radius: 半径（像素）
        contour: 原始轮廓（合并后的）
    """
    image = cv2.imread(image_path)
    if image is None:
        print(f"错误：无法读取图片文件 {image_path}")
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

# ================== 主流程 ==================
def main():
    print("🔷 蓝色正方形 + 绿色物体 联合测量程序（自动模式）")
    print("=" * 50)
    print(f"已知蓝色正方形真实边长：{REAL_BLUE_SIDE_LENGTH} 米")
    print(f"待处理图片：{TARGET_IMAGE_NAME}")
    print(f"结果图片将保存为：{OUTPUT_IMAGE_NAME}")
    print("=" * 50)

    # 检查目标图片是否存在
    if not os.path.exists(TARGET_IMAGE_NAME):
        print(f"❌ 错误：当前目录下未找到文件 '{TARGET_IMAGE_NAME}'")
        sys.exit(1)

    # 1. 检测蓝色正方形，获取像素尺寸及绘制信息
    blue_info = find_robust_blue_square(TARGET_IMAGE_NAME)
    if blue_info is None:
        print("❌ 未能检测到蓝色正方形，程序退出。")
        sys.exit(1)

    w, h, blue_box, blue_contour, blue_center = blue_info
    pixel_side = (w + h) / 2
    print(f"蓝色正方形像素边长（估算）：{pixel_side:.1f} 像素")

    # 2. 计算比例尺（米/像素）
    scale = REAL_BLUE_SIDE_LENGTH / pixel_side
    print(f"比例尺：1 像素 = {scale:.6f} 米")

    # 3. 检测绿色物体
    green_info = find_custom_green_object(TARGET_IMAGE_NAME)
    if green_info is None:
        print("❌ 未能检测到绿色物体，无法计算真实直径。")
        sys.exit(1)

    green_diameter, green_center, green_radius, green_contour = green_info

    # 4. 计算真实直径
    real_diameter = green_diameter * scale
    print(f"\n🎯 绿色物体的真实直径：{real_diameter:.4f} 米 ({real_diameter*100:.2f} 厘米)")

    # 5. 绘制检测结果并保存
    img = cv2.imread(TARGET_IMAGE_NAME)
    if img is None:
        print("❌ 无法读取图片用于绘制，但测量已完成。")
        sys.exit(1)

    # 绘制蓝色正方形
    cv2.drawContours(img, [blue_box], 0, (255, 0, 0), 3)          # 蓝色旋转矩形边框
    cv2.drawContours(img, [blue_contour], -1, (255, 255, 0), 1)  # 青色原始轮廓
    cv2.circle(img, (int(blue_center[0]), int(blue_center[1])), 5, (0, 0, 255), -1)  # 红色中心点

    # 绘制绿色物体
    cv2.circle(img, green_center, green_radius, (0, 255, 0), 3)        # 绿色外接圆
    cv2.drawContours(img, [green_contour], -1, (0, 255, 255), 1)      # 黄色轮廓
    cv2.circle(img, green_center, 5, (255, 0, 0), -1)                 # 蓝色圆心

    # 保存结果
    success = cv2.imwrite(OUTPUT_IMAGE_NAME, img)
    if success:
        print(f"✅ 检测结果图片已保存为：{OUTPUT_IMAGE_NAME}")
    else:
        print(f"❌ 保存图片 {OUTPUT_IMAGE_NAME} 失败，请检查权限或路径。")

    print("=" * 50)

if __name__ == "__main__":
    main()