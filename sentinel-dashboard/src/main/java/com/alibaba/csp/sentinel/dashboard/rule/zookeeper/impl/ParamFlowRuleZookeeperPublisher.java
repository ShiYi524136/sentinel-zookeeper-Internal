package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRulePublisher;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description Zookeeper规则发布类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("paramFlowRuleZookeeperPublisher")
public class ParamFlowRuleZookeeperPublisher extends AbstractZookeeperRulePublisher<ParamFlowRuleEntity> {

    @Override
    public String ruleEntityEncoder(List<ParamFlowRuleEntity> rules) {
        String data = JSON.toJSONString(rules.stream().map(ParamFlowRuleEntity::getRule).collect(Collectors.toList()));
        return data;
    }
}
