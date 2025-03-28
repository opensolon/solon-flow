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
package org.noear.solon.flow.stateful.operator;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateOperator;

import java.util.*;

/**
 * 简单状态操作员
 *
 * @author noear
 * @since 3.1
 */
public class SimpleStateOperator implements StateOperator {
    private final Set<String> keys = new HashSet<>();

    public SimpleStateOperator() {
        this("actor");
    }

    public SimpleStateOperator(String... keys) {
        this.keys.addAll(Arrays.asList(keys));
    }

    @Override
    public boolean isOperatable(FlowContext context, Node node) {
        for (String key : keys) {
            String valOfMeta = node.getMeta(key);
            String valOfCtx = context.get(key);

            if (Objects.equals(valOfMeta, valOfCtx)) {
                return true;
            }
        }

        return false;
    }
}