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
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.Node;
import org.noear.solon.lang.Preview;

/**
 * 状态结果
 *
 * @author noear
 * @since 3.7
 */
public interface StateResult {
    String KEY_ACTIVITY_NODE = "ACTIVITY_NODE";
    String KEY_ACTIVITY_LIST = "ACTIVITY_LIST";
    String KEY_ACTIVITY_LIST_GET = "ACTIVITY_LIST_GET";


    /**
     * 运行当前节点任务（如果有？）
     *
     * @param context 上下文
     */
    @Preview("3.4")
    void runTask(FlowContext context) throws FlowException;

    /**
     * 节点
     */
    Node getNode();

    /**
     * 状态
     */
    StateType getState();
}