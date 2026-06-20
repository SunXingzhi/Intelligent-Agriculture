/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2026 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "adc.h"
#include "i2c.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "bh1750.h"
#include "oled.h"
#include "i2c.h"
#include "sgp30.h"
#include "dht11.h"
#include "soil_moisture.h"
#include "npk_sensor.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/

/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */
// float     LightData; // 光照强度（小数，单位：lx）
// uint32_t  LightData_Hex; // 光照强度（整数，单位：lx）
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{

  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_I2C1_Init();
  MX_I2C2_Init();
  MX_TIM3_Init();
  MX_USART1_UART_Init();
  MX_ADC1_Init();
  MX_USART2_UART_Init();
  /* USER CODE BEGIN 2 */
  // adc callibration
  if (HAL_ADCEx_Calibration_Start(&hadc1)!=HAL_OK) Error_Handler();
  OLED_Init();
  OLED_Clear();

  uint32_t  dat;
  uint16_t  co2Data,TVOCData;
  uint8_t   temperature = 1;  //温度
  uint8_t   humidity = 1;     //湿度
  uint8_t   aTXbuf[32];       //串口发送缓存数组
  float     LightData;
  uint32_t  LightData_Hex;
  BH1750_Init();
  SGP30_Init();
  DHT11_Init();
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
    // 调试BH1750光照传感器
    LightData = BH1750_ReadLight();         // 读取光照强度
    LightData_Hex = (uint32_t)LightData;    // float转换成整数

    // 调试SGP30 CO2传感器
    SGP30_ad_write(0x20,0x08);
    dat = SGP30_ad_read();
    co2Data = (dat & 0xffff0000) >> 16;
    TVOCData = dat & 0x0000ffff;

    // 调试DHT11 温湿度传感器
    if (DHT11_Read_Data(&temperature , &humidity) != 0)
    {
      // 读取失败处理，如果注释则为保留旧值
      // temperature  = 0;
      // humidity     = 0;
    };

    // 土壤湿度读取
    uint8_t soil_moisture = Moisture_GetPercentage();
    uint16_t soil_raw_adc = Moisture_GetRawADC();

    // NPK传感器读取
    int16_t npk_N, npk_P, npk_K;
    NPK_StatusTypeDef npk_ret = NPK_ReadNPK(&npk_N, &npk_P, &npk_K);
    if (npk_ret == NPK_OK) {
      // 读取成功，npk_N/npk_P/npk_K 即为氮磷钾值
    } else {
      npk_K = 0;
      npk_N = 0;
      npk_P = 0;
      // 错误处理
      switch (npk_ret) {
      case NPK_ERR_TIMEOUT: /* 超时 */ break;
      case NPK_ERR_CRC:     /* CRC错误 */ break;
      case NPK_ERR_FRAME:   /* 帧错误 */ break;
      case NPK_ERR_ADDR:    /* 地址错误 */ break;
      case NPK_ERR_FUNC:    /* 功能码错误 */ break;
      default: break;
      }
    }

    // ====== 帧头 + 长度 ======
    aTXbuf[0] = 0xAA;
    aTXbuf[1] = 0x55;
    aTXbuf[2] = 0x11;  // 17字节数据

    // ====== 光照 (4字节, 小端序) ======
    aTXbuf[3]  = (uint8_t)(LightData_Hex & 0xFF);
    aTXbuf[4]  = (uint8_t)((LightData_Hex >> 8) & 0xFF);
    aTXbuf[5]  = (uint8_t)((LightData_Hex >> 16) & 0xFF);
    aTXbuf[6]  = (uint8_t)((LightData_Hex >> 24) & 0xFF);

    // ====== CO2 (2字节) ======
    aTXbuf[7]  = (uint8_t)(co2Data & 0xFF);
    aTXbuf[8]  = (uint8_t)((co2Data >> 8) & 0xFF);

    // ====== TVOC (2字节) ======
    aTXbuf[9]  = (uint8_t)(TVOCData & 0xFF);
    aTXbuf[10] = (uint8_t)((TVOCData >> 8) & 0xFF);

    // ====== 温度、湿度 ======
    aTXbuf[11] = temperature;
    aTXbuf[12] = humidity;

    // ====== 土壤湿度 (1字节) ======
    aTXbuf[13] = (uint8_t)(soil_moisture & 0xFF);

    // ====== N (2字节, int16_t 有符号) ======
    aTXbuf[14] = (uint8_t)(npk_N & 0xFF);
    aTXbuf[15] = (uint8_t)((npk_N >> 8) & 0xFF);

    // ====== P (2字节) ======
    aTXbuf[16] = (uint8_t)(npk_P & 0xFF);
    aTXbuf[17] = (uint8_t)((npk_P >> 8) & 0xFF);

    // ====== K (2字节) ======
    aTXbuf[18] = (uint8_t)(npk_K & 0xFF);
    aTXbuf[19] = (uint8_t)((npk_K >> 8) & 0xFF);

    // ====== 校验和 (帧头 + 长度 + 数据 累加和的低8位) ======
    uint8_t sum = 0;
    for (int i = 0; i < 20; i++) {
      sum += aTXbuf[i];
    }
    aTXbuf[20] = sum;

    // ====== 发送 ======
    HAL_UART_Transmit(&huart1, aTXbuf, 21, HAL_MAX_DELAY);
    HAL_Delay(1000);
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_ADC;
  PeriphClkInit.AdcClockSelection = RCC_ADCPCLK2_DIV2;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/* USER CODE BEGIN 4 */

/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}
#ifdef USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
