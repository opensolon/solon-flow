package org.noear.solon.flow.workflow;

import org.noear.solon.flow.FlowDriver;

/**
 *
 * @author noear 2025/12/20 created
 *
 */
public interface WorkflowDriver extends FlowDriver {
    /**
     * 获取状态控制器
     */
    StateController getStateController();

    /**
     * 获取状态仓库
     */
    StateRepository getStateRepository();
}
