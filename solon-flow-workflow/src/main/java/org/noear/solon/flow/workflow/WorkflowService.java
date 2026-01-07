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
package org.noear.solon.flow.workflow;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

import java.util.Collection;

/**
 * 工作流执行器
 *
 * <pre>{@code
 * WorkflowService workflow = WorkflowService.of(engine, WorkflowDriver.builder()
 *         .stateController(new ActorStateController())
 *         .stateRepository(new InMemoryStateRepository())
 *         .build());
 *
 *
 * //1. 获取任务
 * Task task = workflow.getTask(graph, context);
 *
 * //2. 提交任务
 * workflow.submitTaskIfWaiting(task, TaskAction.FORWARD, context);
 * }</pre>
 *
 * @author noear
 * @since 3.4
 * @since 3.8
 * @deprecated 3.8.1 {@link WorkflowExecutor}
 */
@Deprecated
@Preview("3.4")
public interface WorkflowService extends WorkflowExecutor {
    static WorkflowService of(FlowEngine engine, StateController stateController, StateRepository stateRepository) {
        return new WorkflowExecutorDefault(engine, stateController, stateRepository);
    }

    /**
     * 流程引擎
     */
    FlowEngine engine();

    /**
     * 获取状态
     */
    TaskState getState(Node node, FlowContext context);


    /// ////////////////////////////////

    /**
     * 寻找当前活动任务
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.8.1 {@link #matchTask(String, FlowContext)}
     */
    @Deprecated
    @Nullable
    default Task getTask(String graphId, FlowContext context) {
        return matchTask(graphId, context);
    }

    /**
     * 寻找当前活动任务
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     * @deprecated 3.8.1 {@link #matchTask(Graph, FlowContext)}
     */
    @Deprecated
    @Nullable
    default Task getTask(Graph graph, FlowContext context) {
        return matchTask(graph, context);
    }

    /**
     * 寻找后续可达任务列表
     *
     * @param graphId 图id
     * @param context 流上下文（不需要有人员配置）
     * @deprecated 3.8.1 {@link #findNextTasks(String, FlowContext)}
     */
    @Deprecated
    default Collection<Task> getTasks(String graphId, FlowContext context) {
        return findNextTasks(graphId, context);
    }

    /**
     * 寻找后续可达任务列表
     *
     * @param graph   图
     * @param context 流上下文（不需要有人员配置）
     * @deprecated 3.8.1 {@link #findNextTasks(Graph, FlowContext)}
     */
    @Deprecated
    default Collection<Task> getTasks(Graph graph, FlowContext context) {
        return findNextTasks(graph, context);
    }


    /**
     * 提交任务（如果当前任务为等待介入）
     *
     * @param task    任务
     * @param action  动作
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #submitTaskIfWaiting(Task, TaskAction, FlowContext)}
     */
    @Deprecated
    default boolean postTaskIfWaiting(Task task, TaskAction action, FlowContext context) {
        return submitTaskIfWaiting(task, action, context);
    }

    /**
     * 提交任务
     *
     * @param graphId 图id
     * @param nodeId  节点id
     * @param action  动作
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #submitTask(String, String, TaskAction, FlowContext)}
     */
    @Deprecated
    default void postTask(String graphId, String nodeId, TaskAction action, FlowContext context) {
        submitTask(graphId, nodeId, action, context);
    }

    /**
     * 提交任务
     *
     * @param graph   图
     * @param nodeId  节点id
     * @param action  动作
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #submitTask(Graph, String, TaskAction, FlowContext)}
     */
    @Deprecated
    default void postTask(Graph graph, String nodeId, TaskAction action, FlowContext context) {
        submitTask(graph, nodeId, action, context);
    }

    /**
     * 提交任务
     *
     * @param node    节点
     * @param action  动作
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #submitTask(Graph, String, TaskAction, FlowContext)}
     */
    @Deprecated
    default void postTask(Node node, TaskAction action, FlowContext context) {
        submitTask(node.getGraph(), node.getId(), action, context);
    }


    /**
     * 清空状态（主要方便测试）
     *
     * @param graphId 图id
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #stateRepository()}
     */
    @Deprecated
    default void clearState(String graphId, FlowContext context) {
        stateRepository().stateClear(context);
    }

    /**
     * 清空状态（主要方便测试）
     *
     * @param graph   图
     * @param context 流上下文
     * @deprecated 3.8.1 {@link #stateRepository()}
     */
    @Deprecated
    default void clearState(Graph graph, FlowContext context) {
        stateRepository().stateClear(context);
    }
}