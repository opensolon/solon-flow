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
package org.noear.solon.flow.stateless;

import org.noear.solon.flow.AbstractFlowDriver;
import org.noear.solon.flow.Container;
import org.noear.solon.flow.Actuator;
import org.noear.solon.flow.container.SolonContainer;
import org.noear.solon.flow.script.LiquorActuator;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;

/**
 * 简单流驱动器
 *
 * @author noear
 * @since 3.1
 */
public class StatelessFlowDriver extends AbstractFlowDriver {
    private final Actuator evaluation;
    private final Container container;

    public StatelessFlowDriver() {
        this(null, null);
    }

    public StatelessFlowDriver(Actuator actuator) {
        this(actuator, null);
    }

    public StatelessFlowDriver(Container container) {
        this(null, container);
    }

    /**
     * @param evaluation 脚本评估器
     * @param container  组件容器
     */
    public StatelessFlowDriver(Actuator evaluation, Container container) {
        this.evaluation = (evaluation == null ? new LiquorActuator() : evaluation);
        this.container = (container == null ? new SolonContainer() : container);
    }

    /**
     * 获取脚本评估器
     */
    @Override
    protected Actuator getEvaluation() {
        return evaluation;
    }

    /**
     * 获取组件容器
     */
    @Override
    protected Container getContainer() {
        return container;
    }


    public static StatefulSimpleFlowDriver.Builder builder() {
        return new StatefulSimpleFlowDriver.Builder();
    }

    public static class Builder {
        private Actuator evaluation;
        private Container container;

        /**
         * 设置评估器
         */
        public Builder evaluation(Actuator evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        /**
         * 设置容器
         */
        public Builder container(Container container) {
            this.container = container;
            return this;
        }

        /**
         * 构建
         */
        public StatelessFlowDriver build() {
            return new StatelessFlowDriver(
                    evaluation,
                    container);
        }
    }
}