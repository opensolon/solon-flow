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
 * <p>提供工作流任务的执行框架能力，包括：
 * 1. 任务提交执行（前进、后退、跳转等）
 * 2. 当前活动任务匹配
 * 3. 后续可达任务查找
 *
 * <p>典型使用场景：
 * <pre>{@code
 * // 1. 创建执行器
 * WorkflowExecutor workflow = WorkflowExecutor.of(engine, controller, repository);
 *
 * // 2. 认领任务（检查是否有可操作的待处理任务）
 * Task current = workflow.claimTask(graph, context);
 * if (current != null) {
 *     // 3. 提交任务处理
 *     workflow.submitTask(current, TaskAction.FORWARD, context);
 * }
 *
 * // 4. 查找后续可能任务（下一步）
 * Collection<Task> nextTasks = workflow.findNextTasks(graph, context);
 * }</pre>
 *
 * <p><b>注意：</b>本执行器专注于流程执行逻辑，不包含实例管理等业务功能。
 *
 * @since 3.8.1
 */
@Preview("3.4")
public interface WorkflowExecutor {
    static WorkflowExecutor of(FlowEngine engine, StateController stateController, StateRepository stateRepository) {
        return new WorkflowExecutorDefault(engine, stateController, stateRepository);
    }

    /**
     * 流程引擎
     */
    FlowEngine engine();

    /**
     * 状态控制器
     */
    StateController stateController();

    /**
     * 状态仓库
     */
    StateRepository stateRepository();


    /// ////////////////////////////////


    /**
     * 认领当前活动任务（权限匹配，并锁定状态为等待）
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     */
    @Nullable
    default Task claimTask(String graphId, FlowContext context) {
        return claimTask(engine().getGraphOrThrow(graphId), context);
    }

    /**
     * 认领当前活动任务（权限匹配，并锁定状态为等待）
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     */
    @Nullable
    Task claimTask(Graph graph, FlowContext context);

    /**
     * 寻找当前确定的任务（逻辑探测）
     *
     * @param graphId 图id
     * @param context 流上下文（要有人员配置）
     */
    @Nullable
    default Task findTask(String graphId, FlowContext context) {
        return findTask(engine().getGraphOrThrow(graphId), context);
    }

    /**
     * 寻找当前确定的任务（逻辑探测）
     *
     * @param graph   图
     * @param context 流上下文（要有人员配置）
     */
    @Nullable
    Task findTask(Graph graph, FlowContext context);

    /**
     * 寻找下一步可能的任务列表（逻辑探测）
     *
     * @param graphId 图id
     * @param context 流上下文（不需要有人员配置）
     */
    default Collection<Task> findNextTasks(String graphId, FlowContext context) {
        return findNextTasks(engine().getGraphOrThrow(graphId), context);
    }

    /**
     * 寻找下一步可能的任务列表（寻找所有可能性）
     *
     * @param graph   图
     * @param context 流上下文（逻辑探测）
     */
    Collection<Task> findNextTasks(Graph graph, FlowContext context);


    /// ////////////////////////////////

    /**
     * 获取状态
     */
    TaskState getState(Node node, FlowContext context);

    /// ////////////////////////////////

    /**
     * 提交任务（如果当前任务为等待介入）
     *
     * @param task    任务
     * @param action  动作
     * @param context 流上下文
     */
    boolean submitTaskIfWaiting(Task task, TaskAction action, FlowContext context);

    /**
     * 提交任务
     *
     * @param task    任务
     * @param action  动作
     * @param context 流上下文
     */
    default void submitTask(Task task, TaskAction action, FlowContext context) {
        submitTask(task.getRootGraph(), task.getNode(), action, context);
    }

    /**
     * 提交任务
     *
     * @param graphId 图id
     * @param nodeId  节点id
     * @param action  动作
     * @param context 流上下文
     */
    default void submitTask(String graphId, String nodeId, TaskAction action, FlowContext context) {
        submitTask(engine().getGraphOrThrow(graphId), nodeId, action, context);
    }

    /**
     * 提交任务
     *
     * @param graph   图
     * @param nodeId  节点id
     * @param action  动作
     * @param context 流上下文
     */
    default void submitTask(Graph graph, String nodeId, TaskAction action, FlowContext context) {
        submitTask(graph, graph.getNodeOrThrow(nodeId), action, context);
    }

    /**
     * 提交任务
     *
     * @param graph   图
     * @param node    节点
     * @param action  动作
     * @param context 流上下文
     */
    void submitTask(Graph graph, Node node, TaskAction action, FlowContext context);
}