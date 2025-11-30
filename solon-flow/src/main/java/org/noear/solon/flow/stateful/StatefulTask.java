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

import org.noear.solon.core.util.Assert;
import org.noear.solon.flow.*;
import org.noear.solon.lang.Preview;

/**
 * 有状态的任务
 *
 * @author noear
 * @since 3.1
 */
public class StatefulTask implements StateResult {
    @Deprecated
    public static final String KEY_ACTIVITY_NODE = "ACTIVITY_NODE";
    @Deprecated
    public static final String KEY_ACTIVITY_LIST = "ACTIVITY_LIST";
    @Deprecated
    public static final String KEY_ACTIVITY_LIST_GET = "ACTIVITY_LIST_GET";

    private final FlowEngine flowEngine;
    private final Node node;
    private final StateType state;

    public StatefulTask(FlowEngine flowEngine, Node node, StateType state) {
        this.flowEngine = flowEngine;
        this.node = node;
        this.state = state;
    }

    /**
     * 运行当前节点任务（如果有？）
     *
     * @param context 上下文
     */
    @Preview("3.4")
    public void runTask(FlowContext context) throws FlowException {
        Assert.notNull(node, "node is null");

        try {
            flowEngine.getDriver(node.getGraph()).handleTask(new FlowExchanger(context), node.getTask());
        } catch (FlowException e) {
            throw e;
        } catch (Throwable e) {
            throw new FlowException("The task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 节点
     */
    public Node getNode() {
        return node;
    }

    /**
     * 状态
     */
    public StateType getState() {
        return state;
    }

    @Override
    public String toString() {
        return "StatefulNode{" +
                "node=" + node +
                ", state=" + state +
                '}';
    }
}