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

import org.noear.solon.lang.Internal;
import org.noear.solon.lang.Nullable;
import org.noear.solon.lang.Preview;

/**
 * 流上下文内部增强接口
 *
 * @author noear
 * @since 3.8.1
 */
public interface FlowContextInternal extends FlowContext {
    /**
     * 配置交换器
     *
     * @since 3.8
     */
    @Internal
    @Preview("3.8")
    void exchanger(FlowExchanger exchanger);


    /**
     * 交换器
     */
    @Internal
    @Nullable
    FlowExchanger exchanger();


    /**
     * 记录最后运行的节点
     */
    @Preview("3.8")
    void lastNode(Node node);
}
