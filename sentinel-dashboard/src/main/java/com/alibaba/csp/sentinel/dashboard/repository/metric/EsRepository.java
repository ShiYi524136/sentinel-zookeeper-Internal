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

/**
 * EsRepository
 * 
 * @author huangbing
 * @create 2019-04-03 11:53
 * @since 1.0.0
 **/
public interface EsRepository extends ElasticsearchRepository<EsMetricEntity, String> {

    List<EsMetricEntity> getResourceByAppAndTimestampAfter(String app, Date start);

    List<EsMetricEntity> getByAppAndResourceAndTimestampBetween(String app, String resource, Date from, Date to);
}