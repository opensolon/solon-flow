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
package org.noear.solon.flow;

import org.noear.solon.lang.Preview;

/**
 * 脚本执行器
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 */
@Preview("3.1")
public interface Evaluation {
    /**
     * 运行条件
     *
     * @param context 流上下文
     * @param code    条件代码
     */
    boolean runCondition(FlowContext context, String code) throws Throwable;

    /**
     * 运行条件
     *
     * @param context 流上下文
     * @param code    条件代码
     * @deprecated 3.7.4
     */
    @Deprecated
    default boolean runTest(FlowContext context, String code) throws Throwable {
        return runCondition(context, code);
    }

    /**
     * 运行任务
     *
     * @param context 流上下文
     * @param code    任务代码
     */
    void runTask(FlowContext context, String code) throws Throwable;
}