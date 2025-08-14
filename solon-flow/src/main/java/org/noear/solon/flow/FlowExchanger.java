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

/**
 * 流交换器，表示一个流在一次运行时的可交换数据和状态（对内，不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.5")
public class FlowExchanger {
    //当前流程上下文
    private transient final FlowContext context;
    //当前流程引擎
    protected transient FlowEngine engine;

    //执行时临时存放器
    private transient final Temporary temporary = new Temporary();
    //执行时分支阻断（可选）
    private transient volatile boolean interrupted = false;
    //执行时流程停止（可选）
    private transient volatile boolean stopped = false;

    public FlowExchanger(FlowContext context) {
        this.context = context;
    }

    /**
     * 当前上下文
     *
     */
    public FlowContext context() {
        return context;
    }

    /**
     * 当前流程引擎
     */
    public FlowEngine engine() {
        return engine;
    }

    /**
     * 临时存放器
     */
    public Temporary temporary() {
        return temporary;
    }

    /// ///////////////////////////

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
    public Object runScript(String script) throws FlowException {
        //按脚本运行
        return Scripts.eval(script, context().model());
    }


    /// ///////////////////////////

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
}