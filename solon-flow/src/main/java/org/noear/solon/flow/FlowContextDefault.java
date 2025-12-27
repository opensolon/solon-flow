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
import org.noear.snack4.codec.TypeRef;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.5
 */
@Preview("3.5")
public class FlowContextDefault implements FlowContext {

    //存放数据模型
    private transient final Map<String, Object> model = new ConcurrentHashMap<>();
    private transient final List<NodeTrace> nodeTraces = new ArrayList<>();
    //异步执行器
    private transient volatile ExecutorService executor;
    //最后执行节点
    private transient volatile NodeTrace lastNode;

    public FlowContextDefault() {
        this(null);
    }

    public FlowContextDefault(String instanceId) {
        put("instanceId", (instanceId == null ? "" : instanceId));
        put("context", this); //放这里不需要不断的推入移出，性能更好（序列化是要移除）
    }
    private static final Options OPTIONS = Options.of(
            Feature.Read_AutoType,
            Feature.Write_ClassName,
            Feature.Write_NotMapClassName);

    @Override
    public String toJson() {
        ONode oNode = new ONode(OPTIONS).asObject();
        oNode.getOrNew("model").then(n -> {
            model.forEach((k, v) -> {
                if (FlowContext.TAG.equals(k) ||
                        FlowExchanger.TAG.equals(k) ||
                        "eventBus".equals(k)) {
                    return;
                }

                n.set(k, v);
            });
        });

        oNode.set("nodeTraces", nodeTraces);
        oNode.set("lastNode", lastNode);

        return oNode.toJson();
    }

    @Override
    public FlowContext loadJson(String json){
        ONode oNode = ONode.ofJson(json, OPTIONS);

        if (oNode.hasKey("model")) {
            model.putAll(oNode.get("model").toBean(Map.class));
        }

        if (oNode.hasKey("nodeTraces")) {
            nodeTraces.addAll(oNode.get("nodeTraces").toBean(TypeRef.listOf(NodeTrace.class)));
        }

        if (oNode.hasKey("lastNode")) {
            lastNode = oNode.get("lastNode").toBean(NodeTrace.class);
        }

        return this;
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

    @Override
    public Collection<NodeTrace> nodeTraces() {
        return nodeTraces;
    }

    /**
     * 最后运行的节点
     */
    @Preview("3.7")
    @Override
    public @Nullable NodeTrace lastNode() {
        return lastNode;
    }

    public void lastNode(Node node) {
        this.lastNode = new NodeTrace(node);
        this.nodeTraces.add(lastNode);
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
    public <T> T getAs(String key) {
        return (T) model.get(key);
    }

    public Object getAsObject(String key) {
        return getAs(key);
    }

    public String getAsString(String key) {
        return getAs(key);
    }

    public Number getAsNumber(String key) {
        return getAs(key);
    }

    public Boolean getAsBoolean(String key) {
        return getAs(key);
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
        return getAs("instanceId");
    }

    /**
     * 获取事件总线（based damibus）
     */
    public DamiBus eventBus() {
        //通过模型，可以被转移或替代
        return computeIfAbsent("eventBus", k -> Dami.newBus());
    }
}