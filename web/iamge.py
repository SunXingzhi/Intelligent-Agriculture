import base64
import os
from datetime import datetime

class ImageConverter:
    """图片转换工具类"""
    
    @staticmethod
    def image_to_base64(image_path):
        """将图片转换为Base64字符串"""
        try:
            # 检查文件是否存在
            if not os.path.exists(image_path):
                raise FileNotFoundError(f"图片文件不存在: {image_path}")
            
            # 读取图片
            with open(image_path, "rb") as image_file:
                image_data = image_file.read()
            
            # 转换为Base64
            base64_string = base64.b64encode(image_data).decode('utf-8')
            
            print(f"✅ 图片转换成功")
            print(f"📁 文件路径: {image_path}")
            print(f"📊 文件大小: {len(image_data)} 字节")
            print(f"📊 Base64长度: {len(base64_string)} 字符")
            
            return base64_string
            
        except Exception as e:
            print(f"❌ 转换失败: {e}")
            return None
    
    @staticmethod
    def base64_to_image(base64_string, output_path):
        """将Base64字符串转换回图片"""
        try:
            # 解码Base64
            image_data = base64.b64decode(base64_string)
            
            # 保存图片
            with open(output_path, "wb") as image_file:
                image_file.write(image_data)
            
            print(f"✅ 图片保存成功")
            print(f"📁 保存路径: {output_path}")
            print(f"📊 文件大小: {len(image_data)} 字节")
            
            return True
            
        except Exception as e:
            print(f"❌ 保存失败: {e}")
            return False
    
    @staticmethod
    def save_base64_to_file(base64_string, output_path):
        """将Base64字符串保存到文本文件"""
        try:
            with open(output_path, "w", encoding="utf-8") as f:
                f.write(base64_string)
            
            print(f"✅ Base64保存成功")
            print(f"📁 保存路径: {output_path}")
            
            return True
            
        except Exception as e:
            print(f"❌ 保存失败: {e}")
            return False

# 使用示例
if __name__ == "__main__":
    converter = ImageConverter()
    
    # 1. 图片转Base64
    image_path = "front_view_20260612_173744.jpg"
    base64_data = converter.image_to_base64(image_path)
    
    if base64_data:
        # 2. 保存Base64到文件
        converter.save_base64_to_file(base64_data, "image_base64.txt")
        
        # 3. 从Base64还原图片
        converter.base64_to_image(base64_data, "restored_image.jpg")