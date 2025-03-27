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

import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单节点状态仓库
 *
 * @author noear
 * @since 3.1
 */
public class SimpleFlowStateRepository implements FlowStateRepository {
    private final Map<String, List<FlowStateRecord>> historyMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> stateMap = new ConcurrentHashMap<>();

    @Override
    public int getState(StatefulFlowContext context, String chainId, String nodeId) {
        String stateKey = context.getInstanceId() + ":" + chainId + ":" + nodeId;

        Integer rst = stateMap.get(stateKey);
        if (rst == null) {
            return FlowNodeState.UNDEFINED;
        } else {
            return rst;
        }
    }

    @Override
    public List<FlowStateRecord> getStateRecords(StatefulFlowContext context) {
        return Collections.unmodifiableList(historyMap.get(context.getInstanceId()));
    }

    @Override
    public void postState(StatefulFlowContext context, String chainId, String nodeId, int nodeState, FlowEngine flowEngine) {
        //获取实例Id
        String instanceId = context.getInstanceId();

        //添加记录
        List<FlowStateRecord> records = historyMap.computeIfAbsent(instanceId, k -> new ArrayList<>());
        records.add(new FlowStateRecord(chainId, nodeId, nodeState, context.getUserId(), System.currentTimeMillis()));

        //更新状态
        if (nodeState == FlowNodeState.WITHDRAW) {
            //撤回
            Node node = flowEngine.getChain(chainId).getNode(nodeId);
            //撤回之前的节点
            for (Node n1 : node.prveNodes()) {
                //移除状态（要求重来）
                String stateKey = instanceId + ":" + chainId + ":" + n1.id();
                stateMap.remove(stateKey);
            }
        } else if (nodeState == FlowNodeState.WITHDRAW_ALL) {
            //撤回全部（重新开始）
            stateMap.clear();
        } else {
            //其它（等待或通过或拒绝）
            String stateKey = instanceId + ":" + chainId + ":" + nodeId;
            stateMap.put(stateKey, nodeState);
        }
    }
}