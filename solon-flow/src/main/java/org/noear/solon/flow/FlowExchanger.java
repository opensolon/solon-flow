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
import org.noear.solon.core.util.Assert;
import org.noear.solon.lang.Preview;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流交换器，表示一个流在一次运行时的可交换数据和状态（对内，不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.5")
public class FlowExchanger {
    //当前流程图
    private transient final Graph graph;
    //当前流程引擎
    private transient final FlowEngine engine;
    //当前流驱动
    private transient final FlowDriver driver;
    //当前流程上下文
    private transient final FlowContextInternal context;
    //执行步进
    private transient final int steps;
    private transient final AtomicInteger stepCount;

    //执行时临时存放器
    private transient final Temporary temporary = new Temporary();
    //执行时分支阻断（可选）
    private transient volatile boolean interrupted = false;
    //执行时流程停止（可选）
    private transient volatile boolean stopped = false;
    //执行恢复中
    private transient volatile boolean reverting = true;

    public FlowExchanger(Graph graph, FlowEngine engine, FlowDriver driver, FlowContext context, int steps, AtomicInteger stepCount) {
        Assert.notNull(engine, "The engine is null");
        Assert.notNull(driver, "The driver is null");
        Assert.notNull(context, "The context is null");

        this.graph = graph;
        this.engine = engine;
        this.driver = driver;
        this.context = (FlowContextInternal) context;
        this.steps = steps;
        this.stepCount = stepCount;
    }

    /**
     * 浅度复制
     */
    public FlowExchanger copy(Graph graphNew) {
        return new FlowExchanger(graphNew, engine, driver, context, steps, stepCount);
    }

    /**
     * 浅度复制
     */
    public FlowExchanger copy(Graph graphNew, FlowContext contextNew) {
        return new FlowExchanger(graphNew, engine, driver, contextNew, steps, stepCount);
    }

    public Graph graph() {
        return graph;
    }

    /**
     * 当前流程引擎
     */
    public FlowEngine engine() {
        return engine;
    }

    /**
     * 当前驱动
     */
    public FlowDriver driver() {
        return driver;
    }

    /**
     * 当前上下文
     */
    public FlowContextInternal context() {
        return context;
    }

    /**
     * 临时存放器
     */
    public Temporary temporary() {
        return temporary;
    }

    /// ///////////////////////////

    /**
     * 记录节点
     */
    public void recordNode(Graph graph, Node node) {
        context.trace().recordNode(graph, node);
    }

    /**
     * 运行图
     *
     * @param graph 图
     */
    public void runGraph(Graph graph) {
        //回退节点走的步数（不然子图，会少一步）
        prveSetp();

        engine.eval(graph, copy(graph));

        if (isStopped() == false) {
            //如果没停止，则检查子图是否已结束
            if (context.trace().isEnd(graph.getId()) == false) {
                //子图没结束，则当前分支中断
                interrupt();
            }
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
            engine().getDriver(node.getGraph()).handleTask(this, new TaskDesc(node, description));
        } catch (FlowException e) {
            throw e;
        } catch (Throwable e) {
            throw new FlowException("The task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 运行脚本
     *
     * @param script 脚本
     */
    public Object runScript(String script) throws FlowException {
        //按脚本运行
        return Scripts.eval(script, context().model());
    }

    /// ///////////////////////////


    /**
     * 获取执行步数
     */
    public int getSteps() {
        return steps;
    }

    /**
     * 上一步（有时要回退）
     */
    public void prveSetp() {
        if (steps < 0) {
            return;
        } else {
            stepCount.decrementAndGet();
        }
    }

    /**
     * 下一步
     */
    public boolean nextSetp(Node node) {
        if (steps < 0) {
            return true;
        } else {
            return stepCount.incrementAndGet() <= steps;
        }
    }

    /**
     * 是否已停止（用于内部控制）
     */
    public boolean isStopped() {
        // context.isStopped() 每次 flow.eval 时会重置，但是执行内是有效的（可支持跨图、跨引擎传递）
        return stopped || context.isStopped();
    }

    /**
     * 停止（整个流程不再后流）
     */
    public void stop() {
        stopped = true;
        context.stopped(true);
    }

    /**
     * 是否已阻断（用于内部控制）
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

    /**
     * 是否恢复中（恢复到起始节点）
     */
    public boolean isReverting() {
        return reverting;
    }

    /**
     * 恢复状态重置
     */
    public FlowExchanger reverting(boolean reverting) {
        this.reverting = reverting;
        return this;
    }
}