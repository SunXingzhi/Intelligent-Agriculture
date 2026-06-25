import cv2
import numpy as np

REAL_BLUE_SIDE_LENGTH = 0.2          # 蓝色正方形真实边长（米）

def merge_contours(contours, distance_threshold=20):
    """合并距离较近的轮廓（用于绿色物体）"""
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
            distance = np.sqrt((current_center[0]-center[0])**2 + (current_center[1]-center[1])**2)
            if distance < distance_threshold:
                to_merge.append(cnt)
                contour_info.pop(i)
            else:
                i += 1
        if len(to_merge) > 1:
            merged.append(np.concatenate(to_merge))
        else:
            merged.append(current_cnt)
    return merged

def find_robust_blue_square(image):
    """检测蓝色正方形，返回(w, h, box, contour, center)或None"""
    if image is None:
        return None
    height, width = image.shape[:2]
    blurred = cv2.bilateralFilter(image, 9, 75, 75)
    hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
    h, s, v = cv2.split(hsv)
    v_eq = cv2.equalizeHist(v)
    hsv_eq = cv2.merge((h, s, v_eq))

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

    kernel_small = np.ones((3,3), np.uint8)
    kernel_large = np.ones((7,7), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel_small, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel_large, iterations=2)
    mask = cv2.dilate(mask, kernel_small, iterations=1)

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
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
            w, h = h, w
            angle += 90
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
        score = (1 - abs(aspect_ratio - 1)*0.5) * solidity * convexity
        if score > best_score and score > 0.3:
            best_score = score
            best_square = (w, h, cx, cy, contour, rect)

    if best_square is None:
        return None
    w, h, cx, cy, contour, rect = best_square
    box = cv2.boxPoints(rect)
    box = np.intp(box)
    return (w, h, box, contour, (cx, cy))

def find_custom_green_object(image):
    """检测绿色物体，返回(diameter, center, radius, contour)或None"""
    if image is None:
        return None
    height, width = image.shape[:2]
    blurred = cv2.bilateralFilter(image, 9, 75, 75)
    hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
    lower_green = np.array([30, 70, 110])
    upper_green = np.array([90, 185, 225])
    mask = cv2.inRange(hsv, lower_green, upper_green)
    kernel_small = np.ones((3,3), np.uint8)
    kernel_large = np.ones((9,9), np.uint8)
    mask = cv2.dilate(mask, kernel_large, iterations=3)
    mask = cv2.erode(mask, kernel_large, iterations=2)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel_large, iterations=2)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel_small, iterations=1)

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
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
        score = green_ratio*0.5 + solidity*0.3 + convexity*0.2
        if score > best_score and score > 0.3:
            best_score = score
            best_object = (diameter, center, radius, contour)

    if best_object is None:
        return None
    return best_object

def process_top_view(image_path):
    """
    处理俯视图，返回 (annotated_img, stem_diameter_mm)
    失败返回 (None, None)
    """
    img = cv2.imread(image_path)
    if img is None:
        print(f"❌ 无法读取图片 {image_path}")
        return None, None

    blue_info = find_robust_blue_square(img)
    if blue_info is None:
        print("❌ 未检测到蓝色正方形")
        return None, None
    w, h, blue_box, blue_contour, blue_center = blue_info

    pixel_side = (w + h) / 2
    scale = REAL_BLUE_SIDE_LENGTH / pixel_side  # 米/像素
    print(f"比例尺: 1 像素 = {scale:.6f} 米")

    green_info = find_custom_green_object(img)
    if green_info is None:
        print("❌ 未检测到绿色物体")
        return None, None
    green_diameter, green_center, green_radius, green_contour = green_info

    # 真实直径 (米) → 毫米
    real_diameter_m = green_diameter * scale
    stem_diameter_mm = real_diameter_m * 1000.0
    print(f"🎯 茎粗: {stem_diameter_mm:.2f} mm")

    # 在原图上绘制结果
    cv2.drawContours(img, [blue_box], 0, (255, 0, 0), 3)
    cv2.drawContours(img, [blue_contour], -1, (255, 255, 0), 1)
    cv2.circle(img, (int(blue_center[0]), int(blue_center[1])), 5, (0, 0, 255), -1)

    cv2.circle(img, green_center, green_radius, (0, 255, 0), 3)
    cv2.drawContours(img, [green_contour], -1, (0, 255, 255), 1)
    cv2.circle(img, green_center, 5, (255, 0, 0), -1)

    return img, stem_diameter_mm

if __name__ == "__main__":
    img, dia = process_top_view("image.jpg")
    if img is not None:
        cv2.imwrite("top_output.jpg", img)  # 可选保存
        print(f"茎粗: {dia:.2f} mm")