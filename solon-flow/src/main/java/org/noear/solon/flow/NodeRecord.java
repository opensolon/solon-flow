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
import org.noear.solon.lang.Preview;

import java.io.Serializable;

/**
 * 节点记录
 *
 * @author noear
 * @since 3.8.1
 */
@Preview("3.8")
public class NodeRecord implements Serializable {
    private String graphId;
    private String id;
    private String title;
    @ONodeAttr(features = Feature.Write_EnumUsingName)
    private NodeType type;
    private long timestamp;

    public NodeRecord() {
        //用于反序列化
    }

    public NodeRecord(Node node) {
        this.graphId = node.getGraph().getId();
        this.id = node.getId();
        this.title = node.getTitle();
        this.type = node.getType();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 是否为 end 类型节点
     */
    public boolean isEnd() {
        return NodeType.END == type;
    }

    /**
     * 获取图id
     */
    public String getGraphId() {
        return graphId;
    }

    /**
     * 获取节点Id
     */
    public String getId() {
        return id;
    }

    /**
     * 获取节点标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取节点类型
     */
    public NodeType getType() {
        return type;
    }

    /**
     * 获取记录时间
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "NodeRecord{" +
                "graphId='" + graphId + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                '}';
    }
}