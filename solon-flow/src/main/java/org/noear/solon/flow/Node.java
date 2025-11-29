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
import org.noear.solon.lang.Preview;

import java.util.*;

/**
 * 节点
 *
 * @author noear
 * @since 3.0
 * */
public class Node {
    public static final String TAG = "node";

    private final transient Graph graph;

    private final NodeDecl decl;
    private final List<Link> nextLinks = new ArrayList<>(); //as nextLinks

    private List<Node> prevNodes, nextNodes;
    private List<Link> prevLinks;
    private Condition when;
    private Task task;
    private NodeType imode = NodeType.UNKNOWN;
    private NodeType omode = NodeType.UNKNOWN;

    /**
     * 附件（按需定制使用）
     */
    public Object attachment;//如果做扩展解析，用作存储位；

    protected Node(Graph graph, NodeDecl decl, List<Link> links) {
        this.graph = graph;
        this.decl = decl;

        if (links != null) {
            this.nextLinks.addAll(links);
            //按优先级排序
            Collections.sort(nextLinks);
        }

        ioModeInit();
    }

    private void ioModeInit() {
        //流入
        String i_mode = getMetaAsString("$imode");
        this.imode = NodeType.nameOf(i_mode, NodeType.UNKNOWN);

        //流出
        String o_mode = getMetaAsString("$omode");
        this.omode = NodeType.nameOf(o_mode, NodeType.UNKNOWN);
    }

    /**
     * 流入模式
     *
     * @since 3.4
     */
    @Preview("3.4")
    protected NodeType getImode() {
        return imode;
    }

    /**
     * 流出模式
     *
     * @since 3.4
     */
    @Preview("3.4")
    protected NodeType getOmode() {
        return omode;
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
        return decl.id;
    }

    /**
     * 获取显示标题
     */
    public String getTitle() {
        return decl.title;
    }

    /**
     * 获取类型
     */
    public NodeType getType() {
        return decl.type;
    }

    /**
     * 获取所有元数据
     */
    public Map<String, Object> getMetas() {
        return Collections.unmodifiableMap(decl.meta);
    }

    /**
     * 获取元数据
     */
    public <T> T getMeta(String key) {
        return (T) decl.meta.get(key);
    }

    /**
     * 是否有元数据键
     */
    public boolean hasMeta(String key) {
        return decl.meta.containsKey(key);
    }

    /**
     * 获取元数据并转为 string
     */
    public String getMetaAsString(String key) {
        Object tmp = decl.meta.get(key);
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
        Object tmp = decl.meta.get(key);
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
        Object tmp = decl.meta.get(key);
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
        return (T) decl.meta.getOrDefault(key, def);
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
        return Collections.unmodifiableList(nextLinks);
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
    public Condition getWhen() {
        if (when == null) {
            when = new Condition(graph, decl.when);
        }

        return when;
    }

    /**
     * 任务
     */
    public Task getTask() {
        if (task == null) {
            task = new Task(this, decl.task, decl.taskComponent);
        }

        return task;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("{");
        buf.append("id='").append(decl.id).append('\'');
        buf.append(", type='").append(decl.type).append('\'');

        if (Utils.isNotEmpty(decl.title)) {
            buf.append(", title='").append(decl.title).append('\'');
        }

        if (Utils.isNotEmpty(decl.when)) {
            buf.append(", when='").append(decl.when).append('\'');
        }

        if (Utils.isNotEmpty(decl.task)) {
            buf.append(", task='").append(decl.task).append('\'');
        }

        if (Utils.isNotEmpty(decl.links)) {
            buf.append(", link=").append(decl.links);
        }

        if (Utils.isNotEmpty(decl.meta)) {
            buf.append(", meta=").append(decl.meta);
        }

        buf.append("}");

        return buf.toString();
    }
}