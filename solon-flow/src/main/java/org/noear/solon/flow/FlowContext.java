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
import org.noear.liquor.eval.Scripts;
import org.noear.solon.core.util.Assert;
import org.noear.solon.lang.Preview;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.0
 */
@Preview("3.0")
public class FlowContext {
    //存放数据模型
    private transient final Map<String, Object> model = new ConcurrentHashMap<>();
    //存放执行结果（可选）
    public transient Object result;

    //控制过程计数
    private transient final Counter counter = new Counter();
    //控制分支阻断（可选）
    private transient boolean interrupted = false;
    //控制流程停止（可选）
    private transient boolean stopped = false;

    //当前流程引擎
    protected transient FlowEngine engine;

    public FlowContext() {
        this(null);
    }

    public FlowContext(String instanceId) {
        put("context", this);
        put("instanceId", (instanceId == null ? "" : instanceId));
    }

    private FlowContext bak;

    /**
     * 备份
     */
    public void backup() {
        bak = new FlowContext();
        bak.putAll(this.model);
        bak.result = this.result;
        bak.interrupted = this.interrupted;
        bak.stopped = this.stopped;
        bak.counter.from(this.counter);
    }

    /**
     * 恢复
     */
    public void recovery() {
        if (bak != null) {
            this.model.clear();
            this.putAll(bak.model);
            this.put("context", this);
            this.result = bak.result;
            this.interrupted = bak.interrupted;
            this.stopped = bak.stopped;
            this.counter.from(bak.counter);
        }
    }

    /**
     * 设置临时结果（有些脚本引擎必须用属性方式）
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * 获取临时结果
     */
    public Object getResult() {
        return result;
    }

    /**
     * 当前流程引擎
     */
    public FlowEngine engine() {
        return engine;
    }


    /**
     * 计数器
     */
    public Counter counter() {
        return counter;
    }

    /**
     * 手动下一步（可能要配合中断使用 {@link #interrupt()}）
     *
     * @param node 节点
     */
    public void manualNext(Node node) throws FlowException {
        manualNext(node, -1);
    }

    /**
     * 手动下一步（可能要配合中断使用 {@link #interrupt()}）
     *
     * @param node  节点
     * @param depth 执行深度
     */
    public void manualNext(Node node, int depth) throws FlowException {
        if (node.getType() != NodeType.ACTIVITY) {
            throw new IllegalArgumentException(node.getId() + " is not activity");
        }

        for (Node node1 : node.getNextNodes()) {
            engine.eval(node1, depth, this);
        }
    }

    /**
     * 运行任务
     *
     * @param node        节点
     * @param description 任务描述
     */
    public void runTask(Node node, String description) throws FlowException {
        Assert.notNull(node, "node is null");

        try {
            engine().getDriver(node.getChain()).handleTask(this, new Task(node, description));
        } catch (FlowException e) {
            throw e;
        } catch (Throwable e) {
            throw new FlowException("The task handle failed: " + node.getChain().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 运行脚本
     *
     * @param script 脚本
     */
    public Object runScript(String script) throws InvocationTargetException {
        //按脚本运行
        return Scripts.eval(script, this.model());
    }

    /**
     * 运行脚本
     *
     * @param script 脚本
     * @deprecated 3.3 {@link #runScript(String)}
     */
    @Deprecated
    public Object run(String script) throws InvocationTargetException {
        //按脚本运行
        return Scripts.eval(script, this.model());
    }

    /**
     * 是否已停止
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 停止（整个流程不再后流）
     */
    public void stop() {
        stopped = true;
    }

    /**
     * 是否已阻断
     */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * 阻断（当前分支不再后流）
     */
    public void interrupt() {
        this.interrupted = true;
    }

    /**
     * 阻断重置
     */
    public void interrupt(boolean interrupted) {
        this.interrupted = interrupted;
    }

    /// //////////////////////////////////////////////////

    /**
     * 数据模型
     */
    public Map<String, Object> model() {
        return model;
    }

    /**
     * 推入
     */
    public <Slf extends FlowContext> Slf put(String key, Object value) {
        if (value != null) {
            model.put(key, value);
        }
        return (Slf) this;
    }

    /**
     * 推入
     */
    public <Slf extends FlowContext> Slf putIfAbsent(String key, Object value) {
        if (value != null) {
            model.putIfAbsent(key, value);
        }
        return (Slf) this;
    }

    /**
     * 推入全部
     */
    public <Slf extends FlowContext> Slf putAll(Map<String, Object> model) {
        this.model.putAll(model);
        return (Slf) this;
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