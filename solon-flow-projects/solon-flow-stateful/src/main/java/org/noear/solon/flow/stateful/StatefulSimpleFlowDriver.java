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
import org.noear.solon.flow.stateful.operator.BlockStateOperator;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

import java.util.ArrayList;
import java.util.List;

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
        this.stateOperator = (stateOperator == null ? new BlockStateOperator() : stateOperator);
    }

    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        String instanceId = context.getInstanceId();

        //有实例id，且没有自动提交
        if (Utils.isNotEmpty(instanceId)) {
            if (stateOperator.isAutoForward(context, task.getNode())) {
                //自动前进
                NodeState nodeState = getStateRepository().getState(context, task.getNode());
                if (nodeState == NodeState.UNKNOWN || nodeState == NodeState.WAITING) {
                    //添加状态
                    stateRepository.putState(context, task.getNode(), NodeState.COMPLETED);

                    //发送提交变更事件
                    stateRepository.onPostActivityState(context, task.getNode(), NodeState.COMPLETED);

                    //确保任务只被执行一次
                    postHandleTask(context, task);
                }
            } else {
                //控制前进
                NodeState nodeState = getStateRepository().getState(context, task.getNode());
                List<StatefulNode> nodeList = context.computeIfAbsent(StatefulNode.KEY_ACTIVITY_LIST, k -> new ArrayList<>());
                boolean nodeListGet = context.getOrDefault(StatefulNode.KEY_ACTIVITY_LIST_GET, false);

                if (nodeState == NodeState.UNKNOWN) {
                    //检查是否为当前用户的任务
                    if (stateOperator.isOperatable(context, task.getNode())) {
                        //记录当前流程节点（用于展示）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), NodeState.WAITING);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        if (nodeListGet) {
                            context.interrupt();
                        } else {
                            context.stop();
                        }
                    } else {
                        //阻断当前分支（等待别的用户办理）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), NodeState.UNKNOWN);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        context.interrupt();
                    }
                } else if (nodeState == NodeState.WAITING) {
                    //检查是否为当前用户的任务
                    if (stateOperator.isOperatable(context, task.getNode())) {
                        //记录当前流程节点（用于展示）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), nodeState);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode); //说明之前没有结办
                        nodeList.add(statefulNode); //同时添加到列表

                        if (nodeListGet) {
                            context.interrupt();
                        } else {
                            context.stop();
                        }
                    } else {
                        //阻断当前分支（等待别的用户办理）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), NodeState.UNKNOWN);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode); //同时添加到列表

                        context.interrupt();
                    }
                }
            }
        } else {
            //提交处理任务
            postHandleTask(context, task);
        }
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