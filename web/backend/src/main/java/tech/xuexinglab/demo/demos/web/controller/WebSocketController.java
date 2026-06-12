package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import tech.xuexinglab.demo.demos.web.entity.ImageMessage;
import java.time.LocalDateTime;

@Controller
public class WebSocketController {
    
    /**
     * 接收树莓派发送的图片
     * 路径：/app/sendImage
     */
    @MessageMapping("/sendImage")
    @SendTo("/topic/images")
    public ImageMessage handleImage(ImageMessage imageMessage) {
        // 设置时间戳
        imageMessage.setTimestamp(LocalDateTime.now().toString());
        
        System.out.println("收到图片: " + imageMessage.getType() + 
                          ", 设备: " + imageMessage.getDeviceId());
        
        // 直接推送给所有订阅的前端
        return imageMessage;
    }
}