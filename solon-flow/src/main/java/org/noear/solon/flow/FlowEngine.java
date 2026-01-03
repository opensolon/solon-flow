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

import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.noear.solon.lang.Internal;
import org.noear.solon.lang.Preview;

import java.util.Collection;

/**
 * 流引擎（通用流程引擎）
 *
 * <pre>{@code
 * FlowEngine flowEngine = FlowEngine.newInstance();
 *
 * flowEngine.eval("g1", FlowContext.of());
 * }</pre>
 *
 * <pre>{@code
 * FlowEngine flowEngine = FlowEngine.newInstance(SimpleFlowDriver.builder()
 *                 .container(container)
 *                 .build())
 *
 * flowEngine.eval("g1", FlowContext.of());
 * }</pre>
 *
 * @author noear
 * @since 3.0
 * */
@Preview("3.0")
public interface FlowEngine {
    /**
     * 新实例
     */
    static FlowEngine newInstance() {
        return new FlowEngineDefault();
    }

    /**
     * 新实例
     */
    static FlowEngine newInstance(FlowDriver driver) {
        return new FlowEngineDefault(driver);
    }

    /**
     * 获取驱动
     */
    FlowDriver getDriver(Graph graph);

    /**
     * 获取驱动
     */
    <T extends FlowDriver> T getDriverAs(Graph graph, Class<T> driverClass);

    /**
     * 添加拦截器
     *
     * @param index       顺序位
     * @param interceptor 拦截器
     */
    void addInterceptor(FlowInterceptor interceptor, int index);

    /**
     * 添加拦截器
     *
     * @param interceptor 拦截器
     */
    default void addInterceptor(FlowInterceptor interceptor) {
        addInterceptor(interceptor, 0);
    }

    /**
     * 移除拦截器
     */
    void removeInterceptor(FlowInterceptor interceptor);


    /**
     * 注册驱动器
     *
     * @param name   名字
     * @param driver 驱动器
     */
    void register(String name, FlowDriver driver);

    /**
     * 注册默认驱动器
     *
     * @param driver 默认驱动器
     */
    default void register(FlowDriver driver) {
        register("", driver);
    }

    /**
     * 注销驱动器
     */
    void unregister(String name);


    /**
     * 解析配置文件
     *
     * @param graphUri 图资源地址
     */
    default void load(String graphUri) {
        if (graphUri.contains("*")) {
            for (String u1 : ResourceUtil.scanResources(graphUri)) {
                load(Graph.fromUri(u1));
            }
        } else {
            load(Graph.fromUri(graphUri));
        }
    }

    /**
     * 加载图
     *
     * @param graph 图
     */
    void load(Graph graph);

    /**
     * 卸载图
     *
     * @param graphId 图Id
     */
    void unload(String graphId);

    /**
     * 获取所有图
     */
    Collection<Graph> getGraphs();

    /**
     * 获取图
     */
    Graph getGraph(String graphId);

    /**
     * 获取图
     */
    default Graph getGraphOrThrow(String graphId) {
        Graph graph = getGraph(graphId);
        if (graph == null) {
            throw new FlowException("Flow graph not found: " + graphId);
        }
        return graph;
    }

    /// ////////////////////

