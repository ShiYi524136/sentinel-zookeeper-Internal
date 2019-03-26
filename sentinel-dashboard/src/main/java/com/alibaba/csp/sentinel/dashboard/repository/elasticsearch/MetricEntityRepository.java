package com.alibaba.csp.sentinel.dashboard.repository.elasticsearch;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @Description MetricEntity
 * @author: cc
 * @Date: 2019-03-26
 */
public interface MetricEntityRepository extends ElasticsearchRepository<MetricEntity, Long> {

    List<MetricEntity> findByResource(String resource);

    List<MetricEntity> findByApp(String App);

}