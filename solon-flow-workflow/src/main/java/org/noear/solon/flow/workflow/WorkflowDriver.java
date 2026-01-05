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

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流驱动器实现
 *
 * @author noear
 * @since 3.8
 */
@Preview("3.8")
public class WorkflowDriver implements FlowDriver {
    public static final String KEY_ACTIVITY_NODE = "ACTIVITY_NODE";
    public static final String KEY_ACTIVITY_LIST = "ACTIVITY_LIST";
    public static final String KEY_ACTIVITY_LIST_GET = "ACTIVITY_LIST_GET";

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
                    exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.WAITING);
                    }
                } else {
                    //没有中断或停止，表示已完成

                    //记录当前流程节点（用于展示）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.COMPLETED);
                    exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);

                    //添加状态
                    stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.COMPLETED);
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task task = new Task(exchanger, taskDesc.getNode(), TaskState.TERMINATED);
                exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);

                //终止
                exchanger.stop();
            } else if (state == TaskState.COMPLETED) {
                //完成
                //Task task = new Task(exchanger, task.getNode(), StateType.COMPLETED);
                //exchanger.temporary().vars().put(StateResult.KEY_ACTIVITY_NODE, task);
            }
        } else {
            //控制前进
            TaskState state = stateRepository.stateGet(exchanger.context(), taskDesc.getNode());
            List<Task> nodeList = (List<Task>) exchanger.temporary().vars().computeIfAbsent(KEY_ACTIVITY_LIST, k -> new ArrayList<>());
            boolean nodeListGet = (boolean) exchanger.temporary().vars().getOrDefault(KEY_ACTIVITY_LIST_GET, false);

            if (state == TaskState.UNKNOWN || state == TaskState.WAITING) {
                //检查是否为当前用户的任务
                if (stateController.isOperatable(exchanger.context(), taskDesc.getNode())) {
                    //记录当前流程节点（用于展示）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.WAITING);
                    exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);
                    nodeList.add(task);

                    //添加状态
                    if (state != TaskState.WAITING) {
                        stateRepository.statePut(exchanger.context(), taskDesc.getNode(), TaskState.WAITING);
                    }

                    if (nodeListGet) {
                        exchanger.interrupt();
                    } else {
                        exchanger.stop();
                    }
                } else {
                    //阻断当前分支（等待别的用户办理）
                    Task task = new Task(exchanger, taskDesc.getNode(), TaskState.UNKNOWN);
                    exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);
                    nodeList.add(task);

                    exchanger.interrupt();
                }
            } else if (state == TaskState.TERMINATED) {
                //终止
                Task task = new Task(exchanger, taskDesc.getNode(), TaskState.TERMINATED);
                exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);
                nodeList.add(task);

                if (nodeListGet) {
                    exchanger.interrupt();
                } else {
                    exchanger.stop();
                }
            } else if (state == TaskState.COMPLETED) {
                //完成
                //Task task = new Task(exchanger, task.getNode(), TaskState.COMPLETED);
                //exchanger.temporary().vars().put(KEY_ACTIVITY_NODE, task);
            }
        }
    }

    @Override
    public void postHandleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        driver.postHandleTask(exchanger, task);
    }
}