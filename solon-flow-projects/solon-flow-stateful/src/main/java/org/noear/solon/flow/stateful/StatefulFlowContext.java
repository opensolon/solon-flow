/*
 * Copyright 2017-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.flow.stateful;

import org.noear.solon.flow.FlowContext;

/**
 * 有状态的流上下文
 *
 * @author noear
 * @since 3.1
 */
public class StatefulFlowContext extends FlowContext {
    public StatefulFlowContext() {
        super(); //兼容无状态模式
    }

    public StatefulFlowContext(String instanceId) {
        super();

        put("instanceId", (instanceId == null ? "" : instanceId));
    }

    /**
     * 获取活动节点
     */
    protected void setActivityNode(StatefulNode activityNode) {
        put("activityNode", activityNode);
    }

    /**
     * 获取活动节点
     */
    public StatefulNode getActivityNode() {
        return get("activityNode");
    }

    /**
     * 获取实例id
     */
    public String getInstanceId() {
        return get("instanceId");
    }
}