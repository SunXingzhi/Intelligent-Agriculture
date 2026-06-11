package tech.xuexinglab.demo.demos.web.service;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.User;
import tech.xuexinglab.demo.demos.web.mapper.EnvironmentMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EnvironmentService {

        private final EnvironmentMapper environmentMapper;

        public EnvironmentService(EnvironmentMapper environmentMapper) {
                this.environmentMapper = environmentMapper;
        }

        /**
         * 获取所有环境数据
         */
        public List<User> getAllData() {
                return environmentMapper.findAll();
        }

        /**
         * 根据ID获取数据
         */
        public User getDataById(Long id) {
                return environmentMapper.findById(id);
        }

        /**
         * 获取最新数据
         */
        public User getLatestData() {
                return environmentMapper.findLatest();
        }

        /**
         * 添加环境数据
         */
        public User addData(User user) {
                if (user.getRecordTime() == null) {
                        user.setRecordTime(LocalDateTime.now());
                }
                if (user.getCarbonConcentration() != null) {
                        if (user.getCarbonConcentration() < 0 || user.getCarbonConcentration() > 10000) {
                                user.setCarbonConcentration(null); // 设为null
                        }
                }
                // 验证温度
                if (user.getTemperature() != null) {
                        if (user.getTemperature() < -50 || user.getTemperature() > 100) {
                                user.setTemperature(null); // 设为null
                        }
                }

                // 验证湿度
                if (user.getHumidity() != null) {
                        if (user.getHumidity() < 0 || user.getHumidity() > 100) {
                                user.setHumidity(null); // 设为null
                        }
                }

                // 验证养分浓度
                if (user.getNutrients() != null) {
                        if (user.getNutrients() < 0 || user.getNutrients() > 1000) {
                                user.setNutrients(null); // 设为null
                        }
                }

                // 验证光强
                if (user.getLightIntensity() != null) {
                        if (user.getLightIntensity() < 0 || user.getLightIntensity() > 100000) {
                                user.setLightIntensity(null); // 设为null
                        }
                }

                // 验证pH值
                if (user.getPh() != null) {
                        if (user.getPh() < 0 || user.getPh() > 14) {
                                user.setPh(null); // 设为null
                        }
                }
                environmentMapper.insert(user);

                return user;
        }

        /**
         * 更新环境数据
         */
        public User updateData(Long id, User user) {
                user.setId(id);
                environmentMapper.update(user);
                return environmentMapper.findById(id);
        }

        /**
         * 删除环境数据
         */
        public boolean deleteData(Long id) {
                return environmentMapper.deleteById(id) > 0;
        }

        /**
         * 根据温度范围查询
         */
        public List<User> findByTemperatureRange(double min, double max) {
                return environmentMapper.findByTemperatureRange(min, max);
        }

        /**
         * 获取统计数据
         */
        public Map<String, Object> getStatistics() {
                return environmentMapper.getStatistics();
        }

        /**
         * 批量添加数据
         */
        public List<User> batchAddData(List<User> dataList) {
                for (User data : dataList) {
                        if (data.getRecordTime() == null) {
                                data.setRecordTime(LocalDateTime.now());
                        }
                        environmentMapper.insert(data);
                }
                return dataList;
        }
}