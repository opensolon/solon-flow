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

import org.noear.solon.flow.*;
import org.noear.solon.lang.Preview;

import java.util.Collection;

/**
 * 有状态的服务
 *
 * @author noear
 * @since 3.4
 */
@Preview("3.4")
public interface FlowStatefulService {
    /**
     * 当前流程引擎
     */
    FlowEngine engine();


    /// ////////////////////////////////

    /**
     * 提交操作（如果当前任务为等待介入）
     *
     * @param graphId   图id
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    boolean postTaskIfWaiting(String graphId, String nodeId, StateOp operation, FlowContext context);

    /**
     * 提交操作（如果当前任务为等待介入）
     *
     * @param graph     图
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    boolean postTaskIfWaiting(Graph graph, String nodeId, StateOp operation, FlowContext context);

    /**
     * 提交操作（如果当前任务为等待介入）
     *
     * @param node      节点
     * @param operation 操作
     * @param context   流上下文
     */
    boolean postTaskIfWaiting(Node node, StateOp operation, FlowContext context);

    /**
     * 提交操作
     *
     * @param graphId   图id
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    void postTask(String graphId, String nodeId, StateOp operation, FlowContext context);

    /**
     * 提交操作
     *
     * @param graph     图
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    void postTask(Graph graph, String nodeId, StateOp operation, FlowContext context);

    /**
     * 提交操作
     *
     * @param node      节点
     * @param operation 操作
     * @param context   流上下文
     */
    void postTask(Node node, StateOp operation, FlowContext context);


    /// ////////////////////////////////

    /**
     * 获取多个任务
     *
     * @param graphId 图id
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulTask> getTasks(String graphId, FlowContext context);

    /**
     * 获取多个任务
     *
     * @param graph   图
     * @param context 流上下文（不需要有人员配置）
     */
    Collection<StatefulTask> getTasks(Graph graph, FlowContext context);

    /**
     * 获取当前任务
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     */
    StatefulTask getTask(String graphId, FlowContext context);

    /**
     * 获取当前任务
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     */
    StatefulTask getTask(Graph graph, FlowContext context);

    /// ////////////////////////////////

    /**
     * 清空状态
     *
     * @param graphId 图id
     * @param context 流上下文
     */
    void clearState(String graphId, FlowContext context);

    /**
     * 清空状态
     *
     * @param graph   图
     * @param context 流上下文
     */
    void clearState(Graph graph, FlowContext context);
}