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
import org.noear.solon.lang.Preview;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.function.Consumer;

/**
 * 图
 *
 * <pre>{@code
 * Graph graph = Graph.create("demo1", "示例", spec -> {
 *     spec.addStart("s").title("开始").linkAdd("n1");
 *     spec.addActivity("n1").task("@AaMetaProcessCom").linkAdd("e");
 *     spec.addEnd("e").title("结束");
 * });
 *
 * flowEngine.eval(graph);
 * }</pre>
 *
 * <pre>{@code
 * Graph graphNew = Graph.copy(graph, spec -> {
 *     spec.getNode("n1").linkRemove("e").linkAdd("n2"); //移掉 n1 连接；改为 n2 连接
 *     spec.addActivity("n2").linkAdd("e");
 * });
 *
 * flowEngine.eval(graphNew);
 * }</pre>
 *
 * @author noear
 * @since 3.0
 * @since 3.7
 * */
@Preview("3.0")
public class Graph {
    private transient final String id;
    private transient final String title;
    private transient final String driver;
    private transient final Map<String, Object> metas;

    private transient final Map<String, Node> nodes;
    private transient final List<Link> links;
    private transient Node start;

    protected Graph(GraphSpec spec) {
        this.id = spec.getId();
        this.title = spec.getTitle();
        this.driver = spec.getDriver();

        Map<String, Node> nodeMap = new LinkedHashMap<>(spec.getNodes().size());
        List<Link> linkAry = new ArrayList<>(spec.getNodes().size());

        //倒排加入图
        for (Map.Entry<String, NodeSpec> kv : spec.getNodes().entrySet()) {
            doAddNode(kv.getValue(), nodeMap, linkAry);
        }

        //正排加入图
        this.nodes = Collections.unmodifiableMap(nodeMap);
        this.links = Collections.unmodifiableList(linkAry);
        if (spec.getMeta() == null) {
            this.metas = Collections.emptyMap();
        } else {
            this.metas = Collections.unmodifiableMap(spec.getMeta());
        }

        //校验结构
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
            throw new IllegalStateException("No start node found, graph: " + spec);
        }
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
     */
    public <T> T getMetaAs(String key) {
        return (T) metas.get(key);
    }

    /**
     * 获取元数据或默认
     */
    public <T> T getMetaOrDefault(String key, T def) {
        return (T) metas.getOrDefault(key, def);
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
        return nodes;
    }

    /**
     * 获取所有连接
     */
    public List<Link> getLinks() {
        return links;
    }


    /**
     * 获取节点
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * 获取节点（如果没有则异常）
     */
    public Node getNodeOrThrow(String id) {
        Node node = getNode(id);
        if (node == null) {
            throw new IllegalArgumentException("Node not found, id: " + id);
        }

        return node;
    }

    /// ////////

    /**
     * 添加节点
     */
    private void doAddNode(NodeSpec nodeSpec, Map<String, Node> nodeMap, List<Link> linkAry) {
        List<Link> tmp = new ArrayList<>(nodeSpec.getLinks().size());
        for (LinkSpec linkSpec : nodeSpec.getLinks()) {
            tmp.add(new Link(this, nodeSpec.getId(), linkSpec));
        }

        linkAry.addAll(tmp);

        Node node = new Node(this, nodeSpec, tmp);
        nodeMap.put(node.getId(), node);
        if (nodeSpec.getType() == NodeType.START) {
            start = node;
        }
    }

    /// /////////////

    /**
     * 转为任务组件模式
     *
     * @since 3.8.1
     */
    @Preview("3.8.1")
    public NamedTaskComponent asTask(){
        return new GraphTaskComponent(this);
    }

    /// /////////////

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
     * 转为 PlantUML （状态图）文本
     */
    public String toPlantuml() {
        return toPlantuml(PlantumlOptions.DEFAULT, null);
    }

    /**
     * 转为 PlantUML （状态图）文本
     *
     * @param options 输出选项（null 时使用默认选项）
     * @since 3.10
     */
    public String toPlantuml(PlantumlOptions options) {
        return toPlantuml(options != null ? options : PlantumlOptions.DEFAULT, null);
    }

    /**
     * 转为 PlantUML （状态图）文本
     *
     * @param displayMappingFunc 显示映射函数，用于自定义节点和连接的显示内容
     * @since 3.10
     */
    public String toPlantuml(java.util.function.Function<PlantumlDisplayContext, PlantumlDisplayResult> displayMappingFunc) {
        return toPlantuml(PlantumlOptions.DEFAULT, displayMappingFunc);
    }

