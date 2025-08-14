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

import org.noear.solon.flow.*;
import org.noear.solon.flow.Actuator;
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
public class StatefulFlowDriver extends AbstractFlowDriver implements FlowDriver {
    public StatefulFlowDriver() {
        this(null, null);
    }

    public StatefulFlowDriver(Actuator actuator) {
        super(actuator, null);
    }

    public StatefulFlowDriver(Container container) {
        super(null, container);
    }

    public StatefulFlowDriver(Actuator actuator, Container container) {
        super(actuator, container);
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
            if (exchanger.context().stateController().isAutoForward(exchanger.context(), task.getNode())) {
                //自动前进
                StateType state = exchanger.context().stateRepository().stateGet(exchanger.context(), task.getNode());
                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //添加状态
                    exchanger.context().stateRepository().statePut(exchanger.context(), task.getNode(), StateType.COMPLETED);

                    //确保任务只被执行一次
                    postHandleTask(exchanger, task);
                } else if (state == StateType.TERMINATED) {
                    //终止
                    exchanger.stop();
                }
            } else {
                //控制前进
                StateType state = exchanger.context().stateRepository().stateGet(exchanger.context(), task.getNode());
                List<StatefulTask> nodeList = (List<StatefulTask>) exchanger.temporary().vars().computeIfAbsent(StatefulTask.KEY_ACTIVITY_LIST, k -> new ArrayList<>());
                boolean nodeListGet = (boolean) exchanger.temporary().vars().getOrDefault(StatefulTask.KEY_ACTIVITY_LIST_GET, false);

                if (state == StateType.UNKNOWN || state == StateType.WAITING) {
                    //检查是否为当前用户的任务
                    if (exchanger.context().stateController().isOperatable(exchanger.context(), task.getNode())) {
                        //记录当前流程节点（用于展示）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.WAITING);
                        exchanger.temporary().vars().put(StatefulTask.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        if (nodeListGet) {
                            exchanger.interrupt();
                        } else {
                            exchanger.stop();
                        }
                    } else {
                        //阻断当前分支（等待别的用户办理）
                        StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.UNKNOWN);
                        exchanger.temporary().vars().put(StatefulTask.KEY_ACTIVITY_NODE, statefulNode);
                        nodeList.add(statefulNode);

                        exchanger.interrupt();
                    }
                } else if (state == StateType.TERMINATED) {
                    //终止
                    StatefulTask statefulNode = new StatefulTask(exchanger.engine(), task.getNode(), StateType.TERMINATED);
                    exchanger.temporary().vars().put(StatefulTask.KEY_ACTIVITY_NODE, statefulNode);
                    nodeList.add(statefulNode);

                    if (nodeListGet) {
                        exchanger.interrupt();
                    } else {
                        exchanger.stop();
                    }
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
        private Actuator actuator;
        private Container container;

        /**
         * 设置评估器
         */
        public Builder actuator(Actuator actuator) {
            this.actuator = actuator;
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
        public StatefulFlowDriver build() {
            return new StatefulFlowDriver(
                    actuator,
                    container);
        }
    }
}