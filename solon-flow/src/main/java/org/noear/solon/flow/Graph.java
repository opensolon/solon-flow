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
import java.util.function.Consumer;

/**
 * 图
 *
 * @author noear
 * @since 3.0
 * @since 3.7
 * */
@Preview("3.0")
public class Graph {
    private final GraphDecl decl;
    private Map<String, Node> nodes = new LinkedHashMap<>();
    private List<Link> links = new ArrayList<>();
    private Node start;

    protected Graph(GraphDecl decl) {
        this.decl = decl;


        //倒排加入图
        for (Map.Entry<String, NodeDecl> kv : decl.nodes.entrySet()) {
            this.addNode(kv.getValue());
        }

        //校验结构
        this.check();
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
     * 获取驱动器
     */
    public String getDriver() {
        return decl.driver;
    }

    /**
     * 获取元数据
     */
    public Map<String, Object> getMetas() {
        return decl.meta;
    }

    /**
     * 获取元数据
     */
    public Object getMeta(String key) {
        return decl.meta.get(key);
    }

    /**
     * 获取元数据或默认
     */
    public Object getMetaOrDefault(String key, Object def) {
        return decl.meta.getOrDefault(key, def);
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
     * 获取节点
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /// ////////

    /**
     * 添加节点
     */
    private void addNode(NodeDecl nodeDecl) {
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
     * 校验
     */
    private void check() {
        //如果没有配置 start 节点
        if (start == null) {
            //找到没有流入连接的节点，作为开始节点
            for (Node node : nodes.values()) {
                if (Utils.isEmpty(node.getPrevLinks())) {
                    start = node;
                    break;
                }
            }
        }

        if (start == null) {
            throw new IllegalStateException("No start node found, graph: " + decl);
        }
    }

    /// ////////

    /**
     * 创建
     *
     * @since 3.7
     */
    public static Graph create(String id, Consumer<GraphDecl> consumer) {
        GraphDecl decl = new GraphDecl(id);
        consumer.accept(decl);
        return decl.create();
    }

    /**
     * 解析配置文件
     */
    public static Graph parseByUri(String uri) {
        return GraphDecl.parseByUri(uri).create();
    }

    /**
     * 解析配置文本
     *
     * @param text 配置文本（支持 yml, json 格式）
     */
    public static Graph parseByText(String text) {
        return GraphDecl.parseByText(text).create();
    }

    /**
     * 转为 yaml
     */
    public String toYaml() {
        return decl.toYaml();
    }

    /**
     * 转为 json
     */
    public String toJson() {
        return decl.toJson();
    }
}