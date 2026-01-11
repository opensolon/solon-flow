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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
        return new FlowEngineDefault(null, false);
    }

    /**
     * 新实例
     */
    static FlowEngine newInstance(FlowDriver driver) {
        return new FlowEngineDefault(driver, false);
    }

    /**
     * 新实例
     */
    static FlowEngine newInstance(boolean simplified) {
        return new FlowEngineDefault(null, simplified);
    }

    /**
     * 然后（构建自己）
     */
    default FlowEngine then(Consumer<FlowEngine> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * 获取驱动
     */
    FlowDriver getDriver(Graph graph);

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
        register(null, driver);
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
        eval(graphId, -1, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param context 上下文
     */
    default void eval(String graphId, FlowContext context) throws FlowException {
        eval(graphId, -1, context);
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param steps   步数
     * @param context 上下文
     */
    default void eval(String graphId, int steps, FlowContext context) throws FlowException {
        eval(graphId, steps, context, null);
    }

    /**
     * 运行
     *
     * @param graphId 图Id
     * @param steps   步数
     * @param context 上下文
     * @param options 选项
     */
    default void eval(String graphId, int steps, FlowContext context, FlowOptions options) throws FlowException {
        Graph graph = getGraphOrThrow(graphId);
        eval(graph, steps, context, options);
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
        eval(graph, -1, context);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param steps   步数
     * @param context 上下文
     */
    default void eval(Graph graph, int steps, FlowContext context) throws FlowException {
        eval(graph, steps, context, null);
    }

    /**
     * 运行
     *
     * @param graph   图
     * @param steps   步数
     * @param context 上下文
     * @param options 选项
     */
    default void eval(Graph graph, int steps, FlowContext context, FlowOptions options) throws FlowException {
        FlowDriver driver = getDriver(graph);

        eval(graph, new FlowExchanger(graph, this, driver, context, options, steps, new AtomicInteger(0)));
    }

    /**
     * 运行
     *
     * @param graph     图
     * @param exchanger 交换器
     */
    @Internal
    void eval(Graph graph, FlowExchanger exchanger) throws FlowException;
}