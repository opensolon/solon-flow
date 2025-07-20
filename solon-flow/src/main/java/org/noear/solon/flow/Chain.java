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

import org.noear.snack.ONode;
import org.noear.solon.Utils;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.lang.Preview;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;
import java.util.*;

/**
 * 链
 *
 * @author noear
 * @since 3.0
 * */
@Preview("3.0")
public class Chain {
    private final String id;
    private final String title;
    private final String driver;
    private final Map<String, Object> meta = new LinkedHashMap<>(); //元数据
    private final Map<String, Node> nodes = new LinkedHashMap<>();

    private final List<Link> links = new ArrayList<>();
    private Node start;

    public Chain(String id) {
        this(id, null, null);
    }

    public Chain(String id, String title) {
        this(id, title, null);
    }

    public Chain(String id, String title, String driver) {
        this.id = id;
        this.title = (title == null ? id : title);
        this.driver = (driver == null ? "" : driver);
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
        return title;
    }

    /**
     * 获取驱动器
     */
    public String getDriver() {
        return driver;
    }

    /**
     * 获取元数据
     */
    public Map<String, Object> getMetas() {
        return this.meta;
    }

    /**
     * 获取元数据
     */
    public Object getMeta(String key) {
        return meta.get(key);
    }

    /**
     * 获取元数据或默认
     */
    public Object getMetaOrDefault(String key, Object def) {
        return meta.getOrDefault(key, def);
    }

    /**
     * 获取起始节点
     */
    public Node getStart() {
        return start;
    }

    /**
     * 获取所有节点
     */
    public Map<String, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * 获取所有连接
     */
    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }


    /**
     * 添加节点
     */
    public void addNode(NodeDecl nodeDecl) {
        List<Link> linkAry = new ArrayList<>();

        for (LinkDecl linkSpec : nodeDecl.links) {
            linkAry.add(new Link(this, nodeDecl.id, linkSpec));
        }

        links.addAll(linkAry);

        Node node = new Node(this, nodeDecl, linkAry);
        nodes.put(node.getId(), node);
        if (nodeDecl.type == NodeType.START) {
            start = node;
        }
    }

    /**
     * 获取节点
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * 校验
     */
    public void check() {
        //如果没有配置 start 节点
        if (start == null) {
            //找到没有流入链接的节点，作为开始节点
            for (Node node : nodes.values()) {
                if (Utils.isEmpty(node.getPrevLinks())) {
                    start = node;
                    break;
                }
            }
        }

        if (start == null) {
            throw new IllegalStateException("No start node found, chain: " + id);
        }
    }

    /// ////////

    /**
     * 解析配置文件
     */
    public static Chain parseByUri(String uri) {
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
    public static Chain parseByText(String text) {
        Object dom = new Yaml().load(text);
        return parseByDom(ONode.load(dom));
    }

    /**
     * 解析配置文档模型
     *
     * @param dom 配置文档模型
     */
    public static Chain parseByDom(ONode dom) {
        String id = dom.get("id").getString();
        String title = dom.get("title").getString();
        String driver = dom.get("driver").getString();

        Chain chain = new Chain(id, title, driver);

        //元数据
        Map metaTmp = dom.get("meta").toObject(Map.class);
        if (Utils.isNotEmpty(metaTmp)) {
            chain.getMetas().putAll(metaTmp);
        }

        //节点（倒序加载，方便自动构建 link）
        List<ONode> layoutTmp = null;
        if (dom.contains("layout")) {
            //新用 layout
            layoutTmp = dom.get("layout").ary();
        } else {
            //弃用 3.1
            layoutTmp = dom.get("nodes").ary();
        }

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
            nodeDecl.meta(n1.get("meta").toObject(Map.class));
            nodeDecl.when(n1.get("when").getString());
            nodeDecl.task(n1.get("task").getString());


            ONode linkNode = n1.get("link");
            if (linkNode.isArray()) {
                //数组模式（多个）
                for (ONode l1 : linkNode.ary()) {
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
            chain.addNode(nodeDecl);
        }

        //校验结构
        chain.check();

        return chain;
    }

    /**
     * 添加连接
     */
    private static void addLink(NodeDecl nodeDecl, ONode l1) {
        //支持 when 简写条件
        String whenStr = l1.contains("when") ? l1.get("when").getString() : l1.get("condition").getString();

        nodeDecl.linkAdd(l1.get("nextId").getString(), ld -> ld
                .title(l1.get("title").getString())
                .meta(l1.get("meta").toObject(Map.class))
                .when(whenStr));
    }

    /**
     * 转为 json
     */
    public String toJson() {
        ONode oNode = new ONode();
        oNode.set("id", id);

        if (Utils.isNotEmpty(title)) {
            oNode.set("title", title);
        }

        if (Utils.isNotEmpty(driver)) {
            oNode.set("driver", driver);
        }

        if (Utils.isNotEmpty(meta)) {
            oNode.getOrNew("meta").fill(meta);
        }

        oNode.getOrNew("layout").asArray().build(n1 -> {
            for (Map.Entry<String, Node> kv : nodes.entrySet()) {
                ONode n2 = n1.addNew();
                n2.set("id", kv.getKey());
                n2.set("type", kv.getValue().getType().toString().toLowerCase());

                if (Utils.isNotEmpty(kv.getValue().getTitle())) {
                    n2.set("title", kv.getValue().getTitle());
                }

                if (Utils.isNotEmpty(kv.getValue().getMetas())) {
                    n2.getOrNew("meta").fill(kv.getValue().getMetas());
                }

                if (Condition.isNotEmpty(kv.getValue().getWhen())) {
                    n2.set("when", kv.getValue().getWhen().getDescription());
                }

                if (Task.isNotEmpty(kv.getValue().getTask())) {
                    n2.set("task", kv.getValue().getTask().getDescription());
                }

                if (Utils.isNotEmpty(kv.getValue().getNextLinks())) {
                    n2.getOrNew("link").asArray().build(n3 -> {
                        for (Link link : kv.getValue().getNextLinks()) {
                            ONode n4 = n3.addNew();
                            n4.set("nextId", link.getNextId());

                            if (Utils.isNotEmpty(link.getTitle())) {
                                n4.set("title", link.getTitle());
                            }

                            if (Utils.isNotEmpty(link.getMetas())) {
                                n4.getOrNew("meta").fill(link.getMetas());
                            }

                            if (Condition.isNotEmpty(link.getWhen())) {
                                n4.set("when", link.getWhen().getDescription());
                            }
                        }
                    });
                }
            }
        });

        return oNode.toJson();
    }
}