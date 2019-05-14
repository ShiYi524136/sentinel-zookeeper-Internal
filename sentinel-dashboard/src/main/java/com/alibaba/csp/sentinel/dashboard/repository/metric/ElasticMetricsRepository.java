package com.alibaba.csp.sentinel.dashboard.repository.metric;

/**
 * @ClassName: ElasticMetricsRepository
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/4/311:39
 */

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.util.LocalDateTimeUtils;
import com.alibaba.csp.sentinel.util.StringUtil;

/**
 * Elasticsearch Metrics Repository
 *
 * @author huangbing
 * @create 2019-04-03 11:39
 * @since 1.0.0
 **/
@Repository("elasticMetricsRepository")
// public class ElasticMetricsRepository implements MetricsRepository<MetricEntity> {
public class ElasticMetricsRepository implements MetricsRepository<MetricEntity> {

    private final Logger logger = LoggerFactory.getLogger(ElasticMetricsRepository.class);

    @Autowired
    private EsRepository repository;

    @Autowired
    private TransportClient client;

    /**
     * Save the metric to the storage repository.
     *
     * @param metric
     *            metric data to save
     */
    @Override
    public void save(MetricEntity metric) {
        MetricEntity MetricEntity = new MetricEntity();
        BeanUtils.copyProperties(metric, MetricEntity, "id");
        logger.debug("存储之前metric={},转换之后esMetricEntity={}", metric, MetricEntity);
        repository.save(metric);
    }

    /**
     * Save all metrics to the storage repository.
     *
     * @param metrics
     *            metrics to save
     */
    @Override
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
    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {

        LocalDateTime startDateTime = LocalDateTimeUtils.convertDateToLDT(new Date(startTime));

        LocalDateTime endDateTime = LocalDateTimeUtils.convertDateToLDT(new Date(endTime));

        String startDateTimeStr = startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String endDateTimeStr = endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<MetricEntity> metrics =
            repository.getByAppAndResourceAndTimestampBetween(app, resource, startDateTimeStr, endDateTimeStr);
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
    @Override
    public List<String> listResourcesOfApp(String app) {
        if (StringUtil.isBlank(app)) {
            return Collections.emptyList();
        }
        // 查询3分钟以内的资源
        LocalDateTime dateTime = LocalDateTime.now().minusMinutes(300000000);
        Date start = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        String format1 = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        LocalDateTime dateTime2 = LocalDateTime.now();
        String format2 = dateTime2.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<MetricEntity> metrics = null;
        try {

            // createTimeRangeQuery(app, format1, format2);
            // metrics = repository.getResourceByAppAndTimestampAfter(app, format1);
            metrics = repository.findByAppAndTimestampAfter("sentinel-dashboard", format1);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("查询es失败，异常信息=" + e.getMessage(), e);
        }

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

    public List<MetricEntity> createTimeRangeQuery(String app, String startTime, String endTime) {
        /**
         * rangeQuery时间范围查询 以下三种查询方式的效果一样
         */
        // 多条件查询
        SearchResponse searchResponse = client.prepareSearch("laidian-metric").setTypes("sentinel")
            .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery("app", app))
                // .must(QueryBuilders.rangeQuery("gmtCreate").from("2019-05-13 11:14:30").to("2019-05-13 11:14:32")))
                .must(QueryBuilders.rangeQuery("gmtCreate").from(startTime).to(endTime)))
            .get();

        // 获取查询结果
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        System.out.println("总数目=" + totalHits);
        SearchHit[] hits2 = hits.getHits();

        List<MetricEntity> metrics = new ArrayList<>();

        for (SearchHit searchHit : hits2) {
            System.out.println(searchHit.getSourceAsString());
        }

        return metrics;
    }
}
