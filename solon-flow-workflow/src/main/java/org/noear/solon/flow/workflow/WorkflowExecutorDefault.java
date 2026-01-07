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

import org.noear.solon.flow.*;
import org.noear.solon.lang.Preview;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 工作流服务默认实现
 *
 * @author noear
 * @since 3.4
 * @since 3.5
 * @since 3.8
 */
@Preview("3.4")
public class WorkflowExecutorDefault implements WorkflowExecutor, WorkflowService {
    private final transient FlowEngine engine;
    private final StateController stateController;
    private final StateRepository stateRepository;

    private final transient ReentrantLock LOCKER = new ReentrantLock();

    public WorkflowExecutorDefault(FlowEngine engine, StateController stateController, StateRepository stateRepository) {
        this.engine = engine;
        this.stateController = stateController;
        this.stateRepository = stateRepository;
    }

    /// ////////////////////////////////

    @Override
    public FlowEngine engine() {
        return engine;
    }

    @Override
    public StateController stateController() {
        return stateController;
    }

    @Override
    public StateRepository stateRepository() {
        return stateRepository;
    }

    private FlowDriver getDriver(Graph graph) {
        return new WorkflowDriver(engine.getDriver(graph), stateController, stateRepository);
    }


    /// ////////////////////////

    /**
     * 提交任务（如果当前任务为等待介入）
     */
    @Override
    public boolean submitTaskIfWaiting(Task task, TaskAction action, FlowContext context) {
        if (task == null || stateController.isOperatable(context, task.getNode()) == false) {
            //如果无权
            return false;
        }

        if (task.getState() != TaskState.WAITING || stateRepository.stateGet(context, task.getNode()) != TaskState.WAITING) {
            //如果不是等待（双重确认）
            return false;
        }

        submitTask(task.getNode(), action, context);

        return true;
    }

    @Override
    public void submitTask(String graphId, String nodeId, TaskAction action, FlowContext context) {
        Node node = engine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        submitTask(node, action, context);
    }

    @Override
    public void submitTask(Graph graph, String nodeId, TaskAction action, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        submitTask(node, action, context);
    }

    @Override
    public void submitTask(Node node, TaskAction action, FlowContext context) {
        FlowDriver driver = getDriver(node.getGraph());

        LOCKER.lock();

        try {
            postTaskDo(new FlowExchanger(node.getGraph(), engine, driver, context, -1, new AtomicInteger(0)), node, action);
        } finally {
            LOCKER.unlock();
        }
    }

    protected void postTaskDo(FlowExchanger exchanger, Node node, TaskAction action) {
        if (action == TaskAction.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }

        TaskState newState = TaskState.fromAction(action);

        //更新状态
        if (action == TaskAction.BACK) {
            //后退
            backHandle(node, exchanger);
        } else if (action == TaskAction.BACK_JUMP) {
            //跳转后退
            while (true) {
                Task task = matchTask(node.getGraph(), exchanger.context());
                backHandle(task.getNode(), exchanger);

                //到目标节点了
                if (task.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else if (action == TaskAction.RESTART) {
            //撤回全部（重新开始）
            stateRepository.stateClear(exchanger.context());
        } else if (action == TaskAction.FORWARD) {
            //前进
            forwardHandle(node, exchanger, newState);
        } else if (action == TaskAction.FORWARD_JUMP) {
            //跳转前进
            while (true) {
                Task task = matchTask(node.getGraph(), exchanger.context());
                if (task != null) {
                    forwardHandle(task.getNode(), exchanger, newState);

                    //到目标节点了
                    if (task.getNode().getId().equals(node.getId())) {
                        break;
                    }
                } else {
                    //没有权限
                    break;
                }
            }
        } else {
            //其它（等待或通过或拒绝）
            stateRepository.statePut(exchanger.context(), node, newState);
        }
    }


    /// ////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> findNextTasks(String graphId, FlowContext context) {
        return findNextTasks(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> findNextTasks(Graph graph, FlowContext context) {
        FlowDriver driver = getDriver(graph);

        FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
        exchanger.recordNode(graph, graph.getStart());

        try {
            WorkflowIntent intent = new WorkflowIntent(WorkflowIntent.IntentType.GET_NEXT_TASKS);
            context.put(WorkflowIntent.INTENT_KEY, intent);

            engine.eval(graph, exchanger);

            return intent.nextTasks;
        } finally {
            context.remove(WorkflowIntent.INTENT_KEY);
        }
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task matchTask(String graphId, FlowContext context) {
        return matchTask(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task matchTask(Graph graph, FlowContext context) {
        FlowDriver driver = getDriver(graph);

        FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
        exchanger.recordNode(graph, graph.getStart());

        try {
            WorkflowIntent intent = new WorkflowIntent(WorkflowIntent.IntentType.Get_TASK);
            context.put(WorkflowIntent.INTENT_KEY, intent);

            engine.eval(graph, exchanger);

            return intent.task;
        } finally {
            context.remove(WorkflowIntent.INTENT_KEY);
        }
    }

    @Override
    public TaskState getState(Node node, FlowContext context) {
        return stateRepository.stateGet(context, node);
    }

    /// ////////////////////////////////


    /**
     * 前进处理
     */
    protected void forwardHandle(Node node, FlowExchanger exchanger, TaskState newState) {
        //如果是完成或跳过，则向前流动
        try {
            exchanger.reverting(false);
            exchanger.driver().postHandleTask(exchanger, node.getTask());
            stateRepository.statePut(exchanger.context(), node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            for (Node nextNode : node.getNextNodes()) {
                if (NodeType.isGateway(nextNode.getType())) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点（且以图做为参数，可能自动流转到网关外）
                    Task task = matchTask(node.getGraph(), exchanger.context());

                    if (task != null) {
                        nextNode = task.getNode();
                    } else {
                        nextNode = null;
                    }
                }

                if (nextNode != null) {
                    if (stateController.isAutoForward(exchanger.context(), nextNode)) {
                        //如果要自动前进
                        exchanger.recordNode(nextNode.getGraph(), nextNode);
                        engine.eval(nextNode.getGraph(), exchanger.copy(nextNode.getGraph()).reverting(false));
                    }
                }
            }
        } catch (Throwable e) {
            throw new FlowException("Task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 后退处理
     *
     * @param node      流程节点
     * @param exchanger 流交换器
     */
    protected void backHandle(Node node, FlowExchanger exchanger) {
        //撤回之前的节点
        for (Node n1 : node.getPrevNodes()) {
            //移除状态（要求重来）
            if (n1.getType() == NodeType.ACTIVITY) {
                stateRepository.stateRemove(exchanger.context(), n1);
            } else if (NodeType.isGateway(n1.getType())) {
                //回退所有子节点
                for (Node n2 : n1.getNextNodes()) {
                    if (n2.getType() == NodeType.ACTIVITY) {
                        stateRepository.stateRemove(exchanger.context(), n2);
                    }
                }
                //再到前一级
                backHandle(n1, exchanger);
            }
        }
    }
}