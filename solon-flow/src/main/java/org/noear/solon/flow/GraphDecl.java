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
 * 图申明
 *
 * @author noear
 * @since 3.5
 */
@Preview("3.5")
public class GraphDecl {
    protected final String id;
    protected final String title;
    protected final String driver;
    protected final Map<String, Object> meta = new LinkedHashMap<>(); //元数据
    protected final Map<String, NodeDecl> nodes = new LinkedHashMap<>();

    public GraphDecl(String id) {
        this(id, null, null);
    }

    public GraphDecl(String id, String title) {
        this(id, title, null);
    }

    public GraphDecl(String id, String title, String driver) {
        this.id = id;
        this.title = (title == null ? id : title);
        this.driver = (driver == null ? "" : driver);
    }

    public Graph create() {
        return new Graph(this);
    }

    public Graph create(Consumer<GraphDecl> declaration) {
        declaration.accept(this);
        return create();
    }

    public GraphDecl build(Consumer<GraphDecl> declaration) {
        declaration.accept(this);
        return this;
    }

    /**
     * 移除节点
     *
     */
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
    }

    /**
     * 添加节点（或替换）
     */
    public void addNode(NodeDecl nodeDecl) {
        nodes.put(nodeDecl.id, nodeDecl);
    }

    /**
     * 获取节点（一般用于修改）
     */
    public NodeDecl getNode(String id) {
        return nodes.get(id);
    }


    /// ///////////////////////////

    /**
     * 复制
     */
    public static GraphDecl copy(Graph graph) {
        return parseByText(graph.toJson());
    }


    /**
     * 解析配置文件
     */
    public static GraphDecl parseByUri(String uri) {
        URL url = ResourceUtil.findResource(uri, false);
        if (url == null) {
            throw new IllegalArgumentException("Can't find resource: " + uri);
        }

        if (uri.endsWith(".json") || uri.endsWith(".yml") || uri.endsWith(".yaml")) {
            try {
                return parseByText(ResourceUtil.getResourceAsString(url));
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
    public static GraphDecl parseByText(String text) {
        Object dom = new Yaml().load(text);
        return parseByDom(ONode.ofBean(dom));
    }

    /**
     * 解析配置文档模型
     *
     * @param dom 配置文档模型
     */
    public static GraphDecl parseByDom(ONode dom) {
        String id = dom.get("id").getString();
        String title = dom.get("title").getString();
        String driver = dom.get("driver").getString();

        GraphDecl graphDecl = new GraphDecl(id, title, driver);

        //元数据
        Map metaTmp = dom.get("meta").toBean(Map.class);
        if (Utils.isNotEmpty(metaTmp)) {
            graphDecl.meta.putAll(metaTmp);
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

        List<NodeDecl> nodeDeclList = new ArrayList<>();
        NodeDecl nodesLat = null;
        for (int i = layoutTmp.size(); i > 0; i--) {
            ONode n1 = layoutTmp.get(i - 1);

            //自动构建：如果没有时，生成 id
            String n1_id = n1.get("id").getString();
            if (Utils.isEmpty(n1_id)) {
                n1_id = "n-" + i;
            }

            String n1_typeStr = n1.get("type").getString();
            NodeType n1_type = NodeType.nameOf(n1_typeStr);

            NodeDecl nodeDecl = new NodeDecl(n1_id, n1_type);

            nodeDecl.title(n1.get("title").getString());
            nodeDecl.meta(n1.get("meta").toBean(Map.class));
            nodeDecl.when(n1.get("when").getString());
            nodeDecl.task(n1.get("task").getString());


            ONode linkNode = n1.get("link");
            if (linkNode.isArray()) {
                //数组模式（多个）
                for (ONode l1 : linkNode.getArrayUnsafe()) {
                    if (l1.isObject()) {
                        //对象模式
                        addLink(nodeDecl, l1);
                    } else if (l1.isValue()) {
                        //单值模式
                        nodeDecl.linkAdd(l1.getString());
                    }
                }
            } else if (linkNode.isObject()) {
                //对象模式（单个）
                addLink(nodeDecl, linkNode);
            } else if (linkNode.isValue()) {
                //单值模式（单个）
                nodeDecl.linkAdd(linkNode.getString());
            } else if (linkNode.isNull()) {
                //自动构建：如果没有时，生成 link
                if (nodesLat != null) {
                    nodeDecl.linkAdd(nodesLat.id);
                }
            }

            nodesLat = nodeDecl;
            nodeDeclList.add(nodeDecl);
        }

        //倒排加入图
        for (int i = nodeDeclList.size(); i > 0; i--) {
            graphDecl.addNode(nodeDeclList.get(i - 1));
        }


        return graphDecl;
    }

    /**
     * 添加连接
     */
    private static void addLink(NodeDecl nodeDecl, ONode l1) {
        //支持 when 简写条件
        final String whenStr;
        if (l1.hasKey("when")) {
            whenStr = l1.get("when").getString();
        } else {
            //弃用 v3.3
            whenStr = l1.get("condition").getString();
        }

        nodeDecl.linkAdd(l1.get("nextId").getString(), ld -> ld
                .title(l1.get("title").getString())
                .meta(l1.get("meta").toBean(Map.class))
                .when(whenStr));
    }

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

    public Map<String, NodeDecl> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /// /////////////

    /**
     * 构建开始节点
     *
     * @since 3.7
     */
    public NodeDecl addStart(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.START);
        addNode(decl);
        return decl;
    }

    /**
     * 构建结束节点
     *
     * @since 3.7
     */
    public NodeDecl addEnd(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.END);
        addNode(decl);
        return decl;
    }

    /**
     * 构建活动节点
     *
     * @since 3.7
     */
    public NodeDecl addActivity(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.ACTIVITY);
        addNode(decl);
        return decl;
    }

    /**
     * 构建包容网关节点
     *
     * @since 3.7
     */
    public NodeDecl addInclusive(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.INCLUSIVE);
        addNode(decl);
        return decl;
    }

    /**
     * 构建排他网关节点
     *
     * @since 3.7
     */
    public NodeDecl addExclusive(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.EXCLUSIVE);
        addNode(decl);
        return decl;
    }

    /**
     * 构建并行网关节点
     *
     * @since 3.7
     */
    public NodeDecl addParallel(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.PARALLEL);
        addNode(decl);
        return decl;
    }

    /**
     * 构建循环网关节点
     *
     * @since 3.7
     * @deprecated 3.7 {{@link #addLoop(String)}}
     */
    @Deprecated
    public NodeDecl addLooping(String id) {
        return addLoop(id);
    }

    /**
     * 构建循环网关节点
     *
     * @since 3.7
     */
    public NodeDecl addLoop(String id) {
        NodeDecl decl = new NodeDecl(id, NodeType.LOOP);
        addNode(decl);
        return decl;
    }

    /// //////////////////////////////


    /**
     * 转为 yaml
     */
    public String toYaml() {
        return new Yaml().dump(buildDom());
    }

    /**
     * 转为 json
     */
    public String toJson() {
        return ONode.serialize(buildDom());
    }

    protected Map<String, Object> buildDom() {
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

        for (Map.Entry<String, NodeDecl> kv : nodes.entrySet()) {
            NodeDecl node = kv.getValue();

            Map<String, Object> domNode = new LinkedHashMap<>();
            domNodes.add(domNode);

            domNode.put("id", node.id);
            domNode.put("type", node.type.toString().toLowerCase());

            if (Utils.isNotEmpty(node.title)) {
                domNode.put("title", node.title);
            }

            if (Utils.isNotEmpty(node.meta)) {
                domNode.put("meta", node.meta);
            }

            if (Utils.isNotEmpty(node.when)) {
                domNode.put("when", node.when);
            }

            if (Utils.isNotEmpty(node.task)) {
                domNode.put("task", node.task);
            }

            if (Utils.isNotEmpty(node.links)) {
                List<Map<String, Object>> domLinks = new ArrayList<>();
                domNode.put("link", domLinks);

                for (LinkDecl link : node.links) {
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
}