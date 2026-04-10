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
 * PlantUML 输出选项
 *
 * @author noear
 * @since 3.10
 */
public class PlantumlOptions {
    /**
     * 默认选项
     */
    public static final PlantumlOptions DEFAULT = new PlantumlOptions();

    /**
     * 是否显示网关类型名
     */
    private boolean showGatewayType = true;

    /**
     * 是否在 title 中显示 id
     */
    private boolean showIdInTitle = false;

    public PlantumlOptions() {
    }

    /**
     * 是否显示网关类型名
     */
    public boolean isShowGatewayType() {
        return showGatewayType;
    }

    /**
     * 设置是否显示网关类型名
     */
    public PlantumlOptions showGatewayType(boolean showGatewayType) {
        this.showGatewayType = showGatewayType;
        return this;
    }

    /**
     * 是否在 title 中显示 id
     */
    public boolean isShowIdInTitle() {
        return showIdInTitle;
    }

    /**
     * 设置是否在 title 中显示 id（格式：title (id)）
     */
    public PlantumlOptions showIdInTitle(boolean showIdInTitle) {
        this.showIdInTitle = showIdInTitle;
        return this;
    }
}
