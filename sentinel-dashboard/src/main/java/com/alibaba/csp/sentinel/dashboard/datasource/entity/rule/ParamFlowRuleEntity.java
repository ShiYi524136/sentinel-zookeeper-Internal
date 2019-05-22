/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import java.util.List;

import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.annotation.JSONField;


/**
 * @author Eric Zhao
 * @since 0.2.1
 */
public class ParamFlowRuleEntity extends AbstractRuleEntity<ParamFlowRule> {
 
	/**
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)
	 */
	private static final long serialVersionUID = 57452041509992232L;

	public ParamFlowRuleEntity() {
	}

    public ParamFlowRuleEntity(ParamFlowRule rule) {
        AssertUtil.notNull(rule, "Authority rule should not be null");
        this.rule = rule;
    }

    public static ParamFlowRuleEntity fromAuthorityRule(String app, String ip, Integer port, ParamFlowRule rule) {
        ParamFlowRuleEntity entity = new ParamFlowRuleEntity(rule);
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        return entity;
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public String getLimitApp() {
        return rule.getLimitApp();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public String getResource() {
        return rule.getResource();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public int getGrade() {
        return rule.getGrade();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public Integer getParamIdx() {
        return rule.getParamIdx();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public double getCount() {
        return rule.getCount();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public List<ParamFlowItem> getParamFlowItemList() {
        return rule.getParamFlowItemList();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public boolean isClusterMode() {
        return rule.isClusterMode();
    }

    @JSONField(serialize = false)
    //@JsonIgnore
    public ParamFlowClusterConfig getClusterConfig() {
        return rule.getClusterConfig();
    }
}
