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

import org.noear.solon.Utils;

/**
 * 任务描述（表达式参考：'F,tag/fun1;R,tag/rule1'）
 *
 * @author noear
 * @since 3.0
 * */
public class TaskDesc {
    /**
     * 是否为非空
     */
    public static boolean isNotEmpty(TaskDesc t) {
        return t != null && t.isEmpty() == false;
    }

    private final Node node;
    //任务描述（用于配置）
    private final String description;
    //任务组件（用于硬编码）
    private final TaskComponent component;

    /**
     * 附件（按需定制使用）
     */
    public Object attachment;//如果做扩展解析，用作存储位；


    /**
     * @param description 任务描述
     */
    public TaskDesc(Node node, String description) {
        this.node = node;
        if (description != null) {
            this.description = description.trim();
        } else {
            this.description = null;
        }
        this.component = null;
    }

    /**
     * @param description 任务描述
     */
    public TaskDesc(Node node, String description, TaskComponent component) {
        this.node = node;
        if (description != null) {
            this.description = description.trim();
        } else {
            this.description = null;
        }
        this.component = component;
    }

    /**
     * 获取所属节点
     */
    public Node getNode() {
        return node;
    }

    /**
     * 任务描述（用于配置。示例："F:tag/fun1;R:tag/rule1" 或 "fun1()" 或 "[{t:'F',c:'tag/fun1'}]"）
     */
    public String getDescription() {
        return description;
    }

    /**
     * 任务组件（用于硬编码）
     */
    public TaskComponent getComponent() {
        return component;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return Utils.isEmpty(description) && component == null;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{" +
                    "nodeId='" + node.getId() + '\'' +
                    ", description=null" +
                    '}';
        } else {
            return "{" +
                    "nodeId='" + node.getId() + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}