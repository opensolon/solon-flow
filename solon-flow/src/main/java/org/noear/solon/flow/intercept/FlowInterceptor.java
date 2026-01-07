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
package org.noear.solon.flow.intercept;

import org.noear.solon.flow.*;
import org.noear.solon.lang.Preview;

/**
 * 流拦截器
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 * @since 3.7
 */
@Preview("3.1")
public interface FlowInterceptor {
    /**
     * 拦截流程执行, eval(graph)
     *
     * @param invocation 调用者
     * @see org.noear.solon.flow.FlowEngine#eval(Graph, FlowExchanger)
     */
    default void interceptFlow(FlowInvocation invocation) throws FlowException {
        invocation.invoke();
    }

    /**
     * 节点运行开始时
     *
     * @since 3.4
     */
    default void onNodeStart(FlowContext context, Node node) {

    }

    /**
     * 节点运行结束时
     *
     * @since 3.4
     */
    default void onNodeEnd(FlowContext context, Node node) {

    }
}