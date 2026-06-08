package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.xuexinglab.demo.demos.web.entity.User;
import tech.xuexinglab.demo.demos.web.service.EnvironmentService;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 环境监测数据控制器
 * 处理番茄环境数据的CRUD操作和查询功能
 */
@RestController
@RequestMapping("/api/environment")
public class EnvironmentController {
    
    @Autowired
    private EnvironmentService environmentService;
    
    /**
     * 添加环境数据
     * POST /api/environment/add
     */
    @PostMapping("/add")
    public ResponseEntity<User> addData(@RequestBody User userData) {
        User savedData = environmentService.addData(userData);
        return ResponseEntity.ok(savedData);
    }
    
    /**
     * 获取所有环境数据
     * GET /api/environment/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllData() {
        List<User> dataList = environmentService.getAllData();
        return ResponseEntity.ok(dataList);
    }
    
    /**
     * 获取最新的一条环境数据
     * GET /api/environment/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<User> getLatestData() {
        User latestData = environmentService.getLatestData();
        if (latestData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latestData);
    }
    
    /**
     * 根据ID获取环境数据
     * GET /api/environment/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<User> getDataById(@PathVariable Long id) {
        User data = environmentService.getDataById(id);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }
    
    /**
     * 更新环境数据
     * PUT /api/environment/update/{id}
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<User> updateData(@PathVariable Long id, @RequestBody User userData) {
        User updatedData = environmentService.updateData(id, userData);
        if (updatedData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedData);
    }
    
    /**
     * 删除环境数据
     * DELETE /api/environment/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteData(@PathVariable Long id) {
        boolean deleted = environmentService.deleteData(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        response.put("deletedId", id.toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取统计数据
     * GET /api/environment/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = environmentService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 批量添加数据
     * POST /api/environment/batch-add
     */
    @PostMapping("/batch-add")
    public ResponseEntity<Map<String, Object>> batchAddData(@RequestBody List<User> dataList) {
        List<User> savedDataList = environmentService.batchAddData(dataList);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量添加成功");
        response.put("count", savedDataList.size());
        response.put("data", savedDataList);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据温度范围查询
     * GET /api/environment/temperature?min=20&max=30
     */
    @GetMapping("/temperature")
    public ResponseEntity<List<User>> findByTemperatureRange(
            @RequestParam double min, 
            @RequestParam double max) {
        
        List<User> dataList = environmentService.findByTemperatureRange(min, max);
        return ResponseEntity.ok(dataList);
    }
    
    /**
     * 健康检查接口
     * GET /api/environment/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "番茄环境监测服务");
        health.put("dataCount", environmentService.getAllData().size());
        health.put("database", "MySQL");
        
        return ResponseEntity.ok(health);
    }
}