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

import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.AbstractFlowDriver;
import org.noear.solon.lang.Preview;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流驱动器默认实现
 *
 * @author noear
 * @since 3.8
 */
@Preview("3.8")
public class WorkflowDriverDefault extends AbstractFlowDriver implements WorkflowDriver {
    private final StateController stateController;
    private final StateRepository stateRepository;

    public WorkflowDriverDefault(Evaluation evaluation, Container container, StateController stateController, StateRepository stateRepository) {
        super(evaluation, container);

        this.stateController = stateController;
        this.stateRepository = stateRepository;
    }

    public StateController getStateController() {
        return stateController;
    }

    public StateRepository getStateRepository() {
        return stateRepository;
    }

    /// ////////////////////////////

    /**
     * 处理任务
     *
     * @param exchanger 流交换器
     * @param task      任务
     */
    @Override
    public void handleTask(FlowExchanger exchanger, org.noear.solon.flow.Task task) throws Throwable {
        //有关态的
        if (stateController.isAutoForward(exchanger.context(), task.getNode())) {
            //自动前进
            TaskState state = stateRepository.stateGet(exchanger.context(), task.getNode());
            if (state == TaskState.UNKNOWN || state == TaskState.WAITING) {
                //确保任务只被执行一次
                postHandleTask(exchanger, task);

                if ((exchanger.isStopped() || exchanger.isInterrupted())) {
                    //中断或停止，表示处理中

                    //记录当前流程节点（用于展示）
                    Task statefulNode = new Task(exchanger, task.getNode(), TaskState.WAITING);
                    exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), task.getNode(), TaskState.WAITING);
                    }
                } else {
                    //没有中断或停止，表示已完成

                    //记录当前流程节点（用于展示）
                    Task statefulNode = new Task(exchanger, task.getNode(), TaskState.COMPLETED);
                    exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);

                    //添加状态
                    stateRepository.statePut(exchanger.context(), task.getNode(), TaskState.COMPLETED);
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task statefulNode = new Task(exchanger, task.getNode(), TaskState.TERMINATED);
                exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);

                //终止
                exchanger.stop();
            } else if (state == TaskState.COMPLETED) {
                //完成
                //StatefulTask statefulNode = new StatefulTask(exchanger, task.getNode(), StateType.COMPLETED);
                //exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
            }
        } else {
            //控制前进
            TaskState state = stateRepository.stateGet(exchanger.context(), task.getNode());
            List<Task> nodeList = (List<Task>) exchanger.temporary().vars().computeIfAbsent(Task.KEY_ACTIVITY_LIST, k -> new ArrayList<>());
            boolean nodeListGet = (boolean) exchanger.temporary().vars().getOrDefault(Task.KEY_ACTIVITY_LIST_GET, false);

            if (state == TaskState.UNKNOWN || state == TaskState.WAITING) {
                //检查是否为当前用户的任务
                if (stateController.isOperatable(exchanger.context(), task.getNode())) {
                    //记录当前流程节点（用于展示）
                    Task statefulNode = new Task(exchanger, task.getNode(), TaskState.WAITING);
                    exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);
                    nodeList.add(statefulNode);

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), task.getNode(), TaskState.WAITING);
                    }

                    if (nodeListGet) {
                        exchanger.interrupt();
                    } else {
                        exchanger.stop();
                    }
                } else {
                    //阻断当前分支（等待别的用户办理）
                    Task statefulNode = new Task(exchanger, task.getNode(), TaskState.UNKNOWN);
                    exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);
                    nodeList.add(statefulNode);

                    exchanger.interrupt();
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task statefulNode = new Task(exchanger, task.getNode(), TaskState.TERMINATED);
                exchanger.temporary().vars().put(Task.KEY_ACTIVITY_NODE, statefulNode);
                nodeList.add(statefulNode);

                if (nodeListGet) {
                    exchanger.interrupt();
                } else {
                    exchanger.stop();
                }
            } else if (state == TaskState.COMPLETED) {
                //完成
                //StatefulTask statefulNode = new StatefulTask(exchanger, task.getNode(), StateType.COMPLETED);
                //exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, statefulNode);
            }
        }
    }
}