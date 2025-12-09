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

    /**
     * 单步前进
     *
     * @param graphId 图id
     * @param context 流上下文
     */
    StatefulTask stepForward(String graphId, FlowContext context);

    /**
     * 单步前进
     *
     * @param graph   图
     * @param context 流上下文
     */
    StatefulTask stepForward(Graph graph, FlowContext context);

    /**
     * 单步后退
     *
     * @param graphId 图id
     * @param context 流上下文
     */
    StatefulTask stepBack(String graphId, FlowContext context);

    /**
     * 单步后退
     *
     * @param graph   图
     * @param context 流上下文
     */
    StatefulTask stepBack(Graph graph, FlowContext context);


    /// ////////////////////////////////

    /**
     * 提交操作（如果当前任务为等待介入）
     *
     * @param graphId   图id
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    boolean postOperationIfWaiting(String graphId, String nodeId, Operation operation, FlowContext context);

    /**
     * 提交操作（如果当前任务为等待介入）
     *
     * @param node      节点
     * @param operation 操作
     * @param context   流上下文
     */
    boolean postOperationIfWaiting(Node node, Operation operation, FlowContext context);

    /**
     * 提交操作
     *
     * @param graphId   图id
     * @param nodeId    节点id
     * @param operation 操作
     * @param context   流上下文
     */
    void postOperation(String graphId, String nodeId, Operation operation, FlowContext context);

    /**
     * 提交操作
     *
     * @param node      节点
     * @param operation 操作
     * @param context   流上下文
     */
    void postOperation(Node node, Operation operation, FlowContext context);


    /// ////////////////////////////////

    /**
     * 运行
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    default StateResult eval(String graphId, FlowContext context) {
        return eval(engine().getGraphOrThrow(graphId), context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    default StateResult eval(Graph graph, FlowContext context) {
        return eval(graph, graph.getStart(), context);
    }

    /**
     * 运行
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    default StateResult eval(String graphId, String startId, FlowContext context) {
        return eval(engine().getGraphOrThrow(graphId), startId, context);
    }

    /**
     * 运行
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    default StateResult eval(String graphId, Node startNode, FlowContext context) {
        return eval(engine().getGraphOrThrow(graphId), startNode, context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    default StateResult eval(Graph graph, String startId, FlowContext context) {
        return eval(graph, graph.getNodeOrThrow(startId), context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.7.4
     */
    @Deprecated
    StateResult eval(Graph graph, Node startNode, FlowContext context);


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