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

import org.noear.dami.Dami;
import org.noear.dami.bus.DamiBus;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.0")
public class FlowContextDefault implements FlowContext {
    //存放数据模型
    private transient final Map<String, Object> model = new ConcurrentHashMap<>();
    //异步执行器
    private transient ExecutorService executor;

    public FlowContextDefault() {
        this(null);
    }

    public FlowContextDefault(String instanceId) {
        put("instanceId", (instanceId == null ? "" : instanceId));
    }

    /**
     * 异步执行器
     */
    @Preview("3.3")
    public @Nullable ExecutorService executor() {
        return executor;
    }

    /**
     * 配置异步执行器
     */
    @Preview("3.3")
    public FlowContextDefault executor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    /**
     * 数据模型
     */
    public Map<String, Object> model() {
        return model;
    }

    /**
     * 推入
     */
    public FlowContextDefault put(String key, Object value) {
        if (value != null) {
            model.put(key, value);
        }
        return this;
    }

    /**
     * 推入
     */
    public FlowContextDefault putIfAbsent(String key, Object value) {
        if (value != null) {
            model.putIfAbsent(key, value);
        }
        return this;
    }

    /**
     * 推入全部
     */
    public FlowContextDefault putAll(Map<String, Object> model) {
        this.model.putAll(model);
        return this;
    }

    /**
     * 尝试完成
     */
    public <T> T computeIfAbsent(String key, Function<String, T> mappingFunction) {
        return (T) model.computeIfAbsent(key, mappingFunction);
    }

    /**
     * 获取
     */
    public <T> T get(String key) {
        return (T) model.get(key);
    }

    public Object getAsObject(String key) {
        return get(key);
    }

    public String getAsString(String key) {
        return get(key);
    }

    public Number getAsNumber(String key) {
        return get(key);
    }

    public Boolean getAsBoolean(String key) {
        return get(key);
    }

    /**
     * 获取或默认
     */
    public <T> T getOrDefault(String key, T def) {
        return (T) model.getOrDefault(key, def);
    }

    /**
     * 增量添加
     */
    public int incrAdd(String key, int delta) {
        AtomicInteger tmp = (AtomicInteger) model.computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.addAndGet(delta);
    }

    /**
     * 增量获取
     */
    public int incrGet(String key) {
        AtomicInteger tmp = (AtomicInteger) model.computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.get();
    }

    /**
     * 移除
     */
    public void remove(String key) {
        model.remove(key);
    }

    /**
     * 获取流实例id
     */
    public String getInstanceId() {
        return get("instanceId");
    }

    /**
     * 获取事件总线（based damibus）
     */
    public <C extends Object, R extends Object> DamiBus<C, R> eventBus() {
        //通过模型，可以被转移或替代
        return computeIfAbsent("eventBus", k -> Dami.newBus());
    }
}