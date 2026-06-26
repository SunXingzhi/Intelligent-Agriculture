import { USE_LOCAL, LOCAL_PATH } from "./useImageStream";
// 返回Base64字符串
export async function getLocalImage(imagePath) {
        // 1. 用 fetch 请求 public 目录下的图片
        const response = await fetch(imagePath)      
        console.log('response', response)
        
        // 2. 获取图片二进制数据
        const blob = await response.blob()

        // 3. 转成 base64
        return new Promise((resolve) => {
                const reader = new FileReader()
                reader.onloadend = () => {
                        // 去掉前缀，返回纯 base64，与 WebSocket 格式一致
                        const base64 = reader.result.split(',')[1]
                        resolve(base64)
                }
                reader.readAsDataURL(blob)
        })
}