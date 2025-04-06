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

import org.noear.redisx.RedisClient;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.NodeState;
import org.noear.solon.flow.stateful.StateRepository;

/**
 * Redis 状态仓库
 *
 * @author noear
 * @since 3.1
 */
public class RedisStateRepository implements StateRepository {
    private final RedisClient client;
    private final String statePrefix;

    public RedisStateRepository(RedisClient client) {
        this(client, "state:");
    }

    public RedisStateRepository(RedisClient client, String statePrefix) {
        this.client = client;
        this.statePrefix = statePrefix;
    }

    @Override
    public int getState(FlowContext context, Node node) {
        String stateKey = node.getChain().getId() + ":" + node.getId();

        Integer rst = client.getHash(statePrefix + context.getInstanceId()).getAsInt(stateKey);
        if (rst == null) {
            return NodeState.UNKNOWN;
        } else {
            return rst;
        }
    }

    @Override
    public void putState(FlowContext context, Node node, int nodeState) {
        String stateKey = node.getChain().getId() + ":" + node.getId();
        client.getHash(statePrefix + context.getInstanceId()).put(stateKey, nodeState);
    }

    @Override
    public void removeState(FlowContext context, Node node) {
        String stateKey = node.getChain().getId() + ":" + node.getId();
        client.getHash(statePrefix + context.getInstanceId()).remove(stateKey);
    }

    @Override
    public void clearState(FlowContext context) {
        client.getHash(statePrefix + context.getInstanceId()).clear();
    }
}