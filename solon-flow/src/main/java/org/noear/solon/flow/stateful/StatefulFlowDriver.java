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

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowDriver;
import org.noear.solon.flow.Task;
import org.noear.solon.lang.Preview;

/**
 * 有状态的流驱动器
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public interface StatefulFlowDriver extends FlowDriver {
    /**
     * 获取状态仓库
     */
    StateRepository getStateRepository();

    /**
     * 获取状态控制器
     */
    StateController getStateController();

    /**
     * 提交处理任务
     *
     * @param context 流上下文
     * @param task    任务
     */
    void postHandleTask(FlowContext context, Task task) throws Throwable;
}