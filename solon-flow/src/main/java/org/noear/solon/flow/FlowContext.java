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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
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

    /// ////////////

    /**
     * 转为 json（用于持久化）
     */
    String toJson();

    /**
     * 加载 json（用于持久化）
     */
    FlowContext loadJson(String json);

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

    /// ////////////

    /**
     * 最后运行的节点
     */
    @Preview("3.7.4")
    @Nullable
    NodeTrace lastNode();

    /**
     * 记录最后运行的节点
     */
    void lastNode(Node node);

    /**
     * 最后运行的节点Id
     */
    default String lastNodeId() {
        if (lastNode() != null) {
            return lastNode().getId();
        }

        return null;
    }

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

    /**
     * 增量添加
     *
     * @since 3.8
     */
    @Deprecated
    default int incrAdd(String key, int delta) {
        AtomicInteger tmp = (AtomicInteger) model().computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.addAndGet(delta);
    }

    /**
     * 增量获取
     *
     * @since 3.8
     */
    @Deprecated
    default int incrGet(String key) {
        AtomicInteger tmp = (AtomicInteger) model().computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.get();
    }
}