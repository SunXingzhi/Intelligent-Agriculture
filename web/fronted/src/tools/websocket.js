import { ref, onMounted, onUnmounted } from 'vue';

export function useWebSocket(url) {
        const socket = ref(null);
        const messages = ref([]);

        // 连接 WebSocket
        const connect = () => {
                socket.value = new WebSocket(url);

                socket.value.onopen = () => {
                        console.log('WebSocket连接已建立');
                };

                socket.value.onmessage = (event) => {
                        messages.value.push(event.data);
                        console.log('收到消息:', event.data);
                };

                socket.value.onclose = () => {
                        console.log('WebSocket连接已关闭');
                };

                socket.value.onerror = (error) => {
                        console.error('WebSocket错误:', error);
                };
        };

        // 发送消息
        const sendMessage = (message) => {
                if (socket.value && socket.value.readyState === WebSocket.OPEN) {
                        socket.value.send(message);
                        console.log('发送消息:', message);
                } else {
                        console.error('WebSocket未连接');
                }
        };

        // 清理 WebSocket 连接
        onUnmounted(() => {
                if (socket.value) {
                        socket.value.close();
                }
        });

        onMounted(() => {
                connect();  // 在组件挂载时连接 WebSocket
        });

        return { socket, messages, sendMessage };
}