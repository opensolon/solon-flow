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
import java.util.function.Consumer;

/**
 * 节点定义
 *
 * @author noear
 * @since 3.0
 */
public class NodeSpec {
    protected final String id;
    protected String title;
    protected NodeType type;      //元素类型
    protected final Map<String, Object> meta = new LinkedHashMap<>(); //元数据
    protected List<LinkSpec> links = new ArrayList<>();
    protected String when;
    protected ConditionComponent whenComponent;
    protected String task;
    protected TaskComponent taskComponent;


    /// //////////////

    /**
     * @param id   标识
     * @param type 类型
     */
    public NodeSpec(String id, NodeType type) {
        this.id = id;
        this.type = type;
    }

    /**
     * 配置标题
     */
    public NodeSpec title(String title) {
        this.title = title;
        return this;
    }

    /**
     * 配置元数据
     */
    public NodeSpec meta(Map<String, Object> map) {
        if (Utils.isNotEmpty(map)) {
            this.meta.putAll(map);
        }

        return this;
    }

    /**
     * 配置元数据
     */
    public NodeSpec metaPut(String key, Object value) {
        if (Utils.isNotEmpty(key)) {
            this.meta.put(key, value);
        }

        return this;
    }

    /**
     * 配置连接
     *
     * @param nextId 下个节点Id
     */
    public NodeSpec linkAdd(String nextId, Consumer<LinkSpec> configure) {
        LinkSpec linkDecl = new LinkSpec(nextId);
        if (configure != null) {
            configure.accept(linkDecl);
        }
        this.links.add(linkDecl);
        return this;
    }

    /**
     * 配置连接
     *
     * @param nextId 下个节点Id
     */
    public NodeSpec linkAdd(String nextId) {
        return linkAdd(nextId, null);
    }

    public NodeSpec linkRemove(String nextId) {
        this.links.removeIf(l->l.getNextId().equals(nextId));
        return this;
    }

    /**
     * 配置任务条件
     */
    public NodeSpec when(String when) {
        this.when = when;
        return this;
    }

    /**
     * 配置任务条件（用于硬编码）
     *
     * @since  3.7
     */
    public NodeSpec when(ConditionComponent whenComponent) {
        this.whenComponent = whenComponent;
        return this;
    }

    /**
     * 配置任务描述（适合配置）
     */
    public NodeSpec task(String task) {
        this.task = task;
        return this;
    }

    /**
     * 配置任务组件（适合硬编码）
     *
     * @since 3.7
     */
    public NodeSpec task(TaskComponent taskComponent) {
        this.taskComponent = taskComponent;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer("{");
        buf.append("id='").append(id).append('\'');

        if (Utils.isNotEmpty(title)) {
            buf.append(", title='").append(title).append('\'');
        }

        if (type != null) {
            buf.append(", type=").append(type);
        }

        if (Utils.isNotEmpty(meta)) {
            buf.append(", meta=").append(meta);
        }

        if (Utils.isNotEmpty(links)) {
            buf.append(", links=").append(links);
        }

        if (Utils.isNotEmpty(when)) {
            buf.append(", when='").append(when).append('\'');
        }

        if (whenComponent != null) {
            buf.append(", whenComponent=").append(whenComponent);
        }

        if (Utils.isNotEmpty(task)) {
            buf.append(", task='").append(task).append('\'');
        }

        if (taskComponent != null) {
            buf.append(", taskComponent=").append(taskComponent);
        }

        buf.append('}');
        return buf.toString();
    }

    /// ///////////////

    /**
     * 构建开始节点
     */
    public static NodeSpec startOf(String id) {
        return new NodeSpec(id, NodeType.START);
    }

    /**
     * 构建结束节点
     */
    public static NodeSpec endOf(String id) {
        return new NodeSpec(id, NodeType.END);
    }

    /**
     * 构建活动节点
     */
    public static NodeSpec activityOf(String id) {
        return new NodeSpec(id, NodeType.ACTIVITY);
    }

    /**
     * 构建包容网关节点
     */
    public static NodeSpec inclusiveOf(String id) {
        return new NodeSpec(id, NodeType.INCLUSIVE);
    }

    /**
     * 构建排他网关节点
     */
    public static NodeSpec exclusiveOf(String id) {
        return new NodeSpec(id, NodeType.EXCLUSIVE);
    }

    /**
     * 构建并行网关节点
     */
    public static NodeSpec parallelOf(String id) {
        return new NodeSpec(id, NodeType.PARALLEL);
    }

    /**
     * 构建循环网关节点
     */
    public static NodeSpec loopingOf(String id) {
        return new NodeSpec(id, NodeType.LOOP);
    }

    /**
     * 构建循环网关节点
     * 
     * @deprecated 3.6 {@link #loopingOf(String)}
     */
    @Deprecated
    public static NodeSpec iteratorOf(String id) {
        return loopingOf(id);
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

    public Map<String, Object> getMeta() {
        return meta;
    }

    public List<LinkSpec> getLinks() {
        return links;
    }

    public String getWhen() {
        return when;
    }

    public ConditionComponent getWhenComponent() {
        return whenComponent;
    }

    public String getTask() {
        return task;
    }

    public TaskComponent getTaskComponent() {
        return taskComponent;
    }
}