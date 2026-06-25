import requests
from datetime import datetime

def send_environment_data(sensor_data):
    """
    发送环境数据到服务器。
    sensor_data 为 parse_data.read_sensor_frame() 返回的字典，
    键: light, co2, tvoc, temp, humi, soil, n, p, k
    """
    url = "http://172.16.100.54:8081/api/environment/add"
    
    data = {
        "CarbonConcentration": float(sensor_data['co2']),
        "Temperature": float(sensor_data['temp']),
        "AirHumidity": float(sensor_data['humi']),
        "SoilHumidity": float(sensor_data['soil']),
        "N": float(sensor_data['n']),
        "P": float(sensor_data['p']),
        "K": float(sensor_data['k']),
        "LightIntensity": float(sensor_data['light']),
        "TotalVolatileOrganicCompounds": float(sensor_data['tvoc']),
        "recordTime": datetime.now().isoformat()
    }
    
    headers = {"Content-Type": "application/json"}
    
    try:
        print(f"📡 正在发送环境数据到 {url}")
        response = requests.post(url, json=data, headers=headers, timeout=10)
        if response.status_code == 200:
            print("✅ 环境数据发送成功!")
        else:
            print(f"❌ 环境数据发送失败: HTTP {response.status_code} - {response.text}")
    except Exception as e:
        print(f"❌ 环境数据发送异常: {e}")

# 单独测试用
if __name__ == "__main__":
    test_data = {
        'light': 15000.00,
        'co2': 450.25,
        'tvoc': 6.80,
        'temp': 25.50,
        'humi': 65.30,
        'soil': 45.20,
        'n': 120.75,
        'p': 85.50,
        'k': 95.25
    }
    send_environment_data(test_data)