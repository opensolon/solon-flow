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

import org.noear.solon.core.util.RankEntity;
import org.noear.solon.flow.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 流调用者
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 * @since 3.7
 */
public class FlowInvocation {
    private final FlowExchanger exchanger;
    private final FlowOptions options;
    private final Node startNode;

    private final List<RankEntity<FlowInterceptor>> interceptorList;
    private final BiConsumer<FlowInvocation, FlowOptions> lastHandler;
    private int index;

    public FlowInvocation(FlowExchanger exchanger, FlowOptions options, Node startNode, BiConsumer<FlowInvocation, FlowOptions> lastHandler) {
        this.exchanger = exchanger;
        this.options = options;
        this.startNode = startNode;

        this.interceptorList = options.getInterceptorList();
        this.lastHandler = lastHandler;

        this.index = 0;
    }

    /**
     * 获取交换器
     */
    public FlowExchanger getExchanger() {
        return exchanger;
    }


    /**
     * 获取上下文
     */
    public FlowContext getContext() {
        return exchanger.context();
    }

    /**
     * 获取图
     *
     * @since 3.8
     */
    public Graph getGraph() {
        return startNode.getGraph();
    }

    /**
     * 获取开始节点
     */
    public Node getStartNode() {
        return startNode;
    }

    /**
     * 调用
     */
    public void invoke() throws FlowException {
        if (index < interceptorList.size()) {
            interceptorList.get(index++).target.interceptFlow(this);
        } else {
            lastHandler.accept(this, options);
        }
    }
}