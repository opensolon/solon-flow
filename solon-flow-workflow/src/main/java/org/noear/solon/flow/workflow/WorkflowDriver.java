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
import org.noear.solon.lang.Preview;

/**
 * 工作流驱动器实现
 *
 * @author noear
 * @since 3.8
 */
@Preview("3.8")
public class WorkflowDriver implements FlowDriver {
    private final FlowDriver driver;
    private final StateController stateController;
    private final StateRepository stateRepository;

    public WorkflowDriver(FlowDriver driver, StateController stateController, StateRepository stateRepository) {
        this.driver = driver;
        this.stateController = stateController;
        this.stateRepository = stateRepository;
    }

    /// ////////////////////////////

    @Override
    public void onNodeStart(FlowExchanger exchanger, Node node) {
        driver.onNodeStart(exchanger, node);
    }

    @Override
    public void onNodeEnd(FlowExchanger exchanger, Node node) {
        driver.onNodeEnd(exchanger, node);

        if (node.getType() == NodeType.END) {
            WorkflowIntent intent =  exchanger.context().getAs(WorkflowIntent.INTENT_KEY);
            if(intent == null){
                return;
            }

            //如果结束了，就没有任务了
            intent.task = null;
        }
    }

    @Override
    public boolean handleCondition(FlowExchanger exchanger, ConditionDesc condition) throws Throwable {
        return driver.handleCondition(exchanger, condition);
    }

    /**
     * 处理任务
     *
     * @param exchanger 流交换器
     * @param taskDesc      任务
     */
    @Override
    public void handleTask(FlowExchanger exchanger, TaskDesc taskDesc) throws Throwable {
        WorkflowIntent intent =  exchanger.context().getAs(WorkflowIntent.INTENT_KEY);
        if(intent == null){
            intent = new WorkflowIntent(WorkflowIntent.IntentType.UNKNOWN);
        }

        if (stateController.isAutoForward(exchanger.context(), taskDesc.getNode())) {
            //自动前进
            TaskState state = stateRepository.stateGet(exchanger.context(), taskDesc.getNode());
            if (state == TaskState.UNKNOWN || state == TaskState.WAITING) {
                //确保任务只被执行一次
                postHandleTask(exchanger, taskDesc);

                if ((exchanger.isStopped() || exchanger.isInterrupted())) {
                    //中断或停止，表示处理中

                    //记录当前流程节点（用于展示）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.WAITING);
                    intent.task = task;

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.WAITING);
                    }
                } else {
                    //没有中断或停止，表示已完成

                    //记录当前流程节点（用于展示）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.COMPLETED);
                    intent.task = task;

                    //添加状态
                    stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.COMPLETED);
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task task = new Task(exchanger, taskDesc.getNode(), TaskState.TERMINATED);
                intent.task = task;

                //终止
                exchanger.stop();
            } else if (state == TaskState.COMPLETED) {
                //完成，则跳过
            }
        } else {
            //控制前进
            TaskState state = stateRepository.stateGet(exchanger.context(), taskDesc.getNode());
            if (state == TaskState.UNKNOWN || state == TaskState.WAITING) {
                //检查是否为当前用户的任务
                if (stateController.isOperatable(exchanger.context(), taskDesc.getNode())) {
                    //记录当前流程节点（用于展示）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.WAITING);
                    intent.task = task;
                    intent.nextTasks.add(task);

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.WAITING);
                    }

                    if (intent.type == WorkflowIntent.IntentType.GET_NEXT_TASKS) {
                        exchanger.interrupt();
                    } else {
                        exchanger.stop();
                    }
                } else {
                    //没有权限（不输出 task）。阻断当前分支（等待别的用户办理）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.UNKNOWN);
                    intent.nextTasks.add(task);

                    exchanger.interrupt();
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task task = new Task(exchanger, taskDesc.getNode(), TaskState.TERMINATED);
                intent.task = task;
                intent.nextTasks.add(task);

                if (intent.type == WorkflowIntent.IntentType.GET_NEXT_TASKS) {
                    exchanger.interrupt();
                } else {
                    exchanger.stop();
                }
            } else if (state == TaskState.COMPLETED) {
                //完成，则跳过
            }
        }
    }

    @Override
    public void postHandleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        driver.postHandleTask(exchanger, task);
    }
}