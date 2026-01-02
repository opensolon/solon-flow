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

import org.noear.snack4.Feature;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;

/**
 * 节点痕迹
 *
 * @author noear
 * @since 3.8.1
 */
public class NodeTrace implements Serializable {
    private String graphId;
    private String id;
    private String title;
    @ONodeAttr(features = Feature.Write_EnumUsingName)
    private NodeType type;
    private long timestamp;

    public NodeTrace() {
        //用于反序列化
    }

    public NodeTrace(Node node) {
        this.graphId = node.getGraph().getId();
        this.id = node.getId();
        this.title = node.getTitle();
        this.type = node.getType();
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isEnd() {
        return type == NodeType.END;
    }

    public boolean isNotEnd() {
        return type == NodeType.END;
    }

    public String getGraphId() {
        return graphId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public NodeType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}