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
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 有状态的流引擎（也可以用于无状态）
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulFlowEngine extends FlowEngineDefault implements FlowEngine {
    private StatefulSimpleFlowDriver driver;
    private ReentrantLock LOCKER = new ReentrantLock();

    public StatefulFlowEngine(StatefulSimpleFlowDriver driver) {
        super();
        this.driver = driver;
        register(driver);
    }

    /**
     * 获取状态仓库
     */
    public StateRepository getRepository() {
        return driver.getStateRepository();
    }

    /// //////////////

    /**
     * 单步前进
     */
    public StatefulNode stepForward(String chainId, FlowContext context) {
        return stepForward(getChain(chainId), context);
    }

    /**
     * 单步前进
     */
    public StatefulNode stepForward(Chain chain, FlowContext context) {
        StatefulNode statefulNode = getActivityNode(chain, context);

        if (statefulNode != null) {
            postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);
            statefulNode = new StatefulNode(statefulNode.getNode(), NodeState.COMPLETED);
        }

        return statefulNode;
    }

    /**
     * 单步后退
     */
    public StatefulNode stepBack(String chainId, FlowContext context) {
        return stepBack(getChain(chainId), context);
    }

    /**
     * 单步后退
     */
    public StatefulNode stepBack(Chain chain, FlowContext context) {
        StatefulNode statefulNode = getActivityNode(chain, context);

        if (statefulNode != null) {
            postActivityState(context, statefulNode.getNode(), NodeState.RETURNED);
            statefulNode = new StatefulNode(statefulNode.getNode(), NodeState.RETURNED);
        }

        return statefulNode;
    }

    /// ////////////////////////

    /**
     * 获取活动节点
     */
    public StatefulNode getActivityNode(String chainId, FlowContext context) {
        return getActivityNode(getChain(chainId), context);
    }

    /**
     * 获取活动节点
     */
    public StatefulNode getActivityNode(Chain chain, FlowContext context) {
        eval(chain, context);
        return context.get(StatefulNode.KEY_ACTIVITY_NODE);
    }

    /**
     * 提交活动状态
     */
    public void postActivityState(FlowContext context, String chainId, String activityNodeId, int state) {
        Node node = getChain(chainId).getNode(activityNodeId);
        postActivityState(context, node, state);
    }

    /**
     * 提交活动状态
     */
    public void postActivityState(FlowContext context, Node activity, int state) {
        LOCKER.lock();

        try {
            postActivityStateDo(context, activity, state);
        } finally {
            LOCKER.unlock();
        }
    }

    /**
     * 提交活动状态
     */
    protected void postActivityStateDo(FlowContext context, Node activity, int state) {
        int oldNodeState = driver.getStateRepository().getState(context, activity);
        if (oldNodeState == state) {
            //如果要状态没变化，不处理
            return;
        }

        //添加记录
        @Nullable StateRecord stateRecord = driver.getStateOperator().createRecord(context, activity, state);
        driver.getStateRepository().addStateRecord(context, stateRecord);

        //节点

        //更新状态
        if (state == NodeState.RETURNED) {
            //撤回之前的节点
            for (Node n1 : activity.getPrevNodes()) {
                //移除状态（要求重来）
                driver.getStateRepository().removeState(context, n1);
            }
        } else if (state == NodeState.RESTART) {
            //撤回全部（重新开始）
            driver.getStateRepository().clearState(context);
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().putState(context, activity, state);
        }

        //如果是完成或跳过，则向前流动
        if (state == NodeState.COMPLETED) {
            try {
                postHandleTask(context, activity.getTask());

                Node nextNode = activity.getNextNode();
                if (nextNode != null) {
                    if (driver.getStateOperator().isAutoForward(context, nextNode)) {
                        //如果要自动前进
                        eval(nextNode, context);
                    }
                }
            } catch (Throwable e) {
                throw new FlowException("Task handle failed: " + activity.getChain().getId() + " / " + activity.getId(), e);
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