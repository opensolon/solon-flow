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

import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.lang.Preview;

import java.util.Collection;

/**
 * 有状态的流引擎
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public interface StatefulFlowEngine extends FlowEngine {
    /**
     * 构建实例
     */
    static StatefulFlowEngine newInstance(StatefulSimpleFlowDriver driver) {
        return new StatefulFlowEngineDefault(driver);
    }

    /**
     * 构建实例
     */
    static StatefulFlowEngine newInstance() {
        return newInstance(StatefulSimpleFlowDriver.builder().build());
    }

    /**
     * 单步前进
     */
    StatefulNode stepForward(String chainId, FlowContext context);

    /**
     * 单步前进
     */
    StatefulNode stepForward(Chain chain, FlowContext context);

    /**
     * 单步后退
     */
    StatefulNode stepBack(String chainId, FlowContext context);

    /**
     * 单步后退
     */
    StatefulNode stepBack(Chain chain, FlowContext context);

    /// ////////////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulNode> getActivityNodes(String chainId, FlowContext context);

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulNode> getActivityNodes(Chain chain, FlowContext context);

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有人员配置）
     */
    StatefulNode getActivityNode(String chainId, FlowContext context);

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有人员配置）
     */
    StatefulNode getActivityNode(Chain chain, FlowContext context);

    /// ////////////////////////////////

    /**
     * 提交活动状态（如果当前节点为等待介入）
     */
    boolean postActivityStateIfWaiting(FlowContext context, String chainId, String activityNodeId, StateType state);

    /**
     * 提交活动状态（如果当前节点为等待介入）
     */
    boolean postActivityStateIfWaiting(FlowContext context, Node activity, StateType state);

    /**
     * 提交活动状态
     */
    void postActivityState(FlowContext context, String chainId, String activityNodeId, StateType state);

    /**
     * 提交活动状态
     */
    void postActivityState(FlowContext context, Node activity, StateType state);

    /**
     * 清空活动状态
     */
    void clearActivityState(FlowContext context);
}