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
 * PlantUML 显示映射结果
 *
 * @author noear
 * @since 3.10
 */
public class PlantumlDisplayResult {
    /**
     * 不显示
     */
    public static final PlantumlDisplayResult HIDDEN = new PlantumlDisplayResult(false, null);

    /**
     * 创建显示结果
     */
    public static PlantumlDisplayResult of(String text) {
        if (text == null || text.isEmpty()) {
            return HIDDEN;
        }
        return new PlantumlDisplayResult(true, text);
    }

    /**
     * 使用默认值显示
     */
    public static PlantumlDisplayResult ofDefault() {
        return new PlantumlDisplayResult(true, null);
    }

    private final boolean visible;
    private final String text;

    private PlantumlDisplayResult(boolean visible, String text) {
        this.visible = visible;
        this.text = text;
    }

    /**
     * 是否可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 获取自定义文本
     */
    public String getText() {
        return text;
    }

    /**
     * 是否使用默认值
     */
    public boolean isUseDefault() {
        return visible && text == null;
    }
}
