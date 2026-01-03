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
package org.noear.solon.flow;


import org.noear.solon.lang.Preview;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流痕迹（轻量级跟踪）
 *
 * @author noear
 * @since 3.8.1
 */
@Preview("3.8.1")
public class FlowTrace implements Serializable {
    private volatile boolean enabled = true;
    //根图id
    private volatile String rootGraphId;
    //每个图的最后一个节点记录
    private final Map<String, NodeRecord> lastRecords = new ConcurrentHashMap<>();

    public Collection<NodeRecord> lastRecords() {
        return lastRecords.values();
    }

    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 启用（默认为启用）
     */
    public void enable(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 记录
     */
    public void recordNodeId(Graph graph, String nodeId) {
        if (enabled == false) {
            return;
        }

        Objects.requireNonNull(graph, "graph");

        if (nodeId == null) {
            lastRecords.remove(graph.getId());
        } else {
            recordNode(graph, graph.getNodeOrThrow(nodeId));
        }
    }

    /**
     * 记录
     */
    public void recordNode(Graph graph, Node node) {
        if (enabled == false) {
            return;
        }

        Objects.requireNonNull(graph, "graph");

        if (rootGraphId == null) {
            rootGraphId = graph.getId();
        }

        if (node == null) {
            lastRecords.remove(graph.getId());
        } else {
            lastRecords.put(graph.getId(), new NodeRecord(node));
        }
    }

    /**
     * 获取图的最后记录
     */
    public NodeRecord lastRecord(String graphId) {
        if (enabled == false) {
            return null;
        }

        if (graphId == null) {
            graphId = rootGraphId;
        }

        if (graphId == null) {
            return null;
        }

        return lastRecords.get(graphId);
    }

    public Node lastNode(Graph graph) {
        NodeRecord record = lastRecord(graph.getId());

        if (record == null) {
            return graph.getStart();
        }

        return graph.getNodeOrThrow(record.getId());
    }

    public String lastNodeId(String graphId) {
        NodeRecord tmp = lastRecord(graphId);

        if (tmp == null) {
            return null;
        } else {
            return tmp.getId();
        }
    }
}