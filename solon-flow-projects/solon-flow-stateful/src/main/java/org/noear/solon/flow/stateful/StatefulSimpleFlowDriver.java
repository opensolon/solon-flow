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
package org.noear.solon.flow.stateful;

import org.noear.solon.flow.Container;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * 有状态的简单流驱动器
 *
 * @author noear
 * @since 3.1
 */
public class StatefulSimpleFlowDriver extends SimpleFlowDriver {
    private final FlowStateRepository stateRepository;

    public FlowStateRepository getStateRepository() {
        return stateRepository;
    }

    public StatefulSimpleFlowDriver() {
        this(null);
    }

    public StatefulSimpleFlowDriver(FlowStateRepository stateRepository) {
        super();
        this.stateRepository = (stateRepository == null ? new SimpleFlowStateRepository() : stateRepository);
    }

    public StatefulSimpleFlowDriver(FlowStateRepository stateRepository, Container container) {
        super(container);
        this.stateRepository = (stateRepository == null ? new SimpleFlowStateRepository() : stateRepository);
    }

    public StatefulSimpleFlowDriver(FlowStateRepository stateRepository, Evaluation evaluation) {
        super(evaluation);
        this.stateRepository = (stateRepository == null ? new SimpleFlowStateRepository() : stateRepository);
    }

    public StatefulSimpleFlowDriver(FlowStateRepository stateRepository, Evaluation evaluation, Container container) {
        super(evaluation, container);
        this.stateRepository = (stateRepository == null ? new SimpleFlowStateRepository() : stateRepository);
    }

    @Override
    public void handleTask(FlowContext context0, Task task) throws Throwable {
        StatefulFlowContext context = (StatefulFlowContext) context0;
        int nodeState = getStateRepository().getState(
                context,
                task.node().chain().id(),
                task.node().id());

        if (nodeState == NodeStates.UNDEFINED) {
            //检查是否为当前用户的任务
            if (isMyTask(context, task)) {
                //记录当前流程节点（用于展示）
                context.setTaskNode(new StatefulNode(task.node(), nodeState));
                //停止流程
                context.stop();
                //设置状态为待办
                getStateRepository().postState(
                        context,
                        task.node().chain().id(),
                        task.node().id(),
                        NodeStates.WAIT,
                        context.engine());

            } else {
                //阻断当前分支（等待别的用户办理）
                context.interrupt();
            }
        } else if (nodeState == NodeStates.WAIT) {
            //检查是否为当前用户的任务
            if (isMyTask(context, task)) {
                //记录当前流程节点（用于展示）
                context.setTaskNode(new StatefulNode(task.node(), nodeState)); //说明之前没有结办
                //停止流程
                context.stop();
            } else {
                //阻断当前分支（等待别的用户办理）
                context.interrupt();
            }
        } else {
            //已办
            super.handleTask(context, task);
        }
    }

    protected boolean isMyTask(StatefulFlowContext context, Task task) {
        return context.getUserId().equals(task.node().meta("userId")) ||
                context.getRoleId().equals(task.node().meta("roleId"));
    }
}