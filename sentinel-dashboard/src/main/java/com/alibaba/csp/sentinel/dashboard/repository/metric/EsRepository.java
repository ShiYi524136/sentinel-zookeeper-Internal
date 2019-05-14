package com.alibaba.csp.sentinel.dashboard.repository.metric;

/**
 * @ClassName: EsRepository
 * @ProjectName sentinel-zookeeper-Internal
 * @author huangbing
 * @date 2019/4/311:53
 */

import java.util.Date;
import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;

/**
 * EsRepository
 * 
 * @author huangbing
 * @create 2019-04-03 11:53
 * @since 1.0.0
 **/
public interface EsRepository extends ElasticsearchRepository<MetricEntity, String> {

    List<MetricEntity> getResourceByAppAndTimestampAfter(String app, Date start);

    /**
     * df
     * 
     * @param app
     * @param timestamp
     * @return
     */
    List<MetricEntity> findByAppAndTimestampAfter(String app, String timestamp);

    List<MetricEntity> getByAppAndResourceAndTimestampBetween(String app, String resource, String from, String to);
}