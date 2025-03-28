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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流驱动器基类（方便定制）
 *
 * @author noear
 * @since 3.1
 */
public abstract class AbstractFlowDriver implements FlowDriver {
    static final Logger log = LoggerFactory.getLogger(AbstractFlowDriver.class);

    /**
     * 获取脚本评估器
     */
    protected abstract Evaluation getEvaluation();

    /**
     * 获取组件容器
     */
    protected abstract Container getContainer();

    /**
     * 是否为组件
     */
    protected boolean isChain(String description) {
        return description.startsWith("#");
    }

    /**
     * 是否为组件
     */
    protected boolean isComponent(String description) {
        return description.startsWith("@");
    }

    /// //////////////

    @Override
    public void onNodeStart(FlowContext context, Node node) {
        log.debug("on-node-start: chain={}, node={}", node.getChain().getId(), node);
    }

    @Override
    public void onNodeEnd(FlowContext context, Node node) {
        log.debug("on-node-end: chain={}, node={}", node.getChain().getId(), node);
    }

    /// //////////////

    @Override
    public boolean handleTest(FlowContext context, Condition condition) throws Throwable {
        //（不需要检测是否为空，引擎会把空条件作为默认，不会再传入）

        //如果 condition.description 有加密，可以转码后传入
        return handleTestDo(context, condition, condition.getDescription());
    }

    protected boolean handleTestDo(FlowContext context, Condition condition, String description) throws Throwable {
        //按脚本运行
        return tryAsScriptTest(context, condition, description);
    }

    protected boolean tryAsScriptTest(FlowContext context, Condition condition, String description) throws Throwable {
        return getEvaluation().runTest(context, description);
    }

    /// //////////////

    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        //默认过滤空任务（活动节点可能没有配置任务）
        if (Utils.isEmpty(task.getDescription())) {
            return;
        }

        //如果 task.description 有加密，可以转码后传入
        handleTaskDo(context, task, task.getDescription());
    }

    protected void handleTaskDo(FlowContext context, Task task, String description) throws Throwable {
        if (isChain(description)) {
            //如果跨链调用
            tryAsChainTask(context, task, description);
            return;
        }

        if (isComponent(description)) {
            //如果用组件运行
            tryAsComponentTask(context, task, description);
            return;
        }

        //默认按脚本运行
        tryAsScriptTask(context, task, description);
    }

    /**
     * 尝试如果是链则运行
     */
    protected void tryAsChainTask(FlowContext context, Task task, String description) throws Throwable {
        //调用其它链
        String chainId = description.substring(1);
        context.engine().eval(chainId, context);
    }

    /**
     * 尝试如果是组件则运行
     */
    protected void tryAsComponentTask(FlowContext context, Task task, String description) throws Throwable {
        //按组件运行
        String beanName = description.substring(1);
        Object component = getContainer().getComponent(beanName);

        if (component == null) {
            throw new IllegalStateException("The task component '" + beanName + "' not exist");
        } else if (component instanceof TaskComponent == false) {
            throw new IllegalStateException("The component '" + beanName + "' is not TaskComponent");
        } else {
            ((TaskComponent) component).run(context, task.getNode());
        }
    }

    /**
     * 尝试作为脚本运行
     */
    protected void tryAsScriptTask(FlowContext context, Task task, String description) throws Throwable {
        //按脚本运行
        try {
            context.put("node", task.getNode());

            getEvaluation().runTask(context, description);
        } finally {
            context.remove("node");
        }
    }
}