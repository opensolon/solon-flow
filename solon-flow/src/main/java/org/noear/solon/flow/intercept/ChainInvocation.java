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
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.FlowDriver;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.Node;

import java.util.List;
import java.util.function.Consumer;

/**
 * 链调用者
 *
 * @author noear
 * @since 3.1
 */
public class ChainInvocation {
    private final FlowDriver driver;
    private final FlowExchanger context;
    private final Node startNode;
    private final int evalDepth;

    private final List<RankEntity<ChainInterceptor>> interceptorList;
    private final Consumer<ChainInvocation> lastHandler;
    private int index;

    public ChainInvocation(FlowDriver driver, FlowExchanger context, Node startNode, int evalDepth, List<RankEntity<ChainInterceptor>> interceptorList, Consumer<ChainInvocation> lastHandler) {
        this.driver = driver;
        this.context = context;
        this.startNode = startNode;
        this.evalDepth = evalDepth;

        this.interceptorList = interceptorList;
        this.lastHandler = lastHandler;
        this.index = 0;
    }

    /**
     * 驱动器
     */
    public FlowDriver getDriver() {
        return driver;
    }

    /**
     * 上下文
     */
    public FlowExchanger getContext() {
        return context;
    }

    /**
     * 开始节点
     */
    public Node getStartNode() {
        return startNode;
    }

    /**
     * 评估深度
     */
    public int getEvalDepth() {
        return evalDepth;
    }

    /**
     * 调用
     */
    public void invoke() throws FlowException {
        if (index < interceptorList.size()) {
            interceptorList.get(index++).target.doIntercept(this);
        } else {
            lastHandler.accept(this);
        }
    }
}
