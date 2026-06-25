import { ref, watch } from 'vue';
import { useWebSocket } from './websocket';


const USE_LOCAL = false;
const LOCAL_PATH = '/test_images';
export { USE_LOCAL, LOCAL_PATH };

export function useImageStream() {
        const topImage = ref(null);
        const frontImage = ref(null);

        if (USE_LOCAL) {
                import('./read_files').then(async (mod) => {
                        topImage.value = await mod.getLocalImage(`${LOCAL_PATH}/000.png`);
                        frontImage.value = await mod.getLocalImage(`${LOCAL_PATH}/001.png`);
                })
        } else {
                // 占位： 获取图像数据
                1;
        }
        return { topImage, frontImage };
}
