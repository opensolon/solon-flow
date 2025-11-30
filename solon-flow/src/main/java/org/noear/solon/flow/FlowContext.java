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

import org.noear.dami2.Dami;
import org.noear.dami2.bus.DamiBus;
import org.noear.solon.flow.stateful.StatefulSupporter;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.flow.stateless.StatelessFlowContext;
import org.noear.solon.flow.stateful.StateController;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.flow.stateful.StatefulFlowContext;
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
        //无状态
        return new StatelessFlowContext();
    }

    static FlowContext of(String instanceId) {
        //无状态
        return new StatelessFlowContext(instanceId);
    }

    static FlowContext of(String instanceId, StateController stateController, StateRepository stateRepository) {
        //有状态
        return new StatefulFlowContext(instanceId, stateController, stateRepository);
    }

    static FlowContext of(String instanceId, StateController stateController) {
        //有状态（用于一次性场景）
        return new StatefulFlowContext(instanceId, stateController, new InMemoryStateRepository());
    }

    /**
     * 异步执行器
     */
    @Preview("3.3")
    @Nullable
    ExecutorService executor();

    /**
     * 配置异步执行器
     */
    @Preview("3.3")
    FlowContext executor(ExecutorService executor);

    /**
     * 交换器（只在任务执行时可获取）
     *
     * @since 3.5
     */
    @Preview("3.5")
    @Nullable
    default FlowExchanger exchanger() {
        return getAs(FlowExchanger.TAG);
    }

    /**
     * 中断（仅对当前分支有效）
     *
     * @since 3.7
     */
    @Preview("3.7")
    default void interrupt() {
        FlowExchanger exchanger = exchanger();
        if (exchanger != null) {
            exchanger.interrupt();
        }
    }

    /**
     * 停止（即结束运行）
     *
     * @since 3.7
     */
    @Preview("3.7")
    default void stop() {
        FlowExchanger exchanger = exchanger();
        if (exchanger != null) {
            exchanger.stop();
        }
    }

    /**
     * 数据模型
     */
    Map<String, Object> model();

    /// /////////////////////////////////////

    /**
     * 获取流实例id
     */
    default String getInstanceId() {
        return getAs("instanceId");
    }

    /**
     * 是否为有状态的
     */
    @Preview("3.5")
    boolean isStateful();

    /**
     * 获取有状态的支持者
     */
    @Preview("3.5")
    StatefulSupporter statefulSupporter();


    /// /////////////////////////////////////


    /**
     * 获取事件总线（based damibus）
     */
    default DamiBus eventBus() {
        //通过模型，可以被转移或替代
        return computeIfAbsent("eventBus", k -> Dami.newBus());
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
     * 增量添加
     */
    default int incrAdd(String key, int delta) {
        AtomicInteger tmp = (AtomicInteger) model().computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.addAndGet(delta);
    }

    /**
     * 增量获取
     */
    default int incrGet(String key) {
        AtomicInteger tmp = (AtomicInteger) model().computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.get();
    }

    /**
     * 移除
     */
    default void remove(String key) {
        model().remove(key);
    }
}