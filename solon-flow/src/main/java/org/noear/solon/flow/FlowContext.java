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

import org.noear.liquor.eval.Scripts;
import org.noear.solon.lang.Preview;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.0
 */
@Preview("3.0")
public class FlowContext {
    //存放数据模型
    private transient final Map<String, Object> model = new LinkedHashMap<>();
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

    public FlowContext(Map<String, Object> model) {
        this.model.put("context", this);
        if (model != null) {
            this.model.putAll(model);
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
     * 下一步
     *
     * @param node 节点
     */
    public void next(Node node) throws Throwable {
        if (node.getType() != NodeType.activity) {
            throw new IllegalArgumentException(node.getId() + " is not execute");
        }

        for (Node node1 : node.getNextNodes()) {
            engine.eval(node.getChain(), node1.getId(), -1, this);
        }
    }

    /**
     * 运行脚本
     */
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
    protected void interrupt(boolean interrupted) {
        this.interrupted = interrupted;
    }

    /// ////////

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
        model.put(key, value);
        return (Slf) this;
    }

    /**
     * 推入
     */
    public <Slf extends FlowContext> Slf putIfAbsent(String key, Object value) {
        model.putIfAbsent(key, value);
        return (Slf) this;
    }

    /**
     * 推入全部
     */
    public <Slf extends FlowContext> Slf putAll(Map<String, Object> model) {
        model.putAll(model);
        return (Slf) this;
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
}