package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import com.alibaba.csp.sentinel.dashboard.util.RuleUtils;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @version Revision: 0.0.1
 * @author: weihuang.peng
 * @Date: 2018-12-22
 */
@Service("degradeRuleZookeeperProvider")
public class DegradeRuleZookeeperProvider extends AbstractZookeeperRuleProvider<DegradeRuleEntity> {

    @Override
    protected List<DegradeRuleEntity> getRuleEntityDecoders(String app, String ip, int port, String rules) {
        List<DegradeRule> ruleZ = RuleUtils.parseDegradeRule(rules);
        if (rules != null) {
            return ruleZ.stream().map(rule -> DegradeRuleEntity.fromDegradeRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
