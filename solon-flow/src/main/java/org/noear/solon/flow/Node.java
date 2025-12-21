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

import java.util.*;

/**
 * 节点
 *
 * @author noear
 * @since 3.0
 * */
public class Node {
    public static final String TAG = "node";

    private transient final Graph graph;

    private transient final String id;
    private transient final String title;
    private transient final NodeType type;
    private transient final Map<String, Object> metas;
    private transient final ConditionDesc when;
    private transient final TaskDesc task;

    private transient final List<Link> nextLinks; //as nextLinks

    private transient List<Node> prevNodes, nextNodes;
    private transient List<Link> prevLinks;

    /**
     * 附件（按需定制使用）
     */
    public Object attachment;//如果做扩展解析，用作存储位；

    protected Node(Graph graph, NodeSpec spec, List<Link> links) {
        this.graph = graph;

        this.id = spec.id;
        this.title = spec.title;
        this.type = spec.type;
        this.when = new ConditionDesc(graph, spec.when, spec.whenComponent);
        this.task = new TaskDesc(this, spec.task, spec.taskComponent);

        if (spec.meta == null || spec.meta.size() == 0) {
            this.metas = Collections.emptyMap();
        } else {
            this.metas = Collections.unmodifiableMap(new LinkedHashMap<>(spec.meta));
        }

        if (links == null || links.size() == 0) {
            this.nextLinks = Collections.emptyList();
        } else {
            Collections.sort(links); //按优先级排序
            this.nextLinks = Collections.unmodifiableList(new ArrayList<>(links));
        }
    }

    /**
     * 获取所属图
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * 获取标识
     */
    public String getId() {
        return id;
    }

    /**
     * 获取显示标题
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * 获取类型
     */
    public NodeType getType() {
        return this.type;
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
     * 是否有元数据键
     */
    public boolean hasMeta(String key) {
        return metas.containsKey(key);
    }

    /**
     * 获取元数据并转为 string
     */
    public String getMetaAsString(String key) {
        Object tmp = metas.get(key);
        if (tmp == null) {
            return null;
        } else if (tmp instanceof String) {
            return (String) tmp;
        } else {
            return tmp.toString();
        }
    }

    /**
     * 获取元数据并转为 bool
     */
    public Boolean getMetaAsBool(String key) {
        Object tmp = metas.get(key);
        if (tmp == null) {
            return null;
        } else if (tmp instanceof Boolean) {
            return (Boolean) tmp;
        } else if (tmp instanceof String) {
            return Boolean.parseBoolean((String) tmp);
        } else if (tmp instanceof Number) {
            return ((Number) tmp).doubleValue() > 0;
        } else {
            throw new UnsupportedOperationException(key);
        }
    }

    /**
     * 获取元数据并转为 bool
     */
    public Number getMetaAsNumber(String key) {
        Object tmp = metas.get(key);
        if (tmp == null) {
            return null;
        } else if (tmp instanceof String) {
            return Double.parseDouble((String) tmp);
        } else if (tmp instanceof Number) {
            return (Number) tmp;
        } else {
            throw new UnsupportedOperationException(key);
        }
    }

    /**
     * 获取元数据或默认
     */
    public <T> T getMetaOrDefault(String key, T def) {
        return (T) metas.getOrDefault(key, def);
    }

    /**
     * 前面的连接（流入连接）
     */
    public List<Link> getPrevLinks() {
        if (prevLinks == null) {
            List<Link> tmp = new ArrayList<>();

            if (getType() != NodeType.START) {
                for (Link l : graph.getLinks()) {
                    if (getId().equals(l.getNextId())) { //by nextID
                        tmp.add(l);
                    }
                }

                //按优先级排序
                Collections.reverse(tmp);
            }

            prevLinks = Collections.unmodifiableList(tmp);
        }

        return prevLinks;
    }

    /**
     * 后面的连接（流出连接）
     */
    public List<Link> getNextLinks() {
        return nextLinks;
    }

    /**
     * 前面的节点
     */
    public List<Node> getPrevNodes() {
        if (prevNodes == null) {
            List<Node> tmp = new ArrayList<>();

            if (getType() != NodeType.START) {
                for (Link l : graph.getLinks()) { //要从图处找
                    if (getId().equals(l.getNextId())) {
                        tmp.add(graph.getNode(l.getPrevId()));
                    }
                }
            }
            prevNodes = Collections.unmodifiableList(tmp);
        }

        return prevNodes;
    }

    /**
     * 后面的节点
     */
    public List<Node> getNextNodes() {
        if (nextNodes == null) {
            List<Node> tmp = new ArrayList<>();

            if (getType() != NodeType.END) {
                for (Link l : this.getNextLinks()) { //从自由处找
                    tmp.add(graph.getNode(l.getNextId()));
                }
            }
            nextNodes = Collections.unmodifiableList(tmp);
        }

        return nextNodes;
    }

    /**
     * 后面的节点（一个）
     */
    public Node getNextNode() {
        if (getNextNodes().size() > 0) {
            return getNextNodes().get(0);
        } else {
            return null;
        }
    }

    /**
     * 任务条件
     */
    public ConditionDesc getWhen() {
        return when;
    }

    /**
     * 任务
     */
    public TaskDesc getTask() {
        return task;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("{");
        buf.append("id='").append(id).append('\'');
        buf.append(", type='").append(type).append('\'');

        if (Utils.isNotEmpty(title)) {
            buf.append(", title='").append(title).append('\'');
        }

        if (Utils.isNotEmpty(when.getDescription())) {
            buf.append(", when='").append(when.getDescription()).append('\'');
        }

        if (Utils.isNotEmpty(task.getDescription())) {
            buf.append(", task='").append(task.getDescription()).append('\'');
        }

        if (Utils.isNotEmpty(nextLinks)) {
            buf.append(", link=").append(nextLinks);
        }

        if (Utils.isNotEmpty(metas)) {
            buf.append(", meta=").append(metas);
        }

        buf.append("}");

        return buf.toString();
    }
}