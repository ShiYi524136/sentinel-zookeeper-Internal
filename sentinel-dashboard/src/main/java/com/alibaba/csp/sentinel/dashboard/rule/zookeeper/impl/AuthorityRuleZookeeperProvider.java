package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.impl;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.AbstractZookeeperRuleProvider;
import com.alibaba.csp.sentinel.dashboard.util.RuleUtils;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description Zookeeper规则查询类
 * @author: cc
 * @Date: 2019-03-04
 */
@Service("authorityRuleZookeeperProvider")
public class AuthorityRuleZookeeperProvider extends AbstractZookeeperRuleProvider<AuthorityRuleEntity> {

    @Override
    protected List<AuthorityRuleEntity> getRuleEntityDecoders(String app, String ip, int port, String rules) {
        List<AuthorityRule> ruleZ = RuleUtils.parseAuthorityRule(rules);
        return ruleZ.stream()
                .map(e -> AuthorityRuleEntity.fromAuthorityRule(app, ip, port, e))
                .collect(Collectors.toList());
    }
}
