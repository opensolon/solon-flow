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
import org.noear.solon.flow.intercept.ChainInterceptor;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.lang.Preview;

import java.util.Collection;

/**
 * 流引擎
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
    FlowDriver getDriver(Chain chain);

    /**
     * 获取驱动
     */
    <T extends FlowDriver> T getDriverAs(Chain chain, Class<T> driverClass);

    /**
     * 有状态的服务
     */
    FlowStatefulService statefulService();

    /**
     * 添加拦截器
     *
     * @param index       顺序位
     * @param interceptor 拦截器
     */
    void addInterceptor(ChainInterceptor interceptor, int index);

    /**
     * 添加拦截器
     *
     * @param interceptor 拦截器
     */
    default void addInterceptor(ChainInterceptor interceptor) {
        addInterceptor(interceptor, 0);
    }

    /**
     * 移除拦截器
     *
     */
    void removeInterceptor(ChainInterceptor interceptor);


    /**
     * 注册链驱动器
     *
     * @param name   名字
     * @param driver 驱动器
     */
    void register(String name, FlowDriver driver);

    /**
     * 注册默认链驱动器
     *
     * @param driver 默认驱动器
     */
    default void register(FlowDriver driver) {
        register("", driver);
    }

    /**
     * 注销链驱动器
     */
    void unregister(String name);


    /**
     * 解析配置文件
     *
     * @param chainUri 链资源地址
     */
    default void load(String chainUri) {
        if (chainUri.contains("*")) {
            for (String u1 : ResourceUtil.scanResources(chainUri)) {
                load(Chain.parseByUri(u1));
            }
        } else {
            load(Chain.parseByUri(chainUri));
        }
    }

    /**
     * 加载链
     *
     * @param chain 链
     */
    void load(Chain chain);

    /**
     * 卸载链
     *
     * @param chainId 链Id
     */
    void unload(String chainId);

    /**
     * 获取所有链
     */
    Collection<Chain> getChains();

    /**
     * 获取链
     */
    Chain getChain(String chainId);

    /**
     * 运行
     *
     * @param chainId 链Id
     */
    default void eval(String chainId) throws FlowException {
        eval(chainId, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param chainId 链Id
     * @param context 上下文
     */
    default void eval(String chainId, FlowContext context) throws FlowException {
        eval(chainId, null, -1, context);
    }

    /**
     * 运行
     *
     * @param chainId 链Id
     * @param startId 开始Id
     * @param context 上下文
     */
    default void eval(String chainId, String startId, FlowContext context) throws FlowException {
        eval(chainId, startId, -1, context);
    }

    /**
     * 运行
     *
     * @param chainId 链Id
     * @param startId 开始Id
     * @param depth   执行深度
     * @param context 上下文
     */
    default void eval(String chainId, String startId, int depth, FlowContext context) throws FlowException {
        eval(chainId, startId, depth, new FlowExchanger(context));
    }

    /**
     * 运行
     *
     * @param chainId   链Id
     * @param startId   开始Id
     * @param depth     执行深度
     * @param exchanger 交换器
     */
    void eval(String chainId, String startId, int depth, FlowExchanger exchanger) throws FlowException;

    /**
     * 运行
     *
     * @param chain 链
     */
    default void eval(Chain chain) throws FlowException {
        eval(chain, FlowContext.of());
    }

    /**
     * 运行
     *
     * @param chain   链
     * @param context 上下文
     */
    default void eval(Chain chain, FlowContext context) throws FlowException {
        eval(chain.getStart(), -1, context);
    }

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
        eval(startNode, depth, new FlowExchanger(context));
    }

    /**
     * 运行
     *
     * @param startNode 开始节点
     * @param depth     执行深度
     * @param exchanger 交换器
     */
    void eval(Node startNode, int depth, FlowExchanger exchanger) throws FlowException;
}