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

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.Node;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * FlowInterceptor 简单实现
 *
 * @author noear
 * @since 3.8.1
 */
public class SimpleFlowInterceptor implements FlowInterceptor {
    private final Consumer<FlowInvocation> doIntercept;
    private final BiConsumer<FlowContext, Node> onNodeStart;
    private final BiConsumer<FlowContext, Node> onNodeEnd;

    public SimpleFlowInterceptor(Consumer<FlowInvocation> doIntercept,
                                 BiConsumer<FlowContext, Node> onNodeStart,
                                 BiConsumer<FlowContext, Node> onNodeEnd) {
        this.doIntercept = doIntercept;
        this.onNodeStart = onNodeStart;
        this.onNodeEnd = onNodeEnd;
    }


    @Override
    public void doIntercept(FlowInvocation invocation) throws FlowException {
        if (doIntercept != null) {
            doIntercept.accept(invocation);
        } else {
            invocation.invoke();
        }
    }

    @Override
    public void onNodeStart(FlowContext context, Node node) {
        if (onNodeStart != null) {
            onNodeStart.accept(context, node);
        }
    }

    @Override
    public void onNodeEnd(FlowContext context, Node node) {
        if (onNodeEnd != null) {
            onNodeEnd.accept(context, node);
        }
    }

    /// ///////////////////

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Consumer<FlowInvocation> doIntercept;
        private BiConsumer<FlowContext, Node> onNodeStart;
        private BiConsumer<FlowContext, Node> onNodeEnd;

        public Builder doIntercept(Consumer<FlowInvocation> doIntercept) {
            this.doIntercept = doIntercept;
            return this;
        }

        public Builder onNodeStart(BiConsumer<FlowContext, Node> onNodeStart) {
            this.onNodeStart = onNodeStart;
            return this;
        }

        public Builder onNodeEnd(BiConsumer<FlowContext, Node> onNodeEnd) {
            this.onNodeEnd = onNodeEnd;
            return this;
        }

        public SimpleFlowInterceptor build() {
            return new SimpleFlowInterceptor(doIntercept, onNodeStart, onNodeEnd);
        }
    }
}