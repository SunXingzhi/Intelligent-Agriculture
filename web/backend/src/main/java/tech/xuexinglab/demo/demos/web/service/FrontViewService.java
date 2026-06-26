package tech.xuexinglab.demo.demos.web.service;

import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.FrontViewData;
import tech.xuexinglab.demo.demos.web.mapper.FrontViewMapper;

import java.util.List;

@Service
public class FrontViewService {

    private final FrontViewMapper frontViewMapper;

    public FrontViewService(FrontViewMapper frontViewMapper) {
        this.frontViewMapper = frontViewMapper;
    }

    /**
     * 获取所有前视图记录（按时间倒序）
     */
    public List<FrontViewData> getAll() {
        return frontViewMapper.findAll();
    }

    /**
     * 根据ID获取单条记录
     */
    public FrontViewData getById(Long id) {
        return frontViewMapper.findById(id);
    }

    // 如有需要，可添加更多查询方法，如按设备别名、时间段等
}