    /**
     * 运行
     *
     * @param graphId 图Id
     */
    default void eval(String graphId) throws FlowException {
        eval(graphId, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param context 上下文
     */
    default void eval(String graphId, FlowContext context) throws FlowException {
        Graph graph = getGraphOrThrow(graphId);
        eval(graph, graph.getStart(), -1, context);
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param startId 开始节点Id
     * @since 3.8
     */
    default void eval(String graphId, String startId) throws FlowException {
        eval(graphId, startId, -1, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param startId 开始节点Id
     * @param context 上下文
     */
    default void eval(String graphId, String startId, FlowContext context) throws FlowException {
        eval(graphId, startId, -1, context);
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param startId 开始节点Id
     * @param depth   执行深度
     * @param context 上下文
     */
    default void eval(String graphId, String startId, int depth, FlowContext context) throws FlowException {
        Graph graph = getGraphOrThrow(graphId);
        Node startNode = (startId == null ? graph.getStart() : graph.getNodeOrThrow(startId));
        eval(graph, startNode, depth, context);
    }

    /**
     * 运行
     *
     * @param graphId   图Id
     * @param startNode 开始节点
     * @since 3.8
     */
    default void eval(String graphId, Node startNode) throws FlowException {
        eval(graphId, startNode, -1, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graphId   图Id
     * @param startNode 开始节点
     * @param context   上下文
     * @since 3.8
     */
    default void eval(String graphId, Node startNode, FlowContext context) throws FlowException {
        eval(graphId, startNode, -1, context);
    }

    /**
     * 运行
     *
     * @param graphId   图Id
     * @param startNode 开始节点
     * @param depth     执行深度
     * @param context   上下文
     */
    default void eval(String graphId, Node startNode, int depth, FlowContext context) throws FlowException {
        Graph graph = getGraphOrThrow(graphId);
        eval(graph, startNode, depth, context);
    }


    /**
     * 运行
     *
     * @param graph 图
     */
    default void eval(Graph graph) throws FlowException {
        eval(graph, FlowContext.of());
    }


    /**
     * 运行
     *
     * @param graph   图
     * @param context 上下文
     */
    default void eval(Graph graph, FlowContext context) throws FlowException {
        eval(graph, graph.getStart(), -1, context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param startId 开始节点Id
     */
    default void eval(Graph graph, String startId) throws FlowException {
        eval(graph, startId, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param startId 开始节点Id
     * @param context 上下文
     */
    default void eval(Graph graph, String startId, FlowContext context) throws FlowException {
        eval(graph, startId, -1, context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param startId 开始节点Id
     * @param context 上下文
     */
    default void eval(Graph graph, String startId, int depth, FlowContext context) throws FlowException {
        Node startNode = (startId == null ? graph.getStart() : graph.getNodeOrThrow(startId));
        eval(graph, startNode, depth, context);
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param startNode 开始节点
     */
    default void eval(Graph graph, Node startNode) throws FlowException {
        eval(graph, startNode, -1, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param startNode 开始节点
     * @param context   上下文
     */
    default void eval(Graph graph, Node startNode, FlowContext context) throws FlowException {
        eval(graph, startNode, -1, context);
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param startNode 开始节点
     * @param depth     深度
     * @param context   上下文
     */
    default void eval(Graph graph, Node startNode, int depth, FlowContext context) throws FlowException {
        FlowDriver driver = getDriver(startNode.getGraph());
        eval(graph, startNode, depth, new FlowExchanger(this, driver, context));
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param startId   开始节点Id
     * @param depth     深度
     * @param exchanger 交换器
     */
    @Internal
    default void eval(Graph graph, String startId, int depth, FlowExchanger exchanger) throws FlowException {
        eval(graph, graph.getNodeOrThrow(startId), depth, exchanger);
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param startNode 开始节点
     * @param depth     深度
     * @param exchanger 交换器
     */
    @Internal
    void eval(Graph graph, Node startNode, int depth, FlowExchanger exchanger) throws FlowException;


    /// ///////////////////////////////////////////////////////////////////////////////

    /**
     * 运行
     *
     * @param startNode 开始节点
     */
    default void eval(Node startNode) throws FlowException {
        eval(startNode, -1, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param startNode 开始节点
     * @param context   上下文
     */
    default void eval(Node startNode, FlowContext context) throws FlowException {
        eval(startNode, -1, context);
    }

    /**
     * 运行
     *
     * @param startNode 开始节点
     * @param depth     执行深度
     * @param context   上下文
     */
    default void eval(Node startNode, int depth, FlowContext context) throws FlowException {
        eval(startNode.getGraph(), startNode, depth, context);
    }
}