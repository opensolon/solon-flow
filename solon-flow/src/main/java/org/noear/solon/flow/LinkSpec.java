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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连接定义
 *
 * @author noear
 * @since 3.0
 */
public class LinkSpec {
    private final String nextId;
    private String title;
    private Map<String, Object> meta;
    private String when;
    private ConditionComponent whenComponent;
    /**
     * 优先级（越大越高）
     */
    private int priority;

    /**
     * @param nextId 目标 id
     */
    public LinkSpec(String nextId) {
        this.nextId = nextId;
    }

    /**
     * 配置标题
     */
    public LinkSpec title(String title) {
        this.title = title;
        return this;
    }

    /**
     * 配置元数据
     */
    public LinkSpec meta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }

    /**
     * 配置元数据
     */
    public LinkSpec metaPut(String key, Object value) {
        if (meta == null) {
            meta = new LinkedHashMap<>();
        }

        meta.put(key, value);
        return this;
    }

    /**
     * 配置分支流出条件（用于配置）
     */
    public LinkSpec when(String condition) {
        this.when = condition;
        return this;
    }

    /**
     * 配置分支流出条件（用于硬编码）
     *
     * @since 3.7
     */
    public LinkSpec when(ConditionComponent conditionComponent) {
        this.whenComponent = conditionComponent;
        return this;
    }

    /**
     * 配置分支流出条件
     *
     * @deprecated 3.3 {@link #when(String)}
     */
    @Deprecated
    public LinkSpec condition(String condition) {
        return when(condition);
    }

    /**
     * 配置优先级（越大越优）
     */
    public LinkSpec priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");

        buf.append("nextId='").append(nextId).append('\'');

        if (Utils.isNotEmpty(title)) {
            buf.append(", title='").append(title).append('\'');
        }

        if (Utils.isNotEmpty(when)) {
            buf.append(", when='").append(when).append('\'');
        }

        if (whenComponent != null) {
            buf.append(", whenComponent=").append(whenComponent);
        }

        if (Utils.isNotEmpty(meta)) {
            buf.append(", meta=").append(meta);
        }

        buf.append("}");

        return buf.toString();
    }

    public String getNextId() {
        return nextId;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public String getWhen() {
        return when;
    }

    public ConditionComponent getWhenComponent() {
        return whenComponent;
    }

    public int getPriority() {
        return priority;
    }
}
