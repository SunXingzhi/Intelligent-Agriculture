import requests
import base64
from datetime import datetime

def send_image(image_path, image_type, server_ip="172.16.100.136"):
    """发送图片到服务器"""
    
    url = f"http://{server_ip}:8081/api/image/upload"
    
    # 1. 读取图片并转为Base64
    try:
        with open(image_path, "rb") as image_file:
            image_data = base64.b64encode(image_file.read()).decode('utf-8')
        print(f"✅ 图片读取成功: {image_path}")
    except Exception as e:
        print(f"❌ 图片读取失败: {e}")
        return False
    
    # 2. 构造消息
    message = {
        "type": image_type,  # "front_view" 或 "top_view"
        "imageData": image_data,
        "timestamp": datetime.now().isoformat(),
        "deviceId": "raspberry_pi_001"
    }
    
    # 3. 发送到服务器
    headers = {"Content-Type": "application/json"}
    
    print(f"📡 连接地址: {url}")
    print(f"📷 图片类型: {image_type}")
    print(f"📊 数据大小: {len(image_data)} 字符")
    
    try:
        print("⏳ 正在上传图片...")
        response = requests.post(url, json=message, headers=headers, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ 图片上传成功!")
            print(f"📋 服务器响应: {result}")
            return True
        else:
            print(f"❌ 上传失败: HTTP {response.status_code}")
            print(f"📋 错误信息: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError as e:
        print(f"❌ 连接失败: {e}")
        print("\n🔍 请检查:")
        print(f"1. 服务器IP是否正确: {server_ip}")
        print("2. 服务器Spring Boot是否运行")
        print("3. 防火墙是否允许8081端口")
        return False
        
    except requests.exceptions.Timeout:
        print("❌ 上传超时")
        print("图片太大或网络太慢")
        return False
        
    except Exception as e:
        print(f"❌ 发生错误: {e}")
        return False

def send_both_views(server_ip="172.16.100.136"):
    """发送平视图和俯视图"""
    
    print("=" * 50)
    print("番茄监测系统 - 图片上传")
    print("=" * 50)
    
    # 发送平视图
    print("\n📷 上传平视图...")
    send_image("front.jpg", "front_view", server_ip)
    
    # 发送俯视图
    print("\n📷 上传俯视图...")
    send_image("top.jpg", "top_view", server_ip)
    
    print("=" * 50)

if __name__ == "__main__":
    # 使用你的服务器IP
    server_ip = "172.16.100.136"
    
    # 发送两张图片
    send_both_views(server_ip)