package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
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
@Service("systemRuleZookeeperPublisher")
public class SystemRuleZookeeperPublisher extends AbstractZookeeperRulePublisher<SystemRuleEntity> {

    @Override
    protected String ruleEntityEncoder(List<SystemRuleEntity> rules) {
        String data = JSON.toJSONString(
                rules.stream().map(SystemRuleEntity::toSystemRule).collect(Collectors.toList()));
        return data;
    }
}
