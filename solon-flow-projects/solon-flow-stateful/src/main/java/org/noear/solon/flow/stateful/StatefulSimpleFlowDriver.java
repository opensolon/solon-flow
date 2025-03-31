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

import org.noear.solon.Utils;
import org.noear.solon.flow.Container;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.stateful.operator.SimpleStateOperator;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * 有状态的简单流驱动器
 *
 * @author noear
 * @since 3.1
 */
public class StatefulSimpleFlowDriver extends SimpleFlowDriver {
    private final StateRepository stateRepository;
    private final StateOperator stateOperator;

    public StateRepository getStateRepository() {
        return stateRepository;
    }

    public StateOperator getStateOperator() {
        return stateOperator;
    }

    public StatefulSimpleFlowDriver(StateRepository stateRepository, StateOperator stateOperator, Evaluation evaluation, Container container) {
        super(evaluation, container);
        this.stateRepository = (stateRepository == null ? new InMemoryStateRepository() : stateRepository);
        this.stateOperator = (stateOperator == null ? new SimpleStateOperator() : stateOperator);
    }


    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        String instanceId = context.getInstanceId();

        if (Utils.isNotEmpty(instanceId)) {
            int nodeState = getStateRepository().getState(
                    context,
                    task.getNode());

            if (nodeState == NodeStates.UNDEFINED) {
                //检查是否为当前用户的任务
                if (stateOperator.isOperatable(context, task.getNode())) {
                    //记录当前流程节点（用于展示）
                    context.put(StatefulNode.KEY_ACTIVITY_NODE, new StatefulNode(task.getNode(), NodeStates.WAIT));
                    //停止流程
                    context.stop();
                    //设置状态为待办
                    ((StatefulFlowEngine) context.engine()).postNodeState(
                            context,
                            task.getNode(),
                            NodeStates.WAIT);

                } else {
                    //阻断当前分支（等待别的用户办理）
                    context.put(StatefulNode.KEY_ACTIVITY_NODE, new StatefulNode(task.getNode(), NodeStates.UNDEFINED));
                    context.interrupt();
                }
            } else if (nodeState == NodeStates.WAIT) {
                //检查是否为当前用户的任务
                if (stateOperator.isOperatable(context, task.getNode())) {
                    //记录当前流程节点（用于展示）
                    context.put(StatefulNode.KEY_ACTIVITY_NODE, new StatefulNode(task.getNode(), nodeState)); //说明之前没有结办
                    //停止流程
                    context.stop();
                } else {
                    //阻断当前分支（等待别的用户办理）
                    context.put(StatefulNode.KEY_ACTIVITY_NODE, new StatefulNode(task.getNode(), NodeStates.UNDEFINED));
                    context.interrupt();
                }
            }

            return;
        }


        //提交处理任务
        postHandleTask(context, task);
    }

    /**
     * 提交处理任务
     */
    protected void postHandleTask(FlowContext context, Task task) throws Throwable {
        super.handleTask(context, task);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StateRepository stateRepository;
        private StateOperator stateOperator;
        private Evaluation evaluation;
        private Container container;

        /**
         * 设置状态仓库
         */
        public Builder stateRepository(StateRepository stateRepository) {
            this.stateRepository = stateRepository;
            return this;
        }

        /**
         * 设置状态操作员
         */
        public Builder stateOperator(StateOperator stateOperator) {
            this.stateOperator = stateOperator;
            return this;
        }

        /**
         * 设置评估器
         */
        public Builder evaluation(Evaluation evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        /**
         * 设置容器
         */
        public Builder container(Container container) {
            this.container = container;
            return this;
        }

        /**
         * 构建
         */
        public StatefulSimpleFlowDriver build() {
            return new StatefulSimpleFlowDriver(
                    stateRepository,
                    stateOperator,
                    evaluation,
                    container);
        }
    }
}