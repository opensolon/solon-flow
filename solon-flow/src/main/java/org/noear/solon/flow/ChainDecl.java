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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 链申明
 *
 * @author noear
 * @since 3.5
 */
@Preview("3.5")
public class ChainDecl {
    protected final String id;
    protected final String title;
    protected final String driver;
    protected final Map<String, Object> meta = new LinkedHashMap<>(); //元数据
    protected final Map<String, NodeDecl> nodes = new LinkedHashMap<>();

    public ChainDecl(String id) {
        this(id, null, null);
    }

    public ChainDecl(String id, String title) {
        this(id, title, null);
    }

    public ChainDecl(String id, String title, String driver) {
        this.id = id;
        this.title = (title == null ? id : title);
        this.driver = (driver == null ? "" : driver);
    }

    public Chain create() {
        return new Chain(this);
    }

    public Chain create(Consumer<ChainDecl> declaration) {
        declaration.accept(this);
        return create();
    }

    /**
     * 移除节点
     *
     */
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
    }

    /**
     * 添加节点
     */
    public void addNode(NodeDecl nodeDecl) {
        nodes.put(nodeDecl.id, nodeDecl);
    }


    /// ///////////////////////////

    /**
     * 解析配置文件
     */
    public static ChainDecl parseByUri(String uri) {
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
    public static ChainDecl parseByText(String text) {
        Object dom = new Yaml().load(text);
        return parseByDom(ONode.load(dom));
    }

    /**
     * 解析配置文档模型
     *
     * @param dom 配置文档模型
     */
    public static ChainDecl parseByDom(ONode dom) {
        String id = dom.get("id").getString();
        String title = dom.get("title").getString();
        String driver = dom.get("driver").getString();

        ChainDecl chainDecl = new ChainDecl(id, title, driver);

        //元数据
        Map metaTmp = dom.get("meta").toObject(Map.class);
        if (Utils.isNotEmpty(metaTmp)) {
            chainDecl.meta.putAll(metaTmp);
        }

        //节点（倒序加载，方便自动构建 link）
        final List<ONode> layoutTmp;
        if (dom.contains("layout")) {
            //新用 layout
            layoutTmp = dom.get("layout").ary();
        } else {
            //弃用 v3.1
            layoutTmp = dom.get("nodes").ary();
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
            nodeDeclList.add(nodeDecl);
        }

        //倒排加入链
        for (int i = nodeDeclList.size(); i > 0; i--) {
            chainDecl.addNode(nodeDeclList.get(i - 1));
        }


        return chainDecl;
    }

    /**
     * 添加连接
     */
    private static void addLink(NodeDecl nodeDecl, ONode l1) {
        //支持 when 简写条件
        final String whenStr;
        if (l1.contains("when")) {
            whenStr = l1.get("when").getString();
        } else {
            //弃用 v3.3
            whenStr = l1.get("condition").getString();
        }

        nodeDecl.linkAdd(l1.get("nextId").getString(), ld -> ld
                .title(l1.get("title").getString())
                .meta(l1.get("meta").toObject(Map.class))
                .when(whenStr));
    }

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
        return ONode.stringify(buildDom());
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

                    domLink.put("nextId", link.nextId);

                    if (Utils.isNotEmpty(link.title)) {
                        domLink.put("title", link.title);
                    }

                    if (Utils.isNotEmpty(link.meta)) {
                        domLink.put("meta", link.meta);
                    }

                    if (Utils.isNotEmpty(link.when)) {
                        domLink.put("when", link.when);
                    }
                }

            }
        }

        return domRoot;
    }
}
