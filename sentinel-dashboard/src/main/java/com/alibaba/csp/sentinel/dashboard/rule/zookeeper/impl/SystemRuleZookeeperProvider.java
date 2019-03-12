package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import com.alibaba.csp.sentinel.dashboard.util.RuleUtils;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description Zookeeper规则查询类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("systemRuleZookeeperProvider")
public class SystemRuleZookeeperProvider extends AbstractZookeeperRuleProvider<SystemRuleEntity> {


    @Override
    protected List<SystemRuleEntity> getRuleEntityDecoders(String app, String ip, int port, String rules) {
        List<SystemRule> ruleZ = RuleUtils.parseSystemRule(rules);
        if (rules != null) {
            return ruleZ.stream().map(rule -> SystemRuleEntity.fromSystemRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
