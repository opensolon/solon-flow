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
package org.noear.solon.flow.workflow.controller;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.StateController;

/**
 * 不阻塞（全自动前进）状态控制器
 *
 * @author noear
 * @since 3.5
 */
public class NotBlockStateController implements StateController {
    /**
     * 是否可操作的
     */
    @Override
    public boolean isOperatable(FlowContext context, Node node) {
        return true;
    }

    @Override
    public boolean isAutoForward(FlowContext context, Node node) {
        return true;
    }
}