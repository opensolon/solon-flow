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

import org.noear.snack4.ONode;
import org.noear.solon.Utils;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.lang.Preview;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * 图定义
 *
 * @author noear
 * @since 3.5
 */
@Preview("3.5")
public class GraphSpec {
    private final String id;
    private final String title;
    private final String driver;
    private final Map<String, Object> meta = new LinkedHashMap<>(); //元数据
    private final Map<String, NodeSpec> nodes = new LinkedHashMap<>();

    public GraphSpec(String id) {
        this(id, null, null);
    }

    public GraphSpec(String id, String title) {
        this(id, title, null);
    }

    public GraphSpec(String id, String title, String driver) {
        this.id = id;
        this.title = (title == null ? id : title);
        this.driver = (driver == null ? "" : driver);
    }

    public GraphSpec then(Consumer<GraphSpec> definition) {
        definition.accept(this);
        return this;
    }

    public Graph create() {
        return new Graph(this);
    }

    /**
     * 移除节点
     *
     */
    public NodeSpec removeNode(String nodeId) {
        return nodes.remove(nodeId);
    }

    /**
     * 添加节点（或替换）
     *
     * @return added
     */
    public NodeSpec addNode(NodeSpec nodeSpec) {
        nodes.put(nodeSpec.getId(), nodeSpec);
        return nodeSpec;
    }

    /**
     * 获取节点（一般用于修改）
     */
    public NodeSpec getNode(String id) {
        return nodes.get(id);
    }


    /// ///////////////////////////


    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDriver() {
        return driver;
    }

    public Map<String, Object> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    public Map<String, NodeSpec> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /// /////////////

    public GraphSpec metaPut(String key, Object value) {
        meta.put(key, value);
        return this;
    }

    /**
     * 构建开始节点
     *
     * @since 3.7
     */
    public NodeSpec addStart(String id) {
        return addNode(new NodeSpec(id, NodeType.START));
    }

    /**
     * 构建结束节点
     *
     * @since 3.7
     */
    public NodeSpec addEnd(String id) {
        return addNode(new NodeSpec(id, NodeType.END));
    }

    /**
     * 构建活动节点
     *
     * @since 3.7
     */
    public NodeSpec addActivity(String id) {
        return addNode(new NodeSpec(id, NodeType.ACTIVITY));
    }

    /**
     * 构建活动节点
     *
     * @since 3.8.1
     */
    public NodeSpec addActivity(NamedTaskComponent com) {
        Objects.requireNonNull(com.name(), "name");

        return addNode(new NodeSpec(com.name(), NodeType.ACTIVITY)).task(com).title(com.title());
    }

    /**
     * 构建包容网关节点
     *
     * @since 3.7
     */
    public NodeSpec addInclusive(String id) {
        return addNode(new NodeSpec(id, NodeType.INCLUSIVE));
    }

    /**
     * 构建包容网关节点
     *
     * @since 3.8.1
     */
    public NodeSpec addInclusive(NamedTaskComponent com) {
        Objects.requireNonNull(com.name(), "name");

        return addNode(new NodeSpec(com.name(), NodeType.INCLUSIVE)).task(com).title(com.title());
    }

    /**
     * 构建排他网关节点（最多只能有一个默认的无条件分支）
     *
     * @since 3.7
     */
    public NodeSpec addExclusive(String id) {
        return addNode(new NodeSpec(id, NodeType.EXCLUSIVE));
    }

    /**
     * 构建排他网关节点（最多只能有一个默认的无条件分支）
     *
     * @since 3.8.1
     */
    public NodeSpec addExclusive(NamedTaskComponent com) {
        Objects.requireNonNull(com.name(), "name");

        return addNode(new NodeSpec(com.name(), NodeType.EXCLUSIVE)).task(com).title(com.title());
    }

