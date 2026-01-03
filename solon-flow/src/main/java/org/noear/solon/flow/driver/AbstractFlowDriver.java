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

import org.noear.solon.Utils;
import org.noear.solon.flow.*;
import org.noear.solon.flow.container.SolonContainer;
import org.noear.solon.flow.evaluation.LiquorEvaluation;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 流驱动器基类（方便定制）
 *
 * @author noear
 * @since 3.1
 */
public abstract class AbstractFlowDriver implements FlowDriver {
    private final Evaluation evaluation;
    private final Container container;
    private final ExecutorService executor;

    /**
     * @param evaluation 脚本执行器
     * @param container  组件容器
     */
    public AbstractFlowDriver(Evaluation evaluation, Container container, ExecutorService executor) {
        this.evaluation = (evaluation == null ? new LiquorEvaluation() : evaluation);
        this.container = (container == null ? new SolonContainer() : container);
        this.executor = executor;
    }

    /**
     * 获取脚本执行器
     */
    protected Evaluation getEvaluation() {
        return evaluation;
    }

    /**
     * 获取组件容器
     */
    protected Container getContainer() {
        return container;
    }

    /**
     * 是否为图
     */
    protected boolean isGraph(String description) {
        return description.startsWith("#");
    }

    /**
     * 是否为组件
     */
    protected boolean isComponent(String description) {
        return description.startsWith("@");
    }

    /// //////////////

    public ExecutorService getExecutor() {
        return executor;
    }

    /// //////////////

    /**
     * 当节点开始（节点不是任务）
     */
    @Override
    public void onNodeStart(FlowExchanger exchanger, Node node) {

    }

    /**
     * 当节点结束
     */
    @Override
    public void onNodeEnd(FlowExchanger exchanger, Node node) {

    }

    /// //////////////

    /**
     * 处理条件
     */
    @Override
    public boolean handleCondition(FlowExchanger exchanger, ConditionDesc condition) throws Throwable {
        //（不需要检测是否为空，引擎会把空条件作为默认，不会再传入）

        //如果 condition.description 有加密，可以转码后传入
        return handleConditionDo(exchanger, condition, condition.getDescription());
    }

    protected boolean handleConditionDo(FlowExchanger exchanger, ConditionDesc condition, String description) throws Throwable {
        //按脚本运行
        if (condition.getComponent() != null) {
            return condition.getComponent().test(exchanger.context());
        }

        if (isComponent(condition.getDescription())) {
            return tryAsComponentCondition(exchanger, condition, description);
        } else {
            return tryAsScriptCondition(exchanger, condition, description);
        }
    }

    /**
     * 尝试作为组件条件运行
     */
    protected boolean tryAsComponentCondition(FlowExchanger exchanger, ConditionDesc condition, String description) throws Throwable {
        //按组件运行
        String beanName = description.substring(1);
        Object component = getContainer().getComponent(beanName);

        if (component == null) {
            throw new IllegalStateException("The condition component '" + beanName + "' not exist");
        } else if (component instanceof ConditionComponent == false) {
            throw new IllegalStateException("The component '" + beanName + "' is not ConditionComponent");
        } else {
            return ((ConditionComponent) component).test(exchanger.context());
        }
    }

    /**
     * 尝试作为脚本条件运行
     */
    protected boolean tryAsScriptCondition(FlowExchanger exchanger, ConditionDesc condition, String description) throws Throwable {
        return getEvaluation().runCondition(exchanger.context(), description);
    }

    /// //////////////

    /**
     * 提交处理任务
     */
    @Override
    public void postHandleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        //默认过滤空任务（活动节点可能没有配置任务）
        if (task.isEmpty()) {
            return;
        }

        //如果 task.description 有加密，可以转码后传入
        handleTaskDo(exchanger, task);
    }

    protected void handleTaskDo(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        try {
            if (task.getComponent() != null) {
                task.getComponent().run(exchanger.context(), task.getNode());
                return;
            }

            if (isGraph(task.getDescription())) {
                //如果跨图调用
                tryAsGraphTask(exchanger, task, task.getDescription());
                return;
            }

            if (isComponent(task.getDescription())) {
                //如果用组件运行
                tryAsComponentTask(exchanger, task, task.getDescription());
                return;
            }

            //默认按脚本运行
            tryAsScriptTask(exchanger, task, task.getDescription());
        } finally {
            //任务内部，可能会切换掉交换器
            exchanger.context().exchanger(exchanger);
        }
    }

    /**
     * 尝试作为子图任务运行
     */
    protected void tryAsGraphTask(FlowExchanger exchanger, TaskDesc task, String description) throws Throwable {
        //调用其它图
        String graphId = description.substring(1);
        Graph graph = exchanger.engine().getGraphOrThrow(graphId);

        //获取跟踪的最后节点
        exchanger.engine().eval(graph, exchanger.context().trace().lastNode(graph), -1, exchanger);
    }

    /**
     * 尝试作为组件任务运行
     */
    protected void tryAsComponentTask(FlowExchanger exchanger, TaskDesc task, String description) throws Throwable {
        //按组件运行
        String beanName = description.substring(1);
        Object component = getContainer().getComponent(beanName);

        if (component == null) {
            throw new IllegalStateException("The task component '" + beanName + "' not exist");
        } else if (component instanceof TaskComponent == false) {
            throw new IllegalStateException("The component '" + beanName + "' is not TaskComponent");
        } else {
            ((TaskComponent) component).run(exchanger.context(), task.getNode());
        }
    }

    /**
     * 尝试作为脚本任务运行
     */
    protected void tryAsScriptTask(FlowExchanger exchanger, TaskDesc task, String description) throws Throwable {
        //按脚本运行
        if (description.startsWith("$")) {
            String metaName = description.substring(1);
            description = (String) getDepthMeta(task.getNode().getGraph().getMetas(), metaName);

            if (Utils.isEmpty(description)) {
                throw new FlowException("Graph meta not found: " + metaName);
            }
        }


        try {
            //给脚本用
            exchanger.context().put(Node.TAG, task.getNode());

            getEvaluation().runTask(exchanger.context(), description);
        } finally {
            exchanger.context().remove(Node.TAG);
        }
    }

    /**
     * 获取深度元数据
     */
    protected Object getDepthMeta(Map metas, String key) {
        String[] fragments = key.split("\\.");
        Object rst = null;

        for (int i = 0, len = fragments.length; i < len; i++) {
            String key1 = fragments[i];
            if (i == 0) {
                rst = metas.get(key1);
            } else {
                rst = ((Map) rst).get(key1);
            }

            if (rst == null) {
                break;
            }
        }

        return rst;
    }
}