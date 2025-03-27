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
package org.noear.solon.flow.stateful.repository;

import org.noear.solon.flow.stateful.StateRecord;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.flow.stateful.NodeStates;
import org.noear.solon.flow.stateful.StatefulFlowContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单状态仓库
 *
 * @author noear
 * @since 3.1
 */
public class InMemoryStateRepository<T extends StateRecord> implements StateRepository<T> {
    private final Map<String, List<T>> historyMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> stateMap = new ConcurrentHashMap<>();

    private List<T> getHistory(String instanceId) {
        return historyMap.computeIfAbsent(instanceId, k -> new ArrayList<>());
    }

    public Map<String, Integer> getStates(String instanceId) {
        return stateMap.computeIfAbsent(instanceId, k -> new ConcurrentHashMap<>());
    }

    @Override
    public int getState(StatefulFlowContext context, String chainId, String nodeId) {
        String stateKey = chainId + ":" + nodeId;

        Integer rst = getStates(context.getInstanceId()).get(stateKey);
        if (rst == null) {
            return NodeStates.UNDEFINED;
        } else {
            return rst;
        }
    }

    @Override
    public void putState(StatefulFlowContext context, String chainId, String nodeId, int nodeState) {
        String stateKey = chainId + ":" + nodeId;
        getStates(context.getInstanceId()).put(stateKey, nodeState);
    }

    @Override
    public void removeState(StatefulFlowContext context, String chainId, String nodeId) {
        String stateKey = chainId + ":" + nodeId;
        getStates(context.getInstanceId()).remove(stateKey);
    }

    @Override
    public void clearState(StatefulFlowContext context) {
        getStates(context.getInstanceId()).clear();
    }

    @Override
    public List<T> getStateRecords(StatefulFlowContext context) {
        return Collections.unmodifiableList(getHistory(context.getInstanceId()));
    }

    @Override
    public void addStateRecord(StatefulFlowContext context, T record) {
        getHistory(context.getInstanceId()).add(record);
    }
}