    /**
     * 转为 PlantUML （状态图）文本
     *
     * @param options            输出选项
     * @param displayMappingFunc 显示映射函数，用于自定义节点和连接的显示内容
     * @since 3.10
     */
    public String toPlantuml(PlantumlOptions options,
                              java.util.function.Function<PlantumlDisplayContext, PlantumlDisplayResult> displayMappingFunc) {
        // 确保 options 不为 null
        if (options == null) {
            options = PlantumlOptions.DEFAULT;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");

        // 全局样式优化，让状态图具有活动图的视觉美感
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam state {\n")
                .append("  BackgroundColor White\n")
                .append("  BorderColor #333333\n")
                .append("  FontName SansSerif\n")
                .append("  BackgroundColor<<Gateway>> #fff9c4\n") // 网关用淡黄色
                .append("  BorderColor<<Gateway>> #fbc02d\n")
                .append("}\n");

        if (Utils.isNotEmpty(title)) {
            if (options.isShowIdInTitle()) {
                sb.append("title ").append(title).append(" (").append(id).append(")\n");
            } else {
                sb.append("title ").append(title).append("\n");
            }
        } else if (options.isShowIdInTitle()) {
            sb.append("title ").append(id).append("\n");
        }

        // 1. 声明节点：遍历 nodes
        for (Node node : nodes.values()) {
            String nodeId = node.getId();

            // 渲染逻辑
            switch (node.getType()) {
                case START:
                    // 开始节点
                    sb.append("state ").append(nodeId).append(" <<start>>\n");
                    appendNodeTitle(sb, nodeId, node.getTitle());
                    break;
                case END:
                    // 结束节点
                    sb.append("state ").append(nodeId).append(" <<end>>\n");
                    appendNodeTitle(sb, nodeId, node.getTitle());
                    break;
                case EXCLUSIVE:
                case INCLUSIVE:
                case PARALLEL:
                case LOOP:
                    // 网关节点：使用 choice 刻板印象显示为菱形
                    sb.append("state ").append(nodeId).append(" <<choice>> <<Gateway>>\n");
                    appendNodeTitle(sb, nodeId, node.getTitle());
                    if (options.isShowGatewayType()) {
                        sb.append(nodeId).append(" : ").append(node.getType().name()).append("\n");
                    }
                    break;
                default:
                    // 业务活动节点：显示节点ID、标题和任务描述
                    sb.append("state ").append(nodeId).append("\n");
                    appendNodeTitle(sb, nodeId, node.getTitle());
                    appendNodeTask(sb, nodeId, node, displayMappingFunc);
                    break;
            }
        }

        // 2. 声明连接：遍历 links
        for (Link link : links) {
            // 基本语法: prevId --> nextId
            sb.append(link.getPrevId()).append(" --> ").append(link.getNextId());

            // 拼接连线上的标题或条件描述 (When)
            List<String> labels = new ArrayList<>();
            if (Utils.isNotEmpty(link.getTitle())) {
                labels.add(link.getTitle());
            }
            String whenText = buildLinkWhenText(link, displayMappingFunc);
            if (Utils.isNotEmpty(whenText)) {
                labels.add("[" + whenText + "]");
            }

            if (!labels.isEmpty()) {
                sb.append(" : ").append(String.join(" ", labels));
            }
            sb.append("\n");
        }

        sb.append("@enduml");
        return sb.toString();
    }

    private void appendNodeTitle(StringBuilder sb, String nodeId, String title) {
        if (Utils.isNotEmpty(title)) {
            sb.append(nodeId).append(" : ").append(title).append("\n");
        }
    }

    private void appendNodeTask(StringBuilder sb, String nodeId, Node node,
                                 java.util.function.Function<PlantumlDisplayContext, PlantumlDisplayResult> displayMappingFunc) {
        String task = node.getTask().getDescription();
        if (Utils.isEmpty(task)) {
            return;
        }

        if (displayMappingFunc != null) {
            try {
                PlantumlDisplayResult result = displayMappingFunc.apply(PlantumlDisplayContext.ofNode(node));
                if (result != null) {
                    if (!result.isVisible()) {
                        return; // 隐藏
                    }
                    if (result.isUseDefault()) {
                        sb.append(nodeId).append(" : ").append(task).append("\n");
                    } else {
                        sb.append(nodeId).append(" : ").append(result.getText()).append("\n");
                    }
                    return;
                }
            } catch (Exception e) {
                // 异常时使用默认处理
            }
        }

        // 默认处理
        sb.append(nodeId).append(" : ").append(task).append("\n");
    }

    private String buildLinkWhenText(Link link,
                                      java.util.function.Function<PlantumlDisplayContext, PlantumlDisplayResult> displayMappingFunc) {
        String when = link.getWhen().getDescription();
        if (Utils.isEmpty(when)) {
            return null;
        }

        if (displayMappingFunc != null) {
            try {
                PlantumlDisplayResult result = displayMappingFunc.apply(PlantumlDisplayContext.ofLink(link));
                if (result != null) {
                    if (!result.isVisible()) {
                        return null; // 隐藏
                    }
                    if (result.isUseDefault()) {
                        return when;
                    } else {
                        return result.getText();
                    }
                }
            } catch (Exception e) {
                // 异常时使用默认处理
            }
        }

        // 默认处理
        return when;
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

        if (Utils.isNotEmpty(metas)) {
            domRoot.put("meta", metas);
        }

        List<Map<String, Object>> domNodes = new ArrayList<>();
        domRoot.put("layout", domNodes);

        for (Map.Entry<String, Node> kv : nodes.entrySet()) {
            Node node = kv.getValue();

            Map<String, Object> domNode = new LinkedHashMap<>();
            domNodes.add(domNode);

            domNode.put("id", node.getId());
            domNode.put("type", node.getType().toString().toLowerCase());

            if (Utils.isNotEmpty(node.getTitle())) {
                domNode.put("title", node.getTitle());
            }

            if (Utils.isNotEmpty(node.getMetas())) {
                domNode.put("meta", node.getMetas());
            }

            if (Utils.isNotEmpty(node.getWhen().getDescription())) {
                domNode.put("when", node.getWhen().getDescription());
            }

            if (Utils.isNotEmpty(node.getTask().getDescription())) {
                domNode.put("task", node.getTask().getDescription());
            }

            if (Utils.isNotEmpty(node.getNextLinks())) {
                List<Map<String, Object>> domLinks = new ArrayList<>();
                domNode.put("link", domLinks);

                for (Link link : node.getNextLinks()) {
                    Map<String, Object> domLink = new LinkedHashMap<>();
                    domLinks.add(domLink);

                    domLink.put("nextId", link.getNextId());

                    if (Utils.isNotEmpty(link.getTitle())) {
                        domLink.put("title", link.getTitle());
                    }

                    if (Utils.isNotEmpty(link.getMetas())) {
                        domLink.put("meta", link.getMetas());
                    }

                    if (Utils.isNotEmpty(link.getWhen().getDescription())) {
                        domLink.put("when", link.getWhen().getDescription());
                    }
                }

            }
        }

        return domRoot;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                "}";
    }

    /// ////////

    /**
     * 创建
     *
     * @since 3.7
     */
    public static Graph create(String id, Consumer<GraphSpec> definition) {
        GraphSpec spec = new GraphSpec(id);
        definition.accept(spec);
        return spec.create();
    }

    /**
     * 创建
     *
     * @since 3.7
     */
    public static Graph create(String id, String title, Consumer<GraphSpec> definition) {
        GraphSpec spec = new GraphSpec(id, title);
        definition.accept(spec);
        return spec.create();
    }

    /**
     * 创建
     *
     * @since 3.7
     */
    public static Graph create(String id, String title, String driver, Consumer<GraphSpec> definition) {
        GraphSpec spec = new GraphSpec(id, title, driver);
        definition.accept(spec);
        return spec.create();
    }

    /**
     * 复制
     *
     * @since 3.7.4
     */
    public static Graph copy(Graph graph, Consumer<GraphSpec> modification) {
        GraphSpec spec = GraphSpec.copy(graph);
        modification.accept(spec);
        return spec.create();
    }

    /**
     * 解析配置文件
     */
    public static Graph fromUri(String uri) {
        return GraphSpec.fromUri(uri).create();
    }

    /**
     * 解析配置文本
     *
     * @param text 配置文本（支持 yml, json 格式）
     */
    public static Graph fromText(String text) {
        return GraphSpec.fromText(text).create();
    }
}