    /**
     * 构建并行网关节点
     *
     * @since 3.7
     */
    public NodeSpec addParallel(String id) {
        return addNode(new NodeSpec(id, NodeType.PARALLEL));
    }

    /**
     * 构建并行网关节点
     *
     * @since 3.8.1
     */
    public NodeSpec addParallel(NamedTaskComponent com) {
        Objects.requireNonNull(com.name(), "name");

        return addNode(new NodeSpec(com.name(), NodeType.PARALLEL)).task(com).title(com.title());
    }

    /**
     * 构建循环网关节点
     *
     * @since 3.7
     */
    public NodeSpec addLoop(String id) {
        return addNode(new NodeSpec(id, NodeType.LOOP));
    }

    /**
     * 构建循环网关节点
     *
     * @since 3.8.1
     */
    public NodeSpec addLoop(NamedTaskComponent com) {
        Objects.requireNonNull(com.name(), "name");

        return addNode(new NodeSpec(com.name(), NodeType.LOOP)).task(com).title(com.title());
    }

    /// //////////////////////////////


    /**
     * 转为 yaml
     */
    public String toYaml() {
        return new Yaml().dump(toMap());
    }

    /**
     * 转为 json
     */
    public String toJson() {
        return ONode.serialize(toMap());
    }

    /**
     * 转为 map
     *
     * @since 3.8
     */
    public Map<String, Object> toMap() {
        Map<String, Object> domRoot = new LinkedHashMap<>();
        domRoot.put("id", id);

        if (Utils.isNotEmpty(title)) {
            domRoot.put("title", title);
        }

        if (Utils.isNotEmpty(driver)) {
            domRoot.put("driver", driver);
        }

        if (Utils.isNotEmpty(meta)) {
            domRoot.put("meta", meta);
        }

        List<Map<String, Object>> domNodes = new ArrayList<>();
        domRoot.put("layout", domNodes);

        for (Map.Entry<String, NodeSpec> kv : nodes.entrySet()) {
            NodeSpec node = kv.getValue();

            Map<String, Object> domNode = new LinkedHashMap<>();
            domNodes.add(domNode);

            domNode.put("id", node.getId());
            domNode.put("type", node.getType().toString().toLowerCase());

            if (Utils.isNotEmpty(node.getTitle())) {
                domNode.put("title", node.getTitle());
            }

            if (Utils.isNotEmpty(node.getMeta())) {
                domNode.put("meta", node.getMeta());
            }

            if (Utils.isNotEmpty(node.getWhen())) {
                domNode.put("when", node.getWhen());
            }

            if (Utils.isNotEmpty(node.getTask())) {
                domNode.put("task", node.getTask());
            }

            if (Utils.isNotEmpty(node.getLinks())) {
                List<Map<String, Object>> domLinks = new ArrayList<>();
                domNode.put("link", domLinks);

                for (LinkSpec link : node.getLinks()) {
                    Map<String, Object> domLink = new LinkedHashMap<>();
                    domLinks.add(domLink);

                    domLink.put("nextId", link.getNextId());

                    if (Utils.isNotEmpty(link.getTitle())) {
                        domLink.put("title", link.getTitle());
                    }

                    if (Utils.isNotEmpty(link.getMeta())) {
                        domLink.put("meta", link.getMeta());
                    }

                    if (Utils.isNotEmpty(link.getWhen())) {
                        domLink.put("when", link.getWhen());
                    }
                }

            }
        }

        return domRoot;
    }

    /// ///////////

    /**
     * 复制
     */
    public static GraphSpec copy(Graph graph) {
        return fromText(graph.toJson());
    }


    /**
     * 解析配置文件
     */
    public static GraphSpec fromUri(String uri) {
        URL url = ResourceUtil.findResource(uri, false);
        if (url == null) {
            throw new IllegalArgumentException("Can't find resource: " + uri);
        }

        if (uri.endsWith(".json") || uri.endsWith(".yml") || uri.endsWith(".yaml")) {
            try {
                return fromText(ResourceUtil.getResourceAsString(url));
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Failed to load resource: " + url, ex);
            }
        } else {
            throw new IllegalArgumentException("File format is not supported: " + uri);
        }
    }

