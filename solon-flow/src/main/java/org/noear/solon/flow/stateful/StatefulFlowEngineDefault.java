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
    private StatefulFlowDriver driver;
    private ReentrantLock LOCKER = new ReentrantLock();

    public StatefulFlowEngineDefault(StatefulFlowDriver driver) {
        super();
        this.driver = driver;
        register(driver);
    }

    /**
     * 获取驱动器
     */
    @Override
    public StatefulFlowDriver getDriver() {
        return driver;
    }

    @Override
    public void register(String name, FlowDriver driver) {
        if ("".equals(name)) {
            //如果是默认的
            if (driver instanceof StatefulFlowDriver) {
                this.driver = (StatefulFlowDriver) driver;
            } else {
                throw new IllegalArgumentException("Default driver must be a StatefulFlowDriver");
            }
        }

        super.register(name, driver);
    }

    /// //////////////

    /**
     * 单步前进
     */
    @Override
    public StatefulNode stepForward(String chainId, FlowContext context) {
        return stepForward(getChain(chainId), context);
    }

    /**
     * 单步前进
     */
    @Override
    public StatefulNode stepForward(Chain chain, FlowContext context) {
        StatefulNode statefulNode = getActivityNode(chain, context);

        if (statefulNode != null) {
            postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);
            statefulNode = new StatefulNode(statefulNode.getNode(), StateType.COMPLETED);
        }

        return statefulNode;
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulNode stepBack(String chainId, FlowContext context) {
        return stepBack(getChain(chainId), context);
    }

    /**
     * 单步后退
     */
    @Override
    public StatefulNode stepBack(Chain chain, FlowContext context) {
        context.backup();
        StatefulNode statefulNode = getActivityNode(chain, context);

        if (statefulNode != null) {
            postOperation(context, statefulNode.getNode(), StateOperation.BACK);
            context.recovery();
            statefulNode = getActivityNode(chain, context);
        }

        return statefulNode;
    }



    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, String chainId, String activityNodeId, StateOperation operation) {
        Node node = getChain(chainId).getNode(activityNodeId);
        return postOperationIfWaiting(context, node, operation);
    }

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postOperationIfWaiting(FlowContext context, Node activity, StateOperation operation) {
        context.backup();

        StatefulNode statefulNode = getActivityNode(activity.getChain(), context);
        if (statefulNode == null) {
            return false;
        }

        if (statefulNode.getState() != StateType.WAITING) {
            return false;
        }

        if (statefulNode.getNode().getId().equals(activity.getId()) == false) {
            return false;
        }

        context.recovery();
        postOperation(context, statefulNode.getNode(), operation);

        return true;
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, String chainId, String activityNodeId, StateOperation operation) {
        Node node = getChain(chainId).getNode(activityNodeId);
        postOperation(context, node, operation);
    }

    /**
     * 提交操作
     */
    @Override
    public void postOperation(FlowContext context, Node activity, StateOperation operation) {
        LOCKER.lock();

        try {
            postOperationDo(context, activity, operation);
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * 提交操作
     */
    protected void postOperationDo(FlowContext context, Node activity, StateOperation operation) {
        if(operation ==  StateOperation.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }

        StateType newState = StateType.codeOf(operation.getCode());

        //更新状态
        if (operation == StateOperation.BACK) {
            //撤回之前的节点
            backHandle(activity, context);
        } else if (operation == StateOperation.RESTART) {
            //撤回全部（重新开始）
            driver.getStateRepository().clearState(context);
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().putState(context, activity, newState);
        }

        //如果是完成或跳过，则向前流动
        if (operation == StateOperation.FORWARD) {
            try {
                driver.postHandleTask(context, activity.getTask());

                Node nextNode = activity.getNextNode();
                if (nextNode != null) {
                    if (driver.getStateController().isAutoForward(context, nextNode)) {
                        //如果要自动前进
                        eval(nextNode, context);
                    }
                }
            } catch (Throwable e) {
                throw new FlowException("Task handle failed: " + activity.getChain().getId() + " / " + activity.getId(), e);
            }
        }
    }

    /// ////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulNode> getActivityNodes(String chainId, FlowContext context) {
        return getActivityNodes(getChain(chainId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<StatefulNode> getActivityNodes(Chain chain, FlowContext context) {
        context.put(StatefulNode.KEY_ACTIVITY_LIST_GET, true);

        eval(chain, context);
        Collection<StatefulNode> tmp = context.get(StatefulNode.KEY_ACTIVITY_LIST);

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
    public StatefulNode getActivityNode(String chainId, FlowContext context) {
        return getActivityNode(getChain(chainId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public StatefulNode getActivityNode(Chain chain, FlowContext context) {
        eval(chain, context);
        return context.get(StatefulNode.KEY_ACTIVITY_NODE);
    }

    /// ////////////////////////////////

    @Override
    public void clearState(FlowContext context) {
        driver.getStateRepository().clearState(context);
    }

    /**
     * 后退处理
     *
     * @param activity 活动节点
     * @param context  流上下文
     */
    protected void backHandle(Node activity, FlowContext context) {
        //撤回之前的节点
        for (Node n1 : activity.getPrevNodes()) {
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
                backHandle(n1, context);
            }
        }
    }
}