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
import org.noear.solon.lang.Nullable;
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
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> findNextTasks(Graph graph, FlowContext context) {
        WorkflowIntent intent = new WorkflowIntent(graph, WorkflowIntent.IntentType.FIND_NEXT_TASKS);

        context.with(WorkflowIntent.INTENT_KEY, intent, () -> {
            FlowDriver driver = getDriver(graph);

            FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
            exchanger.recordNode(graph, graph.getStart());

            engine.eval(graph, exchanger);
        });

        return intent.nextTasks;
    }

    @Override
    public @Nullable Task findTask(Graph graph, FlowContext context) {
        WorkflowIntent intent = new WorkflowIntent(graph, WorkflowIntent.IntentType.FIND_TASK);

        context.with(WorkflowIntent.INTENT_KEY, intent, () -> {
            FlowDriver driver = getDriver(graph);

            FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
            exchanger.recordNode(graph, graph.getStart());

            engine.eval(graph, exchanger);
        });

        return intent.task;
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task claimTask(Graph graph, FlowContext context) {
        WorkflowIntent intent = new WorkflowIntent(graph, WorkflowIntent.IntentType.CLAIM_TASK);

        context.with(WorkflowIntent.INTENT_KEY, intent, () -> {
            FlowDriver driver = getDriver(graph);

            FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));
            exchanger.recordNode(graph, graph.getStart());

            engine.eval(graph, exchanger);
        });

        return intent.task;
    }

    @Override
    public TaskState getState(Node node, FlowContext context) {
        return stateRepository.stateGet(context, node);
    }

    /// ////////////////////////////////

    /**
     * 提交任务（如果当前任务为等待介入）
     */
    @Override
    public boolean submitTaskIfWaiting(Task task, TaskAction action, FlowContext context) {
        if (task == null || task.getState() != TaskState.WAITING) {
            //如果无权
            return false;
        }

        LOCKER.lock();

        try {
            if (stateRepository.stateGet(context, task.getNode()) != TaskState.WAITING ||
                    stateController.isOperatable(context, task.getNode()) == false) {
                //如果不是等待（双重确认）
                return false;
            }

            WorkflowIntent intent = new WorkflowIntent(task.getRootGraph(), WorkflowIntent.IntentType.SUBMIT_TASK);
            context.with(WorkflowIntent.INTENT_KEY, intent, () -> {
                submitTaskDo(task.getRootGraph(), task.getNode(), action, context);
            });
        } finally {
            LOCKER.unlock();
        }
        return true;
    }

    @Override
    public void submitTask(Graph graph, Node node, TaskAction action, FlowContext context) {
        LOCKER.lock();

        try {
            WorkflowIntent intent = new WorkflowIntent(graph, WorkflowIntent.IntentType.SUBMIT_TASK);
            context.with(WorkflowIntent.INTENT_KEY, intent, () -> {
                submitTaskDo(graph, node, action, context);
            });
        } finally {
            LOCKER.unlock();
        }
    }

    protected void submitTaskDo(Graph graph, Node node, TaskAction action, FlowContext context) {
        if (action == TaskAction.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }
        FlowDriver driver = getDriver(graph);
        FlowExchanger exchanger = new FlowExchanger(graph, engine, driver, context, -1, new AtomicInteger(0));

        TaskState newState = action.getTargetState();

        //更新状态
        if (action == TaskAction.BACK) {
            //后退
            backHandle(graph, node, exchanger);
        } else if (action == TaskAction.BACK_JUMP) {
            //跳转后退
            Task lastTask = null;
            while (true) {
                Task task = findTask(graph, exchanger.context());
                if (task != null) {
                    if (lastTask != null && lastTask.getNode().equals(task.getNode())) {
                        break;
                    }

                    lastTask = task;
                    backHandle(graph, task.getNode(), exchanger);

                    //到目标节点了
                    if (task.getNode().equals(node)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (action == TaskAction.RESTART) {
            //撤回全部（重新开始）
            stateRepository.stateClear(exchanger.context());
        } else if (action == TaskAction.FORWARD) {
            //前进
            forwardHandle(graph, node, newState, exchanger);
        } else if (action == TaskAction.FORWARD_JUMP) {
            //跳转前进
            Task lastTask = null;
            while (true) {
                Task task = findTask(graph, exchanger.context());
                if (task != null) {
                    if (lastTask != null && lastTask.getNode().equals(task.getNode())) {
                        break;
                    }

                    lastTask = task;
                    forwardHandle(graph, task.getNode(), newState, exchanger);

                    //到目标节点了
                    if (task.getNode().equals(node)) {
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


    /// /////////////////

    /**
     * 前进处理
     */
    protected void forwardHandle(Graph graph, Node node, TaskState newState, FlowExchanger exchanger) {
        //如果是完成或跳过，则向前流动
        try {
            exchanger.reverting(false);
            exchanger.driver().postHandleTask(exchanger, node.getTask());
            stateRepository.statePut(exchanger.context(), node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            for (Node nextNode : node.getNextNodes()) {
                if (NodeType.isGateway(nextNode.getType())) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点（且以图做为参数，可能自动流转到网关外）
                    Task task = findTask(graph, exchanger.context());

                    if (task != null) {
                        if (task.getState() == TaskState.TERMINATED) {
                            //终止的话，禁止前进了
                            break;
                        }
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
    protected void backHandle(Graph graph, Node node, FlowExchanger exchanger) {
        backHandleDo(graph, node, exchanger, new HashSet<>());
    }

    /**
     * 后退处理
     *
     * @param node      流程节点
     * @param exchanger 流交换器
     */
    protected void backHandleDo(Graph graph, Node node, FlowExchanger exchanger, Set<Node> visited) {
        if (visited.contains(node)) {
            return;
        } else {
            visited.add(node);
        }

        //撤回自己
        stateRepository.stateRemove(exchanger.context(), node);

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
                backHandleDo(graph, n1, exchanger, visited);
            }
        }
    }
}