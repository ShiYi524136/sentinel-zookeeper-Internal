package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRulePublisher;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description Zookeeper规则发布类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("degradeRuleZookeeperPublisher")
public class DegradeRuleZookeeperPublisher extends AbstractZookeeperRulePublisher<DegradeRuleEntity> {

    @Override
    protected String ruleEntityEncoder(List<DegradeRuleEntity> rules) {
        String data = JSON.toJSONString(
                rules.stream().map(DegradeRuleEntity::toDegradeRule).collect(Collectors.toList()));
        return data;
    }
}
