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

import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.Node;
import org.noear.solon.lang.Preview;

/**
 * 链拦截器
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 */
@Preview("3.1")
public interface ChainInterceptor {
    /**
     * 拦截链执行
     *
     * @param invocation 链调用者
     */
    void doIntercept(ChainInvocation invocation) throws FlowException;

    /**
     * 节点运行开始时
     *
     * @since 3.4
     */
    default void onNodeStart(FlowExchanger exchanger, Node node) {

    }

    /**
     * 节点运行结束时
     *
     * @since 3.4
     */
    default void onNodeEnd(FlowExchanger exchanger, Node node) {

    }
}