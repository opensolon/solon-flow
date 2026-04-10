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

/**
 * PlantUML 显示映射上下文
 *
 * @author noear
 * @since 3.10
 */
public class PlantumlDisplayContext {
    private final Node node;
    private final Link link;

    protected PlantumlDisplayContext(Node node, Link link) {
        this.node = node;
        this.link = link;
    }

    /**
     * 创建节点显示上下文
     */
    public static PlantumlDisplayContext ofNode(Node node) {
        return new PlantumlDisplayContext(node, null);
    }

    /**
     * 创建连接显示上下文
     */
    public static PlantumlDisplayContext ofLink(Link link) {
        return new PlantumlDisplayContext(null, link);
    }

    /**
     * 是否为节点上下文
     */
    public boolean isNode() {
        return node != null;
    }

    /**
     * 是否为连接上下文
     */
    public boolean isLink() {
        return link != null;
    }

    /**
     * 获取节点
     */
    public Node getNode() {
        return node;
    }

    /**
     * 获取连接
     */
    public Link getLink() {
        return link;
    }

    /**
     * 获取节点 ID
     */
    public String getId() {
        if (node != null) {
            return node.getId();
        }
        return null;
    }

    /**
     * 获取节点标题
     */
    public String getTitle() {
        if (node != null) {
            return node.getTitle();
        }
        if (link != null) {
            return link.getTitle();
        }
        return null;
    }

    /**
     * 获取任务描述（仅节点上下文可用）
     */
    public String getTask() {
        if (node != null) {
            return node.getTask().getDescription();
        }
        return null;
    }

    /**
     * 获取条件描述（仅连接上下文可用）
     */
    public String getWhen() {
        if (link != null) {
            return link.getWhen().getDescription();
        }
        return null;
    }
}
