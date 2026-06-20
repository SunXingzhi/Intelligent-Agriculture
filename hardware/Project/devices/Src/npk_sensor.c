//
// Created by Levi on 2026/6/6.
//
#include "npk_sensor.h"
#include "usart.h"
#include "gpio.h"
#include <string.h>

// 控制 485 方向：0=接收模式，1=发送模式
static void NPK_SetTransmitMode(uint8_t mode)
{
    HAL_GPIO_WritePin(NPK_RE_DE_GPIO_PORT, NPK_RE_DE_PIN, mode ? GPIO_PIN_SET : GPIO_PIN_RESET);
}

// CRC16-Modbus 计算
static uint16_t CRC16_Modbus(uint8_t *buffer, uint16_t length)
{
    uint16_t crc = 0xFFFF;
    for (uint16_t i = 0; i < length; i++) {
        crc ^= buffer[i];
        for (uint8_t j = 0; j < 8; j++) {
            if (crc & 0x0001) {
                crc = (crc >> 1) ^ 0xA001;
            } else {
                crc >>= 1;
            }
        }
    }
    return crc;
}

// 发送 Modbus 请求，接收并校验响应
static NPK_StatusTypeDef NPK_ModbusTransaction(uint8_t *tx_frame, uint8_t tx_len,
                                               uint8_t *rx_frame, uint8_t buff_size,
                                               uint8_t *rx_len,uint32_t timeout_ms)
{
    // 清空接收缓冲区
    memset(rx_frame, 0, buff_size);
    *rx_len = 0;

    // 发送模式
    NPK_SetTransmitMode(1);
    HAL_UART_Transmit(NPK_USART, tx_frame, tx_len, 100);
    // 等待发送完成
    while (HAL_UART_GetState(NPK_USART) != HAL_UART_STATE_READY) {
        // waiting...
    }
    // 延时 2ms 等待最后一个字节发出（4800bps 约 2ms/byte）
    HAL_Delay(2);

    // 切换为接收模式
    NPK_SetTransmitMode(0);

    // 接收响应（至少 5 字节，最多 255 字节）
    uint32_t start = HAL_GetTick();
    uint8_t idx = 0;
    while ((HAL_GetTick() - start) < timeout_ms) {
        if (__HAL_UART_GET_FLAG(NPK_USART, UART_FLAG_RXNE)) {
            uint8_t c;
            HAL_UART_Receive(NPK_USART, &c, 1, 10);
            rx_frame[idx++] = c;
            start = HAL_GetTick();  // 收到数据，重置超时
        }
        // 判断是否接收完整：长度至少 5 字节且已停止接收超过 3.5 字符时间（4800bps 下约 7ms）
        if (idx >= 5) {
            HAL_Delay(1);
            if (__HAL_UART_GET_FLAG(NPK_USART, UART_FLAG_RXNE) == RESET) {
                // 没有新数据，认为接收完成
                break;
            }
        }
        if (idx >= 50) break; // 防止溢出
    }
    *rx_len = idx;
    if (*rx_len < 5) return NPK_ERR_TIMEOUT;

    // CRC 校验
    uint16_t crc_calc = CRC16_Modbus(rx_frame, *rx_len - 2);
    uint16_t crc_recv = (uint16_t)(rx_frame[*rx_len-1] << 8) | rx_frame[*rx_len-2];
    if (crc_calc != crc_recv) return NPK_ERR_CRC;

    // 检查地址和功能码
    if (rx_frame[0] != tx_frame[0]) return NPK_ERR_ADDR;
    if ((rx_frame[1] & 0x7F) != tx_frame[1]) return NPK_ERR_FUNC;
    if (rx_frame[1] & 0x80) return NPK_ERR_FRAME; // 异常响应

    return NPK_OK;
}

// 读取氮、磷、钾三个寄存器
NPK_StatusTypeDef NPK_ReadNPK(int16_t *N, int16_t *P, int16_t *K)
{
    uint8_t tx[8] = {0};
    uint8_t rx[20] = {0};
    uint8_t rx_len;

    // 构建读取 3 个寄存器（起始 0x001E，数量 0x0003）
    tx[0] = NPK_DEVICE_ADDR;
    tx[1] = 0x03;
    tx[2] = 0x00;
    tx[3] = 0x1E;
    tx[4] = 0x00;
    tx[5] = 0x03;
    uint16_t crc = CRC16_Modbus(tx, 6);
    tx[6] = crc & 0xFF;
    tx[7] = crc >> 8;

    NPK_StatusTypeDef ret = NPK_ModbusTransaction(tx, 8, rx, NPK_BUFF_SIZE, &rx_len, 500);
    if (ret != NPK_OK) return ret;

    // 响应格式: addr(1) + func(1) + byteCount(1) + data(2*3) + crc(2)
    if (rx_len != 9) return NPK_ERR_FRAME;
    *N = (int16_t)((rx[3] << 8) | rx[4]);
    *P = (int16_t)((rx[5] << 8) | rx[6]);
    *K = (int16_t)((rx[7] << 8) | rx[8]);
    return NPK_OK;
}

// 写单个寄存器（功能码 0x06）
static NPK_StatusTypeDef NPK_WriteRegister(uint16_t reg, uint16_t value)
{
    uint8_t tx[8];
    tx[0] = NPK_DEVICE_ADDR;
    tx[1] = 0x06;
    tx[2] = reg >> 8;
    tx[3] = reg & 0xFF;
    tx[4] = value >> 8;
    tx[5] = value & 0xFF;
    uint16_t crc = CRC16_Modbus(tx, 6);
    tx[6] = crc & 0xFF;
    tx[7] = crc >> 8;

    uint8_t rx[8];
    uint8_t rx_len;
    NPK_StatusTypeDef ret = NPK_ModbusTransaction(tx, 8, rx, NPK_BUFF_SIZE, &rx_len, 500);
    if (ret != NPK_OK) return ret;
    if (rx_len != 8) return NPK_ERR_FRAME;
    // 正常响应应与请求帧相同
    if (memcmp(tx, rx, 8) != 0) return NPK_ERR_FRAME;
    return NPK_OK;
}

NPK_StatusTypeDef NPK_WriteN(uint16_t value)
{
    return NPK_WriteRegister(0x001E, value);
}
NPK_StatusTypeDef NPK_WriteP(uint16_t value)
{
    return NPK_WriteRegister(0x001F, value);
}
NPK_StatusTypeDef NPK_WriteK(uint16_t value)
{
    return NPK_WriteRegister(0x0020, value);
}