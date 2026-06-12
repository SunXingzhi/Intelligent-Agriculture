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
                // 设置记录时间
                if (user.getRecordTime() == null) {
                        user.setRecordTime(LocalDateTime.now());
                }

                // 数据验证和清洗
                validateAndCleanData(user);

                // 保存到数据库
                environmentMapper.insert(user);

                return user;
        }

        /**
         * 验证并清洗数据
         */
        private void validateAndCleanData(User user) {
                // 验证二氧化碳浓度
                if (user.getCarbonConcentration() != null) {
                        if (user.getCarbonConcentration() < 0 || user.getCarbonConcentration() > 10000) {
                                user.setCarbonConcentration(null);
                        }
                }

                // 验证温度
                if (user.getTemperature() != null) {
                        if (user.getTemperature() < -50 || user.getTemperature() > 100) {
                                user.setTemperature(null);
                        }
                }

                // 验证空气湿度
                if (user.getAirHumidity() != null) {
                        if (user.getAirHumidity() < 0 || user.getAirHumidity() > 100) {
                                user.setAirHumidity(null);
                        }
                }

                // 验证土壤湿度
                if (user.getSoilHumidity() != null) {
                        if (user.getSoilHumidity() < 0 || user.getSoilHumidity() > 100) {
                                user.setSoilHumidity(null);
                        }
                }

                // 验证氮含量
                if (user.getNitrogen() != null) {
                        if (user.getNitrogen() < 0 || user.getNitrogen() > 1000) {
                                user.setNitrogen(null);
                        }
                }

                // 验证磷含量
                if (user.getPhosphorus() != null) {
                        if (user.getPhosphorus() < 0 || user.getPhosphorus() > 1000) {
                                user.setPhosphorus(null);
                        }
                }

                // 验证钾含量
                if (user.getPotassium() != null) {
                        if (user.getPotassium() < 0 || user.getPotassium() > 1000) {
                                user.setPotassium(null);
                        }
                }

                // 验证光强
                if (user.getLightIntensity() != null) {
                        if (user.getLightIntensity() < 0 || user.getLightIntensity() > 100000) {
                                user.setLightIntensity(null);
                        }
                }

                // 验证pH值
                if (user.getPh() != null) {
                        if (user.getPh() < 0 || user.getPh() > 14) {
                                user.setPh(null);
                        }
                }
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