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
public class WorkflowServiceDefault implements WorkflowService {
    private final transient FlowEngine engine;
    private final StateController stateController;
    private final StateRepository stateRepository;

    private final transient ReentrantLock LOCKER = new ReentrantLock();

    public WorkflowServiceDefault(FlowEngine engine, StateController stateController, StateRepository stateRepository) {
        this.engine = engine;
        this.stateController = stateController;
        this.stateRepository = stateRepository;
    }

    /// ////////////////////////////////

    @Override
    public FlowEngine engine() {
        return engine;
    }


    private FlowDriver getDriver(Graph graph) {
        return new WorkflowDriver(engine.getDriver(graph), stateController, stateRepository);
    }


    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postTaskIfWaiting(String graphId, String nodeId, TaskAction action, FlowContext context) {
        Node node = engine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        return postTaskIfWaiting(node, action, context);
    }

    @Override
    public boolean postTaskIfWaiting(Graph graph, String nodeId, TaskAction action, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        return postTaskIfWaiting(node, action, context);
    }

    @Override
    public boolean postTaskIfWaiting(Node node, TaskAction action, FlowContext context) {
        Task task = getTask(node.getGraph(), context);
        if (task == null) {
            return false;
        }

        if (task.getState() != TaskState.WAITING) {
            return false;
        }

        if (task.getNode().getId().equals(node.getId()) == false) {
            return false;
        }

        postTask(task.getNode(), action, context);

        return true;
    }

    @Override
    public void postTask(String graphId, String nodeId, TaskAction action, FlowContext context) {
        Node node = engine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        postTask(node, action, context);
    }

    @Override
    public void postTask(Graph graph, String nodeId, TaskAction action, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        postTask(node, action, context);
    }

    @Override
    public void postTask(Node node, TaskAction action, FlowContext context) {
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
                Task task = getTask(node.getGraph(), exchanger.context());
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
                Task task = getTask(node.getGraph(), exchanger.context());
                if(task != null) {
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
    public Collection<Task> getNextTasks(String graphId, FlowContext context) {
        return getNextTasks(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> getNextTasks(Graph graph, FlowContext context) {
        FlowDriver driver = getDriver(graph);

        FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
        WorkflowCommand command  = new WorkflowCommand(WorkflowCommand.CommandType.GET_NEXT_TASKS);

        exchanger.temporary().vars().put(WorkflowCommand.class.getSimpleName(), command);
        exchanger.recordNode(graph, graph.getStart());

        engine.eval(graph, exchanger);

        Collection<Task> tmp = command.nextTasks;

        if (tmp == null) {
            return Collections.emptyList();
        } else {
            return tmp;
        }
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task getTask(String graphId, FlowContext context) {
        return getTask(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task getTask(Graph graph, FlowContext context) {
        FlowDriver driver = getDriver(graph);

        FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
        exchanger.recordNode(graph, graph.getStart());
        WorkflowCommand command  = new WorkflowCommand(WorkflowCommand.CommandType.Get_TASK);
        exchanger.temporary().vars().put(WorkflowCommand.class.getSimpleName(), command);

        engine.eval(graph, exchanger);

        return command.task;
    }

    @Override
    public TaskState getState(Node node, FlowContext context) {
        return stateRepository.stateGet(context, node);
    }

    @Override
    public void clearState(String graphId, FlowContext context) {
        this.clearState(engine.getGraphOrThrow(graphId), context);
    }


    @Override
    public void clearState(Graph graph, FlowContext context) {
        stateRepository.stateClear(context);
    }

    /// ////////////////////////////////


    /**
     * 前进处理
     */
    protected void forwardHandle(Node node, FlowExchanger exchanger, TaskState newState) {
        //如果是完成或跳过，则向前流动
        try {
            exchanger.driver().postHandleTask(exchanger, node.getTask());
            stateRepository.statePut(exchanger.context(), node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            for (Node nextNode : node.getNextNodes()) {
                if (NodeType.isGateway(nextNode.getType())) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点（且以图做为参数，可能自动流转到网关外）
                    Task task = getTask(node.getGraph(), exchanger.context());

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
                        engine.eval(nextNode.getGraph(), exchanger.copy(nextNode.getGraph()));
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