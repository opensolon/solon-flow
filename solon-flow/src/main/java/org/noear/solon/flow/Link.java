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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连接
 *
 * @author noear
 * @since 3.0
 */
public class Link implements Comparable<Link> {
    private transient final Graph graph;

    private transient final String nextId;
    private transient final String title;
    private transient final Map<String, Object> metas;
    private transient final int priority; //优先级（越大越高）

    private transient final String prevId;
    private transient final ConditionDesc when;
    private transient Node prevNode, nextNode;

    public Link(Graph graph, String prevId, LinkSpec decl) {
        this.graph = graph;
        this.prevId = prevId;

        this.nextId = decl.getNextId();
        this.title = decl.getTitle();
        this.priority = decl.getPriority();
        this.when = new ConditionDesc(graph, decl.getWhen(), decl.getWhenComponent());

        if (decl.getMeta() == null) {
            this.metas = Collections.emptyMap();
        } else {
            this.metas = Collections.unmodifiableMap(new LinkedHashMap<>(decl.getMeta()));
        }
    }

    /**
     * 获取所属图
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * 获取标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取所有元数据
     */
    public Map<String, Object> getMetas() {
        return metas;
    }

    /**
     * 获取元数据
     */
    public Object getMeta(String key) {
        return metas.get(key);
    }

    /**
     * 获取元数据
     *
     * @since 3.8
     */
    public <T> T getMetaAs(String key) {
        return (T) metas.get(key);
    }

    /**
     * 获取元数据或默认
     */
    public Object getMetaOrDefault(String key, Object def) {
        return metas.getOrDefault(key, def);
    }

    /**
     * 分支流出条件
     */
    public ConditionDesc getWhen() {
        return when;
    }

    /**
     * 分支流出条件
     *
     * @deprecated 3.3 {@link #getWhen()}
     */
    @Deprecated
    public ConditionDesc getCondition() {
        return getWhen();
    }

    /**
     * 前面的节点Id
     */
    public String getPrevId() {
        return prevId;
    }

    /**
     * 后面的节点Id
     */
    public String getNextId() {
        return nextId;
    }

    /**
     * 前面的节点
     */
    public Node getPrevNode() {
        if (prevNode == null) {
            prevNode = graph.getNode(getPrevId()); //by id query
        }

        return prevNode;
    }

    /**
     * 后面的节点
     */
    public Node getNextNode() {
        if (nextNode == null) {
            nextNode = graph.getNode(getNextId()); //by id query
        }

        return nextNode;
    }

    @Override
    public int compareTo(Link o) {
        if (this.priority > o.priority) {
            return -1; //大的在前
        } else if (this.priority < o.priority) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("{");
        buf.append("priority=").append(priority);
        buf.append(", prevId='").append(getPrevId()).append('\'');
        buf.append(", nextId='").append(getNextId()).append('\'');

        if (Utils.isNotEmpty(title)) {
            buf.append(", title='").append(title).append('\'');
        }

        if (Utils.isNotEmpty(metas)) {
            buf.append(", meta=").append(metas);
        }

        if (Utils.isNotEmpty(when.getDescription())) {
            buf.append(", when=").append(when.getDescription());
        }

        buf.append("}");

        return buf.toString();
    }
}