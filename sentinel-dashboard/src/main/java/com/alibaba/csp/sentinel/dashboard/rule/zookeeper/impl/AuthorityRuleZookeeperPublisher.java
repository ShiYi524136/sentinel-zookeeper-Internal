package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRulePublisher;
import com.alibaba.fastjson.JSON;

/**
 * @Description Zookeeper规则发布类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("authorityRuleZookeeperPublisher")
public class AuthorityRuleZookeeperPublisher extends AbstractZookeeperRulePublisher<AuthorityRuleEntity> {

    @Override
    protected String ruleEntityEncoder(List<AuthorityRuleEntity> rules) {
        String data = JSON.toJSONString(rules.stream().map(AuthorityRuleEntity::getRule).collect(Collectors.toList()));

        return data;
    }
}
