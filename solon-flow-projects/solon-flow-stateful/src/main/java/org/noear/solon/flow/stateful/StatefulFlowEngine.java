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

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有状态的流引擎（也可以用于无状态）
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulFlowEngine extends FlowEngineDefault {
    private StatefulSimpleFlowDriver driver;
    private ReentrantLock LOCKER = new ReentrantLock();

    public StatefulFlowEngine(StatefulSimpleFlowDriver driver) {
        super();
        this.driver = driver;
        register(driver);
    }

    /**
     * 获取活动节点
     */
    public StatefulNode getActivityNode(String chainId, FlowContext context) {
        eval(chainId, context);
        return context.get(StatefulSimpleFlowDriver.KEY_ACTIVITY_NODE);
    }

    /**
     * 获取活动节点
     */
    public StatefulNode getActivityNode(Chain chain, FlowContext context) {
        eval(chain, context);
        return context.get(StatefulSimpleFlowDriver.KEY_ACTIVITY_NODE);
    }

    /**
     * 获取状态记录
     */
    public List<StateRecord> getStateRecords(FlowContext context) {
        return driver.getStateRepository().getStateRecords(context);
    }

    /**
     * 获取节点状态
     */
    public int getNodeState(FlowContext context, String chainId, String nodeId) {
        Node node = getChain(chainId).getNode(nodeId);
        return getNodeState(context, node);
    }

    /**
     * 获取节点状态
     */
    public int getNodeState(FlowContext context, Node node) {
        return driver.getStateRepository().getState(context, node);
    }

    /**
     * 提交节点状态
     */
    public void postNodeState(FlowContext context, String chainId, String nodeId, int nodeState) {
        Node node = getChain(chainId).getNode(nodeId);
        postNodeState(context, node, nodeState);
    }

    /**
     * 提交节点状态
     */
    public void postNodeState(FlowContext context, Node node, int nodeState) {
        LOCKER.lock();

        try {
            postNodeStateDo(context, node, nodeState);
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * 提交节点状态
     */
    protected void postNodeStateDo(FlowContext context, Node node, int nodeState) {
        int oldNodeState = driver.getStateRepository().getState(context, node);
        if (oldNodeState == nodeState) {
            //如果要状态没变化，不处理
            return;
        }

        //添加记录
        StateRecord stateRecord = driver.getStateOperator().createRecord(context, node, nodeState);
        driver.getStateRepository().addStateRecord(context, stateRecord);

        //节点

        //更新状态
        if (nodeState == NodeStates.WITHDRAW) {
            //撤回之前的节点
            for (Node n1 : node.getPrveNodes()) {
                //移除状态（要求重来）
                driver.getStateRepository().removeState(context, node);
            }
        } else if (nodeState == NodeStates.WITHDRAW_ALL) {
            //撤回全部（重新开始）
            driver.getStateRepository().clearState(context);
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().putState(context, node, nodeState);
        }

        //如果是通过，则提交任务
        if (nodeState == NodeStates.PASS) {
            try {
                postHandleTask(context, node.getTask());
            } catch (Throwable e) {
                throw new FlowException("Task handle failed: " + node.getChain().getId() + " / " + node.getId(), e);
            }
        }
    }

    /**
     * 提交处理任务
     */
    protected void postHandleTask(FlowContext context, Task task) throws Throwable {
        driver.postHandleTask(context, task);
    }
}