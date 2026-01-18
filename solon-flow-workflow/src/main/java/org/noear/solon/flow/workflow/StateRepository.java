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

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.lang.Preview;

import java.util.Map;

/**
 * 状态仓库
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public interface StateRepository {
    /**
     * 数据模型获取
     */
    default Map<String,Object> modelGet(FlowContext context, Node node) {
        return null;
    }

    /**
     * 状态获取
     */
    TaskState stateGet(FlowContext context, Node node);

    /**
     * 状态推入
     */
    void statePut(FlowContext context, Node node, TaskState state);

    /**
     * 状态移除
     */
    void stateRemove(FlowContext context, Node node);

    /**
     * 状态清空
     */
    void stateClear(FlowContext context);
}