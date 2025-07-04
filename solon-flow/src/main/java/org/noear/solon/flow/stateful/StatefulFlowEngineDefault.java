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
 * 有状态的流引擎默认实现
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulFlowEngineDefault extends FlowEngineDefault implements FlowEngine, StatefulFlowEngine {
    private ReentrantLock LOCKER = new ReentrantLock();

    public StatefulFlowEngineDefault(StatefulFlowDriver driver) {
        super(driver);
    }

    /// //////////////

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(String chainId, FlowContext context) {
        return stepForward(getChain(chainId), context);
    }

    /**
     * 单步前进
     */
    @Override
    public StatefulTask stepForward(Chain chain, FlowContext context) {
        StatefulTask statefulTask = getTask(chain, context);

        if (statefulTask != null) {
            postOperation(context, statefulTask.getNode(), StateOperation.FORWARD);
            statefulTask = new StatefulTask(statefulTask.getNode(), StateType.COMPLETED);
        }

        return statefulTask;
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(String chainId, FlowContext context) {
        return stepBack(getChain(chainId), context);
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulTask stepBack(Chain chain, FlowContext context) {
        context.backup();
        StatefulTask statefulTask = getTask(chain, context);

        if (statefulTask != null) {
            postOperation(context, statefulTask.getNode(), StateOperation.BACK);
            context.recovery();
            statefulTask = getTask(chain, context);
        }

        return statefulTask;
    }


    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, String chainId, String nodeId, StateOperation operation) {
        Node node = getChain(chainId).getNode(nodeId);
        return postOperationIfWaiting(context, node, operation);
    }

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, Node node, StateOperation operation) {
        context.backup();

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

        context.recovery();
        postOperation(context, statefulTask.getNode(), operation);

        return true;
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, String chainId, String nodeId, StateOperation operation) {
        Node node = getChain(chainId).getNode(nodeId);
        postOperation(context, node, operation);
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, Node node, StateOperation operation) {
        LOCKER.lock();

        try {
            postOperationDo(context, node, operation);
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * 提交操作
     */
    protected void postOperationDo(FlowContext context, Node node, StateOperation operation) {
        if (operation == StateOperation.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }

        StateType newState = StateType.codeOf(operation.getCode());
        StatefulFlowDriver driver = getDriver(node.getChain(), StatefulFlowDriver.class);

        //更新状态
        if (operation == StateOperation.BACK) {
            //撤回之前的节点
            backHandle(driver, node, context);
        } else if (operation == StateOperation.RESTART) {
            //撤回全部（重新开始）
            driver.getStateRepository().clearState(context);
        } else if (operation == StateOperation.FORWARD) {
            //如果是完成或跳过，则向前流动
            try {
                driver.postHandleTask(context, node.getTask());
                driver.getStateRepository().putState(context, node, newState);

                //重新查找下一个可执行节点（可能为自动前进）
                Node nextNode = node.getNextNode();
                if (nextNode != null) {
                    if (nextNode.getType() == NodeType.INCLUSIVE || nextNode.getType() == NodeType.PARALLEL) {
                        //如果是流入网关，要通过引擎计算获取下个活动节点
                        StatefulTask statefulNextNode = getTask(node.getChain(), new FlowContext().putAll(context.model()));

                        if (statefulNextNode != null) {
                            nextNode = statefulNextNode.getNode();
                        } else {
                            nextNode = null;
                        }
                    }

                    if (nextNode != null) {
                        if (driver.getStateController().isAutoForward(context, nextNode)) {
                            //如果要自动前进
                            eval(nextNode, context);
                        }
                    }
                }
            } catch (Throwable e) {
                throw new FlowException("Task handle failed: " + node.getChain().getId() + " / " + node.getId(), e);
            }
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().putState(context, node, newState);
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
        return getTasks(getChain(chainId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulTask> getTasks(Chain chain, FlowContext context) {
        context.put(StatefulTask.KEY_ACTIVITY_LIST_GET, true);

        eval(chain, context);
        Collection<StatefulTask> tmp = context.get(StatefulTask.KEY_ACTIVITY_LIST);

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
        return getTask(getChain(chainId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public StatefulTask getTask(Chain chain, FlowContext context) {
        eval(chain, context);
        return context.get(StatefulTask.KEY_ACTIVITY_NODE);
    }

    @Override
    public void clearState(String chainId, FlowContext context) {
        this.clearState(getChain(chainId), context);
    }


    @Override
    public void clearState(Chain chain, FlowContext context) {
        StatefulFlowDriver driver = getDriver(chain, StatefulFlowDriver.class);
        driver.getStateRepository().clearState(context);
    }

    /// ////////////////////////////////


    /**
     * 后退处理
     *
     * @param node    流程节点
     * @param context 流上下文
     */
    protected void backHandle(StatefulFlowDriver driver, Node node, FlowContext context) {
        //撤回之前的节点
        for (Node n1 : node.getPrevNodes()) {
            //移除状态（要求重来）
            if (n1.getType() == NodeType.ACTIVITY) {
                driver.getStateRepository().removeState(context, n1);
            } else if (NodeType.isGateway(n1.getType())) {
                //回退所有子节点
                for (Node n2 : n1.getNextNodes()) {
                    if (n2.getType() == NodeType.ACTIVITY) {
                        driver.getStateRepository().removeState(context, n2);
                    }
                }
                //再到前一级
                backHandle(driver, n1, context);
            }
        }
    }
}