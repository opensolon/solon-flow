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
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有状态的服务默认实现
 *
 * @author noear
 * @since 3.4
 * @since 3.5
 */
@Preview("3.4")
public class FlowStatefulServiceDefault implements FlowStatefulService {
    private final FlowEngine flowEngine;
    private final ReentrantLock LOCKER = new ReentrantLock();

    public FlowStatefulServiceDefault(FlowEngine flowEngine) {
        this.flowEngine = flowEngine;
    }

    /// ////////////////////////////////

    @Override
    public FlowEngine engine() {
        return flowEngine;
    }


    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(String graphId, String nodeId, Operation operation, FlowContext context) {
        Node node = flowEngine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        return postOperationIfWaiting(node, operation, context);
    }

    @Override
    public boolean postOperationIfWaiting(Graph graph, String nodeId, Operation operation, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        return postOperationIfWaiting(node, operation, context);
    }

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(Node node, Operation operation, FlowContext context) {
        StatefulTask statefulTask = getTask(node.getGraph(), context);
        if (statefulTask == null) {
            return false;
        }

        if (statefulTask.getState() != StateType.WAITING) {
            return false;
        }

        if (statefulTask.getNode().getId().equals(node.getId()) == false) {
            return false;
        }

        postOperation(statefulTask.getNode(), operation, context);

        return true;
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(String graphId, String nodeId, Operation operation, FlowContext context) {
        Node node = flowEngine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        postOperation(node, operation, context);
    }

    @Override
    public void postOperation(Graph graph, String nodeId, Operation operation, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        postOperation(node, operation, context);
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(Node node, Operation operation, FlowContext context) {
        LOCKER.lock();

        try {
            postOperationDo(new FlowExchanger(context), node, operation);
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * 提交操作
     */
    protected void postOperationDo(FlowExchanger exchanger, Node node, Operation operation) {
        if (operation == Operation.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }

        StateType newState = StateType.byOperation(operation);
        FlowDriver driver = flowEngine.getDriverAs(node.getGraph(), FlowDriver.class);

        //更新状态
        if (operation == Operation.BACK) {
            //后退
            backHandle(driver, node, exchanger);
        } else if (operation == Operation.BACK_JUMP) {
            //跳转后退
            while (true) {
                StatefulTask statefulNode = getTask(node.getGraph(), exchanger.context());
                backHandle(driver, statefulNode.getNode(), exchanger);

                //到目标节点了
                if (statefulNode.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else if (operation == Operation.RESTART) {
            //撤回全部（重新开始）
            exchanger.context().statefulSupporter().stateClear();
        } else if (operation == Operation.FORWARD) {
            //前进
            forwardHandle(driver, node, exchanger, newState);
        } else if (operation == Operation.FORWARD_JUMP) {
            //跳转前进
            while (true) {
                StatefulTask task = getTask(node.getGraph(), exchanger.context());
                forwardHandle(driver, task.getNode(), exchanger, newState);

                //到目标节点了
                if (task.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else {
            //其它（等待或通过或拒绝）
            exchanger.context().statefulSupporter().statePut(node, newState);
        }
    }


    /// ////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulTask> getTasks(String graphId, FlowContext context) {
        return getTasks(flowEngine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulTask> getTasks(Graph graph, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(context);

        exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_LIST_GET, true);

        flowEngine.eval(graph, graph.getStart(), -1, exchanger);
        Collection<StatefulTask> tmp = (Collection<StatefulTask>) exchanger.temporary().vars().get(StateResult.KEY_ACTIVITY_LIST);

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
    public StatefulTask getTask(String graphId, FlowContext context) {
        return getTask(flowEngine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public StatefulTask getTask(Graph graph, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(context);

        flowEngine.eval(graph, graph.getStart(), -1, exchanger);
        return (StatefulTask) exchanger.temporary().vars().get(StateResult.KEY_ACTIVITY_NODE);
    }

    @Override
    public void clearState(String graphId, FlowContext context) {
        this.clearState(flowEngine.getGraphOrThrow(graphId), context);
    }


    @Override
    public void clearState(Graph graph, FlowContext context) {
        context.statefulSupporter().stateClear();
    }

    /// ////////////////////////////////


    /**
     * 前进处理
     */
    protected void forwardHandle(FlowDriver driver, Node node, FlowExchanger exchanger, StateType newState) {
        //如果是完成或跳过，则向前流动
        try {
            driver.postHandleTask(exchanger, node.getTask());
            exchanger.context().statefulSupporter().statePut(node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            Node nextNode = node.getNextNode();
            if (nextNode != null) {
                if (nextNode.getType() == NodeType.INCLUSIVE || nextNode.getType() == NodeType.PARALLEL) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点（且以图做为参数，可能自动流转到网关外）
                    StatefulTask statefulNextNode = getTask(node.getGraph(), exchanger.context());

                    if (statefulNextNode != null) {
                        nextNode = statefulNextNode.getNode();
                    } else {
                        nextNode = null;
                    }
                }

                if (nextNode != null) {
                    if (exchanger.context().statefulSupporter().isAutoForward(nextNode)) {
                        //如果要自动前进
                        flowEngine.eval(nextNode.getGraph(), nextNode, exchanger.context());
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
    protected void backHandle(FlowDriver driver, Node node, FlowExchanger exchanger) {
        //撤回之前的节点
        for (Node n1 : node.getPrevNodes()) {
            //移除状态（要求重来）
            if (n1.getType() == NodeType.ACTIVITY) {
                exchanger.context().statefulSupporter().stateRemove(n1);
            } else if (NodeType.isGateway(n1.getType())) {
                //回退所有子节点
                for (Node n2 : n1.getNextNodes()) {
                    if (n2.getType() == NodeType.ACTIVITY) {
                        exchanger.context().statefulSupporter().stateRemove(n2);
                    }
                }
                //再到前一级
                backHandle(driver, n1, exchanger);
            }
        }
    }


    /// ////////////////////////////////

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(String graphId, FlowContext context) {
        return stepForward(flowEngine.getGraphOrThrow(graphId), context);
    }

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(Graph graph, FlowContext context) {
        StatefulTask statefulTask = getTask(graph, context);

        if (statefulTask != null) {
            postOperation(statefulTask.getNode(), Operation.FORWARD, context);
            statefulTask = new StatefulTask(engine(), statefulTask.getNode(), StateType.COMPLETED);
        }

        return statefulTask;
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(String graphId, FlowContext context) {
        return stepBack(flowEngine.getGraphOrThrow(graphId), context);
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(Graph graph, FlowContext context) {
        StatefulTask statefulTask = getTask(graph, context);

        if (statefulTask != null) {
            postOperation(statefulTask.getNode(), Operation.BACK, context);
            statefulTask = getTask(graph, context);
        }

        return statefulTask;
    }


    /// ////////////////////////////////

    @Override
    public StateResult eval(Graph graph, Node startNode, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(context);

        flowEngine.eval(graph, startNode, -1, exchanger);
        return (StateResult) exchanger.temporary().vars().get(StateResult.KEY_ACTIVITY_NODE);
    }
}