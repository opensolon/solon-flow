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
package org.noear.solon.flow.driver;

import org.noear.solon.flow.Container;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.container.SolonContainer;
import org.noear.solon.flow.evaluation.LiquorEvaluation;

/**
 * 简单流驱动器
 *
 * @author noear
 * @since 3.1
 */
public class SimpleFlowDriver extends AbstractFlowDriver {
    private final Evaluation evaluation;
    private final Container container;

    public SimpleFlowDriver() {
        this(null, null);
    }

    /**
     * @param evaluation 脚本评估器
     */
    public SimpleFlowDriver(Evaluation evaluation) {
        this(evaluation, null);
    }

    /**
     * @param container 组件容器
     */
    public SimpleFlowDriver(Container container) {
        this(null, container);
    }

    /**
     * @param evaluation 脚本评估器
     * @param container  组件容器
     */
    public SimpleFlowDriver(Evaluation evaluation, Container container) {
        this.evaluation = (evaluation == null ? new LiquorEvaluation() : evaluation);
        this.container = (container == null ? new SolonContainer() : container);
    }

    /**
     * 获取脚本评估器
     */
    @Override
    protected Evaluation getEvaluation() {
        return evaluation;
    }

    /**
     * 获取组件容器
     */
    @Override
    protected Container getContainer() {
        return container;
    }
}