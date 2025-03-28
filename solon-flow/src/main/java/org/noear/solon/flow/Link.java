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

import org.noear.solon.Utils;

import java.util.Map;

/**
 * 连接
 *
 * @author noear
 * @since 3.0
 */
public class Link implements Comparable<Link> {
    private final Chain chain;
    private final String prveId;
    private final LinkDecl decl;

    private Node prveNode, nextNode;
    private Condition condition;

    public Link(Chain chain, String prveId, LinkDecl decl) {
        this.chain = chain;
        this.prveId = prveId;
        this.decl = decl;
    }

    /**
     * 获取所属链
     */
    public Chain getChain() {
        return chain;
    }

    /**
     * 获取所有元信息
     */
    public Map<String, Object> getMetas() {
        return decl.meta;
    }

    /**
     * 获取元信息
     */
    public Object getMeta(String key) {
        return decl.meta.get(key);
    }

    /**
     * 获取元信息或默认
     */
    public Object getMetaOrDefault(String key, Object def) {
        return decl.meta.getOrDefault(key, def);
    }

    /**
     * 条件
     */
    public Condition getCondition() {
        if (condition == null) {
            condition = new Condition(chain, decl.condition);
        }

        return condition;
    }

    /**
     * 前面的节点Id
     */
    public String getPrveId() {
        return prveId;
    }

    /**
     * 后面的节点Id
     */
    public String getNextId() {
        return decl.nextId;
    }

    /**
     * 前面的节点
     */
    public Node getPrveNode() {
        if (prveNode == null) {
            prveNode = chain.getNode(getPrveId()); //by id query
        }

        return prveNode;
    }

    /**
     * 后面的节点
     */
    public Node getNextNode() {
        if (nextNode == null) {
            nextNode = chain.getNode(getNextId()); //by id query
        }

        return nextNode;
    }

    @Override
    public int compareTo(Link o) {
        if (this.decl.priority > o.decl.priority) {
            return -1; //大的在前
        } else if (this.decl.priority < o.decl.priority) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("{");
        buf.append("priority=").append(decl.priority);
        buf.append(", prveId='").append(getPrveId()).append('\'');
        buf.append(", nextId='").append(getNextId()).append('\'');

        if (Utils.isNotEmpty(decl.title)) {
            buf.append(", title='").append(decl.title).append('\'');
        }

        if (Utils.isNotEmpty(decl.meta)) {
            buf.append(", meta=").append(decl.meta);
        }

        if (Utils.isNotEmpty(decl.condition)) {
            buf.append(", condition=").append(decl.condition);
        }

        buf.append("}");

        return buf.toString();
    }
}