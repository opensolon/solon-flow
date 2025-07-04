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
 * 有状态的服务
 *
 * @author noear
 * @since 3.4
 */
@Preview("3.4")
public interface StatefulService {
    /**
     * 当前流程引擎
     */
    FlowEngine engine();

    /**
     * 单步前进
     */
    StatefulTask stepForward(String chainId, FlowContext context);

    /**
     * 单步前进
     */
    StatefulTask stepForward(Chain chain, FlowContext context);

    /**
     * 单步后退
     */
    StatefulTask stepBack(String chainId, FlowContext context);

    /**
     * 单步后退
     */
    StatefulTask stepBack(Chain chain, FlowContext context);


    /// ////////////////////////////////

    /**
     * 提交操作（如果当前任务为等待介入）
     */
    boolean postOperationIfWaiting(FlowContext context, String chainId, String nodeId, StateOperation operation);

    /**
     * 提交操作（如果当前任务为等待介入）
     */
    boolean postOperationIfWaiting(FlowContext context, Node node, StateOperation operation);

    /**
     * 提交操作
     */
    void postOperation(FlowContext context, String chainId, String nodeId, StateOperation operation);

    /**
     * 提交操作
     */
    void postOperation(FlowContext context, Node node, StateOperation operation);


    /// ////////////////////////////////

    /**
     * 获取多个任务
     *
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulTask> getTasks(String chainId, FlowContext context);

    /**
     * 获取多个任务
     *
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulTask> getTasks(Chain chain, FlowContext context);

    /**
     * 获取当前任务
     *
     * @param context 流上下文（要有人员配置）
     */
    StatefulTask getTask(String chainId, FlowContext context);

    /**
     * 获取当前任务
     *
     * @param context 流上下文（要有人员配置）
     */
    StatefulTask getTask(Chain chain, FlowContext context);

    /// ////////////////////////////////

    /**
     * 清空状态
     */
    void clearState(String chainId, FlowContext context);

    /**
     * 清空状态
     */
    void clearState(Chain chain, FlowContext context);
}