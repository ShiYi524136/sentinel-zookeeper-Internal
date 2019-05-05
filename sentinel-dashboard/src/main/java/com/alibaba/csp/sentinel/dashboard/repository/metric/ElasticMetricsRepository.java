package com.alibaba.csp.sentinel.dashboard.repository.metric;

/**
 * @ClassName: ElasticMetricsRepository
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/4/311:39
 */

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.util.StringUtil;

/**
 * Elasticsearch Metrics Repository
 *
 * @author huangbing
 * @create 2019-04-03 11:39
 * @since 1.0.0
 **/
@Repository("elasticMetricsRepository")
public class ElasticMetricsRepository implements MetricsRepository<MetricEntity> {

    private final Logger logger = LoggerFactory.getLogger(ElasticMetricsRepository.class);

    @Autowired
    private EsRepository repository;

    /**
     * Save the metric to the storage repository.
     *
     * @param metric
     *            metric data to save
     */
    public void save(MetricEntity metric) {
        EsMetricEntity esMetricEntity = new EsMetricEntity();
        BeanUtils.copyProperties(metric, esMetricEntity, "id");
        logger.debug("存储之前metric={},转换之后esMetricEntity={}", metric, esMetricEntity);
        repository.save(esMetricEntity);
    }

    /**
     * Save all metrics to the storage repository.
     *
     * @param metrics
     *            metrics to save
     */
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        metrics.forEach(this::save);
    }

    /**
     * Get all metrics by {@code appName} and {@code resourceName} between a period of time.
     *
     * @param app
     *            application name for Sentinel
     * @param resource
     *            resource name
     * @param startTime
     *            start timestamp
     * @param endTime
     *            end timestamp
     * @return all metrics in query conditions
     */
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        List<EsMetricEntity> metrics =
            repository.getByAppAndResourceAndTimestampBetween(app, resource, new Date(startTime), new Date(endTime));
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyList();
        }
        List<MetricEntity> entities = new ArrayList<>();
        metrics.forEach((metric) -> {
            MetricEntity entity = new MetricEntity();
            BeanUtils.copyProperties(metric, entity);
            entities.add(entity);
        });
        return entities;
    }

    /**
     * List resource name of provided application name.
     *
     * @param app
     *            application name
     * @return list of resources
     */
    public List<String> listResourcesOfApp(String app) {
        if (StringUtil.isBlank(app)) {
            return Collections.emptyList();
        }
        // 查询3分钟以内的资源
        LocalDateTime dateTime = LocalDateTime.now().minusMinutes(3);
        Date start = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        List<EsMetricEntity> metrics = repository.getResourceByAppAndTimestampAfter(app, start);
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, MetricEntity> resources = new HashMap<>(32);
        metrics.forEach((metric) -> {
            MetricEntity entity = new MetricEntity();
            BeanUtils.copyProperties(metric, entity);
            String resource = entity.getResource();
            if (resources.containsKey(resource)) {
                MetricEntity exist = resources.get(resource);
                exist.addBlockQps(entity.getBlockQps());
                exist.addPassQps(entity.getPassQps());
                exist.addExceptionQps(entity.getExceptionQps());
                exist.addCount(entity.getCount());
                exist.addRtAndSuccessQps(entity.getRt(), entity.getSuccessQps());
            } else {
                resources.put(resource, MetricEntity.copyOf(entity));
            }
        });

        return resources.entrySet().stream().sorted((o1, o2) -> {
            MetricEntity e1 = o1.getValue();
            MetricEntity e2 = o2.getValue();
            int t = e2.getPassQps().compareTo(e1.getPassQps());
            if (t != 0) {
                return t;
            }
            return e2.getExceptionQps().compareTo(e1.getExceptionQps());
        }).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
