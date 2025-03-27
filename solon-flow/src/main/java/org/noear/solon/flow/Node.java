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
    private final transient Chain chain;

    private final NodeDecl decl;
    private final List<Link> nextLinks = new ArrayList<>(); //as nextLinks

    private List<Node> prveNodes, nextNodes;
    private List<Link> prveLinks;
    private Condition when;
    private Task task;

    protected Node(Chain chain, NodeDecl decl, List<Link> links) {
        this.chain = chain;
        this.decl = decl;

        if (links != null) {
            this.nextLinks.addAll(links);
            //按优先级排序
            Collections.sort(nextLinks);
        }
    }


    /**
     * 获取所属链
     */
    public Chain getChain() {
        return chain;
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
     * 获取所有元信息
     */
    public Map<String, Object> getMetas() {
        return Collections.unmodifiableMap(decl.meta);
    }

    /**
     * 获取元信息
     */
    public <T> T getMeta(String key) {
        return (T) decl.meta.get(key);
    }

    /**
     * 获取元信息或默认
     */
    public <T> T getMetaOrDefault(String key, T def) {
        return (T) decl.meta.getOrDefault(key, def);
    }

    /**
     * 前面的连接（流入连接）
     */
    public List<Link> getPrveLinks() {
        if (prveLinks == null) {
            List<Link> tmp = new ArrayList<>();

            if (getType() != NodeType.start) {
                for (Link l : chain.getLinks()) {
                    if (getId().equals(l.getNextId())) { //by nextID
                        tmp.add(l);
                    }
                }

                //按优先级排序
                Collections.reverse(tmp);
            }

            prveLinks = Collections.unmodifiableList(tmp);
        }

        return prveLinks;
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
    public List<Node> getPrveNodes() {
        if (prveNodes == null) {
            List<Node> tmp = new ArrayList<>();

            if (getType() != NodeType.start) {
                for (Link l : chain.getLinks()) { //要从链处找
                    if (getId().equals(l.getNextId())) {
                        tmp.add(chain.getNode(l.getPrveId()));
                    }
                }
            }
            prveNodes = Collections.unmodifiableList(tmp);
        }

        return prveNodes;
    }

    /**
     * 后面的节点
     */
    public List<Node> getNextNodes() {
        if (nextNodes == null) {
            List<Node> tmp = new ArrayList<>();

            if (getType() != NodeType.end) {
                for (Link l : this.getNextLinks()) { //从自由处找
                    tmp.add(chain.getNode(l.getNextId()));
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
            when = new Condition(decl.when);
        }

        return when;
    }

    /**
     * 任务
     */
    public Task getTask() {
        if (task == null) {
            task = new Task(this, decl.task);
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