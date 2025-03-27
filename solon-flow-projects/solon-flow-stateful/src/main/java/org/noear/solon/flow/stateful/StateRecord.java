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

import java.io.Serializable;

/**
 * 状态记录
 *
 * @author noear
 * @since 3.1
 */
public class StateRecord implements Serializable {
    private String chainId;
    private String nodeId;
    private int nodeState;
    private long created;

    public StateRecord(String chainId, String nodeId, int nodeState, long created) {
        this.chainId = chainId;
        this.nodeId = nodeId;
        this.nodeState = nodeState;
        this.created = created;
    }

    /**
     * 链Id
     */
    public String getChainId() {
        return chainId;
    }

    /**
     * 节点Id
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 节点状态
     */
    public int getNodeState() {
        return nodeState;
    }

    /**
     * 创建时间戳
     */
    public long getCreated() {
        return created;
    }
}