import { ref } from 'vue';

const USE_LOCAL = false;
const LOCAL_PATH = '/test_images';
export { USE_LOCAL, LOCAL_PATH };

/**
 * 提供 topImage / frontImage 两个响应式引用。
 * - 本地模式：从 public/test_images/ 加载测试图片
 * - 远程模式：由 Detection.vue 中的 WebSocket watcher 直接写入
 */
export function useImageStream() {
        const topImage = ref(null);
        const frontImage = ref(null);

        if (USE_LOCAL) {
                import('./read_files').then(async (mod) => {
                        topImage.value = await mod.getLocalImage(`${LOCAL_PATH}/000.png`);
                        frontImage.value = await mod.getLocalImage(`${LOCAL_PATH}/001.png`);
                })
        }

        return { topImage, frontImage };
}
