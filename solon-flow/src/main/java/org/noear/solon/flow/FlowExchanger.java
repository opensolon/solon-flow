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

import org.noear.dami.bus.DamiBus;
import org.noear.liquor.eval.Scripts;
import org.noear.solon.core.util.Assert;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 流执行交换器（对内，不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.5")
public class FlowExchanger {
    //当前流程上下文
    private transient final FlowContext flowContext;
    //当前流程引擎
    protected transient FlowEngine engine;

    //执行时临时存放器
    private transient final Temporary temporary = new Temporary();
    //执行时分支阻断（可选）
    private transient volatile boolean interrupted = false;
    //执行时流程停止（可选）
    private transient volatile boolean stopped = false;

    public FlowExchanger(FlowContext flowContext) {
        this.flowContext = flowContext;
    }

    /**
     * 当前上下文
     *
     */
    public FlowContext context() {
        return flowContext;
    }

    /**
     * 当前流程引擎
     */
    public FlowEngine engine() {
        return engine;
    }

    /**
     * 异步执行器
     */
    @Preview("3.3")
    @Nullable
    public ExecutorService executor() {
        return flowContext.executor();
    }

    /**
     * 临时存放器
     */
    public Temporary temporary() {
        return temporary;
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
        return flowContext.model();
    }

    /**
     * 推入
     */
    public <Slf extends FlowExchanger> Slf put(String key, Object value) {
        flowContext.put(key, value);
        return (Slf) this;
    }

    /**
     * 推入
     */
    public <Slf extends FlowExchanger> Slf putIfAbsent(String key, Object value) {
        flowContext.putIfAbsent(key, value);
        return (Slf) this;
    }

    /**
     * 推入全部
     */
    public <Slf extends FlowExchanger> Slf putAll(Map<String, Object> model) {
        flowContext.putAll(model);
        return (Slf) this;
    }

    /**
     * 尝试完成
     */
    public <T> T computeIfAbsent(String key, Function<String, T> mappingFunction) {
        return flowContext.computeIfAbsent(key, mappingFunction);
    }

    /**
     * 获取
     */
    public <T> T get(String key) {
        return (T) flowContext.get(key);
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
        return (T) flowContext.getOrDefault(key, def);
    }

    /**
     * 增量添加
     */
    public int incrAdd(String key, int delta) {
        AtomicInteger tmp = flowContext.computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.addAndGet(delta);
    }

    /**
     * 增量获取
     */
    public int incrGet(String key) {
        AtomicInteger tmp = flowContext.computeIfAbsent(key, k -> new AtomicInteger(0));
        return tmp.get();
    }

    /**
     * 移除
     */
    public void remove(String key) {
        flowContext.remove(key);
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
        return flowContext.eventBus();
    }
}