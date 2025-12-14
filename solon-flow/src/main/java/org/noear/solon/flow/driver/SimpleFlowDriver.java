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
package org.noear.solon.flow.driver;

import org.noear.solon.flow.*;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.stateful.StateResult;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.lang.Preview;

import java.util.ArrayList;
import java.util.List;

/**
 * 有状态的简单流驱动器（兼容无状态）
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 */
@Preview("3.1")
public class SimpleFlowDriver extends AbstractFlowDriver implements FlowDriver {
    public SimpleFlowDriver() {
        this(null, null);
    }

    public SimpleFlowDriver(Evaluation evaluation) {
        super(evaluation, null);
    }

    public SimpleFlowDriver(Container container) {
        super(null, container);
    }

    public SimpleFlowDriver(Evaluation evaluation, Container container) {
        super(evaluation, container);
    }

    /// ////////////////////////////

    /**
     * 处理任务
     *
     * @param exchanger 流交换器
     * @param task      任务
     */
    @Override
    public void handleTask(FlowExchanger exchanger, Task task) throws Throwable {
        if (exchanger.context().isStateful()) {
            //有关态的
            if (exchanger.context().statefulSupporter().isAutoForward(task.getNode())) {
                //自动前进
                StateType state = exchanger.context().statefulSupporter().stateGet(task.getNode());
                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //确保任务只被执行一次
                    postHandleTask(exchanger, task);

                    if ((exchanger.isStopped() || exchanger.isInterrupted())) {
                        //中断或停止，表示处理中

                        //记录当前流程节点（用于展示）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.WAITING);
                        exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);

                        //添加状态
                        if(state != StateType.WAITING) {
                            exchanger.context().statefulSupporter().statePut(task.getNode(), StateType.WAITING);
                        }
                    } else {
                        //没有中断或停止，表示已完成

                        //记录当前流程节点（用于展示）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.COMPLETED);
                        exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);

                        //添加状态
                        exchanger.context().statefulSupporter().statePut(task.getNode(), StateType.COMPLETED);
                    }
                } else if (state == StateType.TERMINATED) {
                    //终止
                    StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.TERMINATED);
                    exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);

                    //终止
                    exchanger.stop();
                } else if (state == StateType.COMPLETED) {
                    //完成
                    //StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.COMPLETED);
                    //exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
                }
            } else {
                //控制前进
                StateType state = exchanger.context().statefulSupporter().stateGet(task.getNode());
                List<StatefulTask> nodeList = (List<StatefulTask>) exchanger.temporary().vars().computeIfAbsent(StateResult.KEY_ACTIVITY_LIST, k -> new ArrayList<>());
                boolean nodeListGet = (boolean) exchanger.temporary().vars().getOrDefault(StateResult.KEY_ACTIVITY_LIST_GET, false);

                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //检查是否为当前用户的任务
                    if (exchanger.context().statefulSupporter().isOperatable(task.getNode())) {
                        //记录当前流程节点（用于展示）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.WAITING);
                        exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        //添加状态
                        if(state != StateType.WAITING) {
                            exchanger.context().statefulSupporter().statePut(task.getNode(), StateType.WAITING);
                        }

                        if (nodeListGet) {
                            exchanger.interrupt();
                        } else {
                            exchanger.stop();
                        }
                    } else {
                        //阻断当前分支（等待别的用户办理）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.UNKNOWN);
                        exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        exchanger.interrupt();
                    }
                } else if (state == StateType.TERMINATED) {
                    //终止
                    StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.TERMINATED);
                    exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
                    nodeList.add(statefulNode);

                    if (nodeListGet) {
                        exchanger.interrupt();
                    } else {
                        exchanger.stop();
                    }
                } else if (state == StateType.COMPLETED) {
                    //完成
                    //StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.COMPLETED);
                    //exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
                }
            }
        } else {
            //无状态的
            postHandleTask(exchanger, task);
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Evaluation evaluation;
        private Container container;

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
        public SimpleFlowDriver build() {
            return new SimpleFlowDriver(
                    evaluation,
                    container);
        }
    }
}