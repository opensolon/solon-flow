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
package org.noear.solon.flow.workflow;

import org.noear.solon.core.util.Assert;
import org.noear.solon.flow.*;
import org.noear.solon.lang.NonSerializable;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

/**
 * 任务
 *
 * @author noear
 * @since 3.1
 */
public class Task implements NonSerializable {
    private transient final FlowExchanger exchanger;
    private transient final Graph rootGraph;
    private transient final Node node;
    private transient final TaskState state;

    public Task(FlowExchanger exchanger, Graph rootGraph, Node node, TaskState state) {
        this.exchanger = exchanger;
        this.rootGraph = rootGraph;
        this.node = node;
        this.state = state;
    }

    /**
     * 运行当前任务
     *
     * @param context 上下文
     */
    @Preview("3.4")
    public void run(FlowContext context) throws FlowException {
        Assert.notNull(node, "node is null");

        try {
            exchanger.driver().handleTask(exchanger.copy(node.getGraph(), context).reverting(false), node.getTask());
        } catch (FlowException e) {
            throw e;
        } catch (Throwable e) {
            throw new FlowException("The task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 最后运行记录
     */
    public @Nullable NodeRecord lastRecord() {
        //它是动态的（不适合固化为字段）
        return exchanger.context().lastRecord();
    }

    /**
     * 是否为最后节点
     */
    public boolean isEnd() {
        NodeRecord lastRecord = exchanger.context().lastRecord();
        if (lastRecord == null) {
            return false;
        }

        return lastRecord.isEnd();
    }

    public Graph getRootGraph() {
        return rootGraph;
    }

    /**
     * 节点
     */
    public Node getNode() {
        return node;
    }

    /**
     * 节点Id
     */
    public String getNodeId() {
        if (node == null) {
            return null;
        } else {
            return node.getId();
        }
    }

    /**
     * 状态
     */
    public TaskState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Task{" +
                "state=" + state +
                ", node=" + node +
                '}';
    }
}