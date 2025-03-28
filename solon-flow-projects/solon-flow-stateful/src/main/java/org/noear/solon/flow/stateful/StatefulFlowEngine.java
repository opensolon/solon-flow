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

import org.noear.solon.flow.FlowEngineDefault;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.Task;
import org.noear.solon.lang.Preview;

import java.util.List;

/**
 * 有状态的流引擎（也可以用于无状态）
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulFlowEngine extends FlowEngineDefault {
    private StatefulSimpleFlowDriver driver;

    public StatefulFlowEngine(StatefulSimpleFlowDriver driver) {
        super();
        this.driver = driver;
        register(driver);
    }

    /**
     * 获取状态
     */
    public int getState(StatefulFlowContext context, String chainId, String nodeId) {
        return driver.getStateRepository().getState(context, chainId, nodeId);
    }

    /**
     * 获取状态记录
     */
    public List<StateRecord> getStateRecords(StatefulFlowContext context) {
        return driver.getStateRepository().getStateRecords(context);
    }

    /**
     * 提交状态
     */
    public void postState(StatefulFlowContext context, String chainId, String nodeId, int nodeState) throws Throwable {
        //添加记录
        StateRecord stateRecord = driver.getStateOperator().createRecord(context, chainId, nodeId, nodeState);
        driver.getStateRepository().addStateRecord(context, stateRecord);

        //节点
        Node node = getChain(chainId).getNode(nodeId);

        //更新状态
        if (nodeState == NodeStates.WITHDRAW) {
            //撤回之前的节点
            for (Node n1 : node.getPrveNodes()) {
                //移除状态（要求重来）
                driver.getStateRepository().removeState(context, chainId, n1.getId());
            }
        } else if (nodeState == NodeStates.WITHDRAW_ALL) {
            //撤回全部（重新开始）
            driver.getStateRepository().clearState(context);
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().putState(context, chainId, nodeId, nodeState);
        }

        //如果是通过，则提交任务
        if (nodeState == NodeStates.PASS) {
            postHandleTask(context, node.getTask());
        }
    }

    /**
     * 提交处理任务
     */
    protected void postHandleTask(StatefulFlowContext context, Task task) throws Throwable {
        driver.postHandleTask(context, task);
    }
}