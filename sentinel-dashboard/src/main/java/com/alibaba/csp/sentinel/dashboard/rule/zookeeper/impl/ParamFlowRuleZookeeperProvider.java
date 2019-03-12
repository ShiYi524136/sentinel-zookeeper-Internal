package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import com.alibaba.csp.sentinel.dashboard.util.RuleUtils;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description Zookeeper规则查询类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("paramFlowRuleZookeeperProvider")
public class ParamFlowRuleZookeeperProvider extends AbstractZookeeperRuleProvider<ParamFlowRuleEntity> {

    @Override
    protected List<ParamFlowRuleEntity> getRuleEntityDecoders(String app, String ip, int port,String rules) {
        List<ParamFlowRule> ruleZ = RuleUtils.parseParamFlowRule(rules);
        return ruleZ.stream()
                .map(e -> ParamFlowRuleEntity.fromAuthorityRule(app, ip, port, e))
                .collect(Collectors.toList());
    }
}
