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

    /// //////////////

    @Override
    public FlowEngine engine() {
        return flowEngine;
    }

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(String chainId, FlowContext context) {
        return stepForward(flowEngine.getChain(chainId), context);
    }

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(Chain chain, FlowContext context) {
        StatefulTask statefulTask = getTask(chain, context);

        if (statefulTask != null) {
            postOperation(context, statefulTask.getNode(), Operation.FORWARD);
            statefulTask = new StatefulTask(engine(), statefulTask.getNode(), StateType.COMPLETED);
        }

        return statefulTask;
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(String chainId, FlowContext context) {
        return stepBack(flowEngine.getChain(chainId), context);
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(Chain chain, FlowContext context) {
        StatefulTask statefulTask = getTask(chain, context);

        if (statefulTask != null) {
            postOperation(context, statefulTask.getNode(), Operation.BACK);
            statefulTask = getTask(chain, context);
        }

        return statefulTask;
    }


    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, String chainId, String nodeId, Operation operation) {
        Node node = flowEngine.getChain(chainId).getNode(nodeId);
        return postOperationIfWaiting(context, node, operation);
    }

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, Node node, Operation operation) {
        StatefulTask statefulTask = getTask(node.getChain(), context);
        if (statefulTask == null) {
            return false;
        }

        if (statefulTask.getState() != StateType.WAITING) {
            return false;
        }

        if (statefulTask.getNode().getId().equals(node.getId()) == false) {
            return false;
        }

        postOperation(context, statefulTask.getNode(), operation);

        return true;
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, String chainId, String nodeId, Operation operation) {
        Node node = flowEngine.getChain(chainId).getNode(nodeId);
        postOperation(context, node, operation);
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, Node node, Operation operation) {
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
        FlowDriver driver = flowEngine.getDriverAs(node.getChain(), FlowDriver.class);

        //更新状态
        if (operation == Operation.BACK) {
            //后退
            backHandle(driver, node, exchanger);
        } else if (operation == Operation.BACK_JUMP) {
            //跳转后退
            while (true) {
                StatefulTask statefulNode = getTask(node.getChain(), exchanger.context());
                backHandle(driver, statefulNode.getNode(), exchanger);

                //到目标节点了
                if (statefulNode.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else if (operation == Operation.RESTART) {
            //撤回全部（重新开始）
            exchanger.context().getStateRepository().clearState(exchanger.context());
        } else if (operation == Operation.FORWARD) {
            //前进
            forwardHandle(driver, node, exchanger, newState);
        } else if (operation == Operation.FORWARD_JUMP) {
            //跳转前进
            while (true) {
                StatefulTask task = getTask(node.getChain(), exchanger.context());
                forwardHandle(driver, task.getNode(), exchanger, newState);

                //到目标节点了
                if (task.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else {
            //其它（等待或通过或拒绝）
            exchanger.context().getStateRepository().putState(exchanger.context(), node, newState);
        }
    }

    /// ////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulTask> getTasks(String chainId, FlowContext context) {
        return getTasks(flowEngine.getChain(chainId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulTask> getTasks(Chain chain, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(context);

        exchanger.temporary().vars().put(StatefulTask.KEY_ACTIVITY_LIST_GET, true);

        flowEngine.eval(chain.getStart(), -1, exchanger);
        Collection<StatefulTask> tmp = (Collection<StatefulTask>) exchanger.temporary().vars().get(StatefulTask.KEY_ACTIVITY_LIST);

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
    public StatefulTask getTask(String chainId, FlowContext context) {
        return getTask(flowEngine.getChain(chainId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public StatefulTask getTask(Chain chain, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(context);

        flowEngine.eval(chain.getStart(), -1, exchanger);
        return (StatefulTask) exchanger.temporary().vars().get(StatefulTask.KEY_ACTIVITY_NODE);
    }

    @Override
    public void clearState(String chainId, FlowContext context) {
        this.clearState(flowEngine.getChain(chainId), context);
    }


    @Override
    public void clearState(Chain chain, FlowContext context) {
        context.getStateRepository().clearState(context);
    }

    /// ////////////////////////////////


    /**
     * 前进处理
     *
     */
    protected void forwardHandle(FlowDriver driver, Node node, FlowExchanger exchanger, StateType newState) {
        //如果是完成或跳过，则向前流动
        try {
            driver.postHandleTask(exchanger, node.getTask());
            exchanger.context().getStateRepository().putState(exchanger.context(), node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            Node nextNode = node.getNextNode();
            if (nextNode != null) {
                if (nextNode.getType() == NodeType.INCLUSIVE || nextNode.getType() == NodeType.PARALLEL) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点
                    StatefulTask statefulNextNode = getTask(node.getChain(), exchanger.context());

                    if (statefulNextNode != null) {
                        nextNode = statefulNextNode.getNode();
                    } else {
                        nextNode = null;
                    }
                }

                if (nextNode != null) {
                    if (exchanger.context().getStateController().isAutoForward(exchanger.context(), nextNode)) {
                        //如果要自动前进
                        flowEngine.eval(nextNode, exchanger.context());
                    }
                }
            }
        } catch (Throwable e) {
            throw new FlowException("Task handle failed: " + node.getChain().getId() + " / " + node.getId(), e);
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
                exchanger.context().getStateRepository().removeState(exchanger.context(), n1);
            } else if (NodeType.isGateway(n1.getType())) {
                //回退所有子节点
                for (Node n2 : n1.getNextNodes()) {
                    if (n2.getType() == NodeType.ACTIVITY) {
                        exchanger.context().getStateRepository().removeState(exchanger.context(), n2);
                    }
                }
                //再到前一级
                backHandle(driver, n1, exchanger);
            }
        }
    }
}