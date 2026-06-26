#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
番茄成熟度检测模块（支持文件路径或numpy数组输入）
"""

from ultralytics import YOLO
import cv2

CLASS_NAMES = {
    0: "fully_ripened",
    1: "green",
    2: "half_ripened"
}

_model = None

def get_model():
    global _model
    if _model is None:
        # **** 请修改为你的模型路径 ****
        model_path = 'D:/mydataset2/runs/detect/tomato_train/weights/best.pt'
        _model = YOLO(model_path)
    return _model

def detect_tomatoes(source):
    """
    检测番茄，返回 (annotated_img, tomato_list)
    source: 文件路径(str) 或 numpy数组(BGR)
    """
    model = get_model()
    
    # 如果传入的是文件路径，YOLO可直接处理；若是数组，同样支持
    results = model.predict(source=source, conf=0.25, save=False, workers=0)
    
    if not results:
        # 返回原始图像（如果source是路径则读取，否则直接返回source）
        if isinstance(source, str):
            img = cv2.imread(source)
        else:
            img = source
        return img, []

    result = results[0]
    boxes = result.boxes

    tomato_list = []
    if boxes is not None and len(boxes) > 0:
        for box in boxes:
            cls_id = int(box.cls[0].item())
            ripeness = CLASS_NAMES.get(cls_id, "unknown")
            conf = box.conf[0].item()
            tomato_list.append({"ripeness": ripeness, "confidence": conf})

    annotated_img = result.plot()   # 内置标注绘制，返回numpy数组
    return annotated_img, tomato_list

if __name__ == "__main__":
    img, tomatos = detect_tomatoes("front_input.jpg")
    if img is not None:
        cv2.imwrite("front_output.jpg", img)
        print(f"检测到 {len(tomatos)} 个番茄")
        for i, t in enumerate(tomatos, 1):
            print(f"  {i}. {t['ripeness']} (置信度 {t['confidence']:.2f})")