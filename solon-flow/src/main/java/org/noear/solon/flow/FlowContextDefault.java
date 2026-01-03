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
import org.noear.snack4.Feature;
import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;
import org.noear.solon.util.RunnableTx;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.5
 */
@Preview("3.5")
public class FlowContextDefault implements FlowContextInternal {
    static final String TAG = "context";

    //存放数据模型
    private transient final Map<String, Object> model = new ConcurrentHashMap<>();
    //痕迹
    private transient FlowTrace trace = new FlowTrace();
    //交换器
    private transient volatile FlowExchanger exchanger;
    //事件总线
    private transient volatile DamiBus eventBus;
    //是否已停止
    private transient volatile boolean stopped;

    protected FlowContextDefault() {
        this(null);
    }

    protected FlowContextDefault(String instanceId) {
        put("instanceId", (instanceId == null ? "" : instanceId));
        put("context", this); //放这里不需要不断的推入移出，性能更好（序列化是要移除）
    }

    private static final Options OPTIONS = Options.of(
            Feature.Read_AutoType,
            Feature.Write_ClassName,
            Feature.Write_NotMapClassName, //比如只读的 Map 避免有类名
            Feature.Write_EnumUsingName);

    protected static FlowContext fromJson(String json) {
        FlowContextDefault tmp = new FlowContextDefault();
        ONode oNode = ONode.ofJson(json, OPTIONS);

        if (oNode.hasKey("model")) {
            tmp.model.putAll(oNode.get("model").toBean(Map.class));
        }

        if (oNode.hasKey("trace")) {
            tmp.trace = oNode.get("trace").toBean(FlowTrace.class);
        }

        return tmp;
    }

    @Override
    public String toJson() {
        ONode oNode = new ONode(OPTIONS).asObject();
        oNode.getOrNew("model").then(n -> {
            model.forEach((k, v) -> {
                if (TAG.equals(k)) {
                    return;
                }

                n.set(k, ONode.ofBean(v, OPTIONS));
            });
        });

        if(trace != null) {
            oNode.set("trace", ONode.ofBean(trace));
        }

        return oNode.toJson();
    }


    @Override
    public void exchanger(FlowExchanger exchanger) {
        this.exchanger = exchanger;
    }

    @Override
    public @Nullable FlowExchanger exchanger() {
        return exchanger;
    }

    /**
     * 获取事件总线（based damibus）
     */
    public DamiBus eventBus() {
        if (eventBus == null) {
            eventBus = Dami.newBus();
        }

        return eventBus;
    }

    /**
     * 中断（仅对当前分支有效）
     */
    public void interrupt() {
        if (exchanger != null) {
            exchanger.interrupt();
        }
    }

    /**
     * 停止（即结束运行）
     */
    public void stop() {
        if (exchanger != null) {
            exchanger.stop();
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void stopped(boolean stopped) {
        this.stopped = stopped;
    }

    /// //////////////////

    @Override
    public FlowTrace trace() {
        return trace;
    }

    @Override
    public FlowContext enableTrace(boolean enable) {
        trace.enable(enable);
        return this;
    }

    @Override
    public void lastNode(Graph graph, @Nullable Node node) {
        if (trace.isEnabled() == false) {
            return;
        }

        Objects.requireNonNull(graph, "graph");

        if(node == null) {
            trace.record(graph.getStart());
        } else {
            trace.record(node);
        }
    }

    @Override
    public @Nullable NodeRecord lastNode(String graphId) {
        return trace.last(graphId);
    }

    @Override
    public @Nullable String lastNodeId(String graphId) {
        return trace.lastNodeId(graphId);
    }

    /// //////////////////

    /**
     * 数据模型
     */
    public Map<String, Object> model() {
        return model;
    }


    /**
     * 获取流实例id
     */
    public String getInstanceId() {
        return getAs("instanceId");
    }

    @Override
    public <X extends Throwable> void with(String key, Object value, RunnableTx<X> runnable) throws X {
        Object bak = get(key);

        try {
            put(key, value);
            runnable.run();
        } finally {
            if (bak == null) {
                remove(key);
            } else {
                put(key, bak);
            }
        }
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
    public <T> T getAs(String key) {
        return (T) model.get(key);
    }

    /**
     * 获取或默认
     */
    public <T> T getOrDefault(String key, T def) {
        return (T) model.getOrDefault(key, def);
    }

    /**
     * 移除
     */
    public void remove(String key) {
        model.remove(key);
    }
}