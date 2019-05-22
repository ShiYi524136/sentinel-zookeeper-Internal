 package com.alibaba.csp.sentinel.dashboard.es;


import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;

import com.alibaba.csp.sentinel.dashboard.Tester;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.ElasticMetricsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticMetricsRepositoryTest extends Tester {

	@Resource
	private ElasticMetricsRepository elasticMetricsRepository;

	@Test
	public void queryByAppAndResourceBetween() throws Exception {
		String app = "sentinel-dashboard" ;
		String resource = "/metric/queryTopResourceMetric.json";// "/app/sentinel-dashboard/machines.json";//
															// "/registry/machine";
		String startDateTimeStr = "2019-05-22 10:20:22";
		String endDateTimeStr = "2019-05-22 10:30:22";
		
		List<MetricEntity> queryByAppAndResourceBetween = null;
		try {
			queryByAppAndResourceBetween = elasticMetricsRepository.queryByAppAndResourceBetween(app, resource,
					startDateTimeStr, endDateTimeStr);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			 e.printStackTrace();
			System.out.println();
		}
		System.out.println("end");
	}
}
