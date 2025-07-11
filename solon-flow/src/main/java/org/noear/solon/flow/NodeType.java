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

/**
 * 节点类型
 *
 * @author noear
 * @since 3.0
 * */
public enum NodeType {
    /**
     * 开始
     */
    START(0),
    /**
     * 结束
     */
    END(1),

    /**
     * 活动节点
     */
    ACTIVITY(11),

    /**
     * 排他网关（单选）
     */
    EXCLUSIVE(21),
    /**
     * 包容网关（多选）
     */
    INCLUSIVE(22),
    /**
     * 并行网关（全选）
     */
    PARALLEL(23);

    private int code;

    public int getCode() {
        return code;
    }

    NodeType(int code) {
        this.code = code;
    }

    public static NodeType codeOf(int code) {
        NodeType[] values = NodeType.values();
        for (NodeType v : values) {
            if (v.code == code) {
                return v;
            }
        }

        return ACTIVITY;
    }

    public static NodeType nameOf(String name) {
        NodeType[] values = NodeType.values();
        for (NodeType v : values) {
            if (v.name().equalsIgnoreCase(name)) {
                return v;
            }
        }

        return ACTIVITY;
    }

    /**
     * 是否为网关
     */
    public static boolean isGateway(NodeType nodeType) {
        return nodeType.getCode() > ACTIVITY.getCode();
    }
}