    /**
     * 解析配置文本
     *
     * @param text 配置文本（支持 yml, json 格式）
     */
    public static GraphSpec fromText(String text) {
        Object dom = new Yaml().load(text);
        return fromDom(ONode.ofBean(dom));
    }

    /**
     * 解析配置文档模型
     *
     * @param dom 配置文档模型
     */
    public static GraphSpec fromDom(ONode dom) {
        String id = dom.get("id").getString();
        String title = dom.get("title").getString();
        String driver = dom.get("driver").getString();

        GraphSpec spec = new GraphSpec(id, title, driver);

        //元数据
        Map metaTmp = dom.get("meta").toBean(Map.class);
        if (Utils.isNotEmpty(metaTmp)) {
            spec.meta.putAll(metaTmp);
        }

        //节点（倒序加载，方便自动构建 link）
        final List<ONode> layoutTmp;
        if (dom.hasKey("layout")) {
            //新用 layout
            layoutTmp = dom.get("layout").getArray();
        } else {
            //弃用 v3.1
            layoutTmp = dom.get("nodes").getArray();
        }

        List<NodeSpec> nodeSpecList = new ArrayList<>();
        NodeSpec nodesLat = null;
        for (int i = layoutTmp.size(); i > 0; i--) {
            ONode n1 = layoutTmp.get(i - 1);

            //自动构建：如果没有时，生成 id
            String n1_id = n1.get("id").getString();
            if (Utils.isEmpty(n1_id)) {
                n1_id = "n-" + i;
            }

            String n1_typeStr = n1.get("type").getString();
            NodeType n1_type = NodeType.nameOf(n1_typeStr);

            NodeSpec nodeSpec = new NodeSpec(n1_id, n1_type);

            nodeSpec.title(n1.get("title").getString());
            nodeSpec.meta(n1.get("meta").toBean(Map.class));
            nodeSpec.when(n1.get("when").getString());
            nodeSpec.task(n1.get("task").getString());


            ONode linkNode = n1.get("link");
            if (linkNode.isArray()) {
                //数组模式（多个）
                for (ONode l1 : linkNode.getArrayUnsafe()) {
                    if (l1.isObject()) {
                        //对象模式
                        addLink(nodeSpec, l1);
                    } else if (l1.isValue()) {
                        //单值模式
                        nodeSpec.linkAdd(l1.getString());
                    }
                }
            } else if (linkNode.isObject()) {
                //对象模式（单个）
                addLink(nodeSpec, linkNode);
            } else if (linkNode.isValue()) {
                //单值模式（单个）
                nodeSpec.linkAdd(linkNode.getString());
            } else if (linkNode.isNull()) {
                //自动构建：如果没有时，生成 link
                if (nodesLat != null) {
                    nodeSpec.linkAdd(nodesLat.getId());
                }
            }

            nodesLat = nodeSpec;
            nodeSpecList.add(nodeSpec);
        }

        //倒排加入图
        for (int i = nodeSpecList.size(); i > 0; i--) {
            spec.addNode(nodeSpecList.get(i - 1));
        }


        return spec;
    }

    /**
     * 添加连接
     */
    private static void addLink(NodeSpec nodeSpec, ONode l1) {
        //支持 when 简写条件
        final String whenStr;
        if (l1.hasKey("when")) {
            whenStr = l1.get("when").getString();
        } else {
            //弃用 v3.3
            whenStr = l1.get("condition").getString();
        }

        nodeSpec.linkAdd(l1.get("nextId").getString(), ld -> ld
                .title(l1.get("title").getString())
                .meta(l1.get("meta").toBean(Map.class))
                .when(whenStr));
    }
}