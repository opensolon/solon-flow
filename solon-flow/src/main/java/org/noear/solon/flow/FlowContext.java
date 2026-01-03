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

import org.noear.dami2.bus.DamiBus;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;
import org.noear.solon.util.RunnableTx;

import java.util.Map;
import java.util.function.Function;

/**
 * 流上下文，表示一个流实例的上下文数据（对外，不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.0")
public interface FlowContext {
    static FlowContext of() {
        return new FlowContextDefault();
    }

    static FlowContext of(String instanceId) {
        return new FlowContextDefault(instanceId);
    }

    /**
     * 从 json 加载（用于持久化）
     *
     * @since 3.8.1
     */
    static FlowContext fromJson(String json) {
        return FlowContextDefault.fromJson(json);
    }

    /// ////////////

    /**
     * 转为 json（用于持久化）
     *
     * @since 3.8.1
     */
    String toJson();


    /// ////////////

    /**
     * 获取事件总线（based damibus）
     */
    DamiBus eventBus();

    /**
     * 中断当前分支（如果有其它分支，仍会执行）
     *
     * @since 3.7
     */
    void interrupt();

    /**
     * 停止执行（即结束运行）
     *
     * @since 3.7
     */
    void stop();

    /**
     * 是否已停止（用于外部检测）
     */
    boolean isStopped();

    /// ////////////

    /**
     * 启用跟踪（默认为启用）
     */
    void enableTrace(boolean enable);

    /**
     * 根图最后运行的节点
     */
    @Preview("3.8.0")
    @Nullable
    default NodeRecord lastNode(){
        return lastNode(null);
    }

    /**
     * 子图最后运行的节点
     */
    @Nullable
    NodeRecord lastNode(String graphId);

    /**
     * 根图最后运行的节点Id
     *
     * @since 3.8.0
     */
    @Preview("3.8.0")
    @Nullable
    default String lastNodeId(){
        return lastNodeId(null);
    }

    /**
     * 子图最后运行的节点Id
     *
     * @since 3.8.0
     */
    @Nullable
    String lastNodeId(String graphId);

    /// ////////////

    /**
     * 数据模型
     */
    Map<String, Object> model();

    /**
     * 获取流实例id
     */
    default String getInstanceId() {
        return getAs("instanceId");
    }


    /**
     * 临时域变量
     */
    <X extends Throwable> void with(String key, Object value, RunnableTx<X> runnable) throws X;


    /**
     * 推入
     */
    default FlowContext put(String key, Object value) {
        if (value != null) {
            model().put(key, value);
        }
        return this;
    }

    /**
     * 推入
     */
    default FlowContext putIfAbsent(String key, Object value) {
        if (value != null) {
            model().putIfAbsent(key, value);
        }
        return this;
    }

    /**
     * 推入全部
     */
    default FlowContext putAll(Map<String, Object> model) {
        this.model().putAll(model);
        return this;
    }

    /**
     * 尝试完成
     */
    default <T> T computeIfAbsent(String key, Function<String, T> mappingFunction) {
        return (T) model().computeIfAbsent(key, mappingFunction);
    }

    /**
     * 包含 key
     *
     */
    default boolean containsKey(String key) {
        return model().containsKey(key);
    }

    /**
     * 获取
     */
    default Object get(String key) {
        return model().get(key);
    }

    /**
     * 获取
     */
    default <T> T getAs(String key) {
        return (T) model().get(key);
    }

    /**
     * 获取或默认
     */
    default <T> T getOrDefault(String key, T def) {
        return (T) model().getOrDefault(key, def);
    }

    /**
     * 移除
     */
    default void remove(String key) {
        model().remove(key);
    }
}