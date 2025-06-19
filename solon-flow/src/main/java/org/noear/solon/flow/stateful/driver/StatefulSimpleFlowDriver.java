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
package org.noear.solon.flow.stateful.driver;

import org.noear.solon.Utils;
import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.lang.Preview;

import java.util.ArrayList;
import java.util.List;

/**
 * 有状态的简单流驱动器
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulSimpleFlowDriver extends SimpleFlowDriver implements FlowDriver, StatefulFlowDriver {
    private final StateRepository stateRepository;
    private final StateController stateController;

    public StatefulSimpleFlowDriver(StateRepository stateRepository, StateController stateController, Evaluation evaluation, Container container) {
        super(evaluation, container);
        this.stateRepository = (stateRepository == null ? new InMemoryStateRepository() : stateRepository);
        this.stateController = (stateController == null ? new BlockStateController() : stateController);
    }

    /**
     * 获取状态仓库
     */
    @Override
    public StateRepository getStateRepository() {
        return stateRepository;
    }

    /**
     * 获取状态控制器
     */
    @Override
    public StateController getStateController() {
        return stateController;
    }

    /**
     * 提交处理任务
     *
     * @param context 流上下文
     * @param task    任务
     */
    @Override
    public void postHandleTask(FlowContext context, Task task) throws Throwable {
        super.handleTask(context, task);
    }

    /**
     * 处理任务
     *
     * @param context 流上下文
     * @param task    任务
     */
    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        String instanceId = context.getInstanceId();

        if (Utils.isNotEmpty(instanceId)) {
            //有实例id，作有状态处理
            if (stateController.isAutoForward(context, task.getNode())) {
                //自动前进
                StateType state = getStateRepository().getState(context, task.getNode());
                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //添加状态
                    stateRepository.putState(context, task.getNode(), StateType.COMPLETED);

                    //发送提交变更事件
                    stateRepository.onPostState(context, task.getNode(), StateType.COMPLETED);

                    //确保任务只被执行一次
                    postHandleTask(context, task);
                } else if (state == StateType.TERMINATED) {
                    //终止
                    context.stop();
                }
            } else {
                //控制前进
                StateType state = getStateRepository().getState(context, task.getNode());
                List<StatefulNode> nodeList = context.computeIfAbsent(StatefulNode.KEY_ACTIVITY_LIST, k -> new ArrayList<>());
                boolean nodeListGet = context.getOrDefault(StatefulNode.KEY_ACTIVITY_LIST_GET, false);

                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //检查是否为当前用户的任务
                    if (stateController.isOperatable(context, task.getNode())) {
                        //记录当前流程节点（用于展示）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), StateType.WAITING);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        if (nodeListGet) {
                            context.interrupt();
                        } else {
                            context.stop();
                        }
                    } else {
                        //阻断当前分支（等待别的用户办理）
                        StatefulNode statefulNode = new StatefulNode(task.getNode(), StateType.UNKNOWN);
                        context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        context.interrupt();
                    }
                } else if (state == StateType.TERMINATED) {
                    //终止
                    StatefulNode statefulNode = new StatefulNode(task.getNode(), StateType.TERMINATED);
                    context.put(StatefulNode.KEY_ACTIVITY_NODE, statefulNode);
                    nodeList.add(statefulNode);

                    if (nodeListGet) {
                        context.interrupt();
                    } else {
                        context.stop();
                    }
                }
            }
        } else {
            //没有实例id，作无状态处理 //直接提交处理任务
            postHandleTask(context, task);
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StateRepository stateRepository;
        private StateController stateController;
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
         * 设置状态控制器
         */
        public Builder stateController(StateController stateController) {
            this.stateController = stateController;
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
                    stateController,
                    evaluation,
                    container);
        }
    }
}