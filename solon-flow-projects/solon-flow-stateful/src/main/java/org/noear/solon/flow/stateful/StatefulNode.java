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
package org.noear.solon.flow.stateful;

import org.noear.solon.flow.Node;

/**
 * 有状态的节点
 *
 * @author noear
 * @since 3.1
 */
public class StatefulNode {
    public static final String KEY_ACTIVITY_NODE = "ACTIVITY_NODE";
    public static final String KEY_ACTIVITY_LIST = "ACTIVITY_LIST";
    public static final String KEY_ACTIVITY_LIST_GET = "ACTIVITY_LIST_GET";

    private final Node node;
    private final int state;

    public StatefulNode(Node node, int state) {
        this.node = node;
        this.state = state;
    }

    /**
     * 节点
     */
    public Node getNode() {
        return node;
    }

    /**
     * 状态
     */
    public int getState() {
        return state;
    }

    @Override
    public String toString() {
        return "StatefulNode{" +
                "node=" + node +
                ", state=" + state +
                '}';
    }
}