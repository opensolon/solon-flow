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

import org.noear.solon.flow.*;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.lang.Preview;

import java.util.concurrent.ExecutorService;

/**
 * 有状态的简单流驱动器（兼容无状态）
 *
 * @author noear
 * @since 3.1
 * @since 3.5
 */
@Preview("3.1")
public class SimpleFlowDriver extends AbstractFlowDriver implements FlowDriver {
    public SimpleFlowDriver() {
        this(null, null);
    }

    public SimpleFlowDriver(Evaluation evaluation) {
        super(evaluation, null, null);
    }

    public SimpleFlowDriver(Container container) {
        super(null, container, null);
    }

    public SimpleFlowDriver(Evaluation evaluation, Container container) {
        super(evaluation, container, null);
    }

    public SimpleFlowDriver(Evaluation evaluation, Container container,  ExecutorService executor) {
        super(evaluation, container, executor);
    }



    /// ////////////////////////////

    /**
     * 处理任务
     *
     * @param exchanger 流交换器
     * @param task      任务
     */
    @Override
    public void handleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        postHandleTask(exchanger, task);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Evaluation evaluation;
        private Container container;
        private ExecutorService executor;

        /**
         * 设置评估器
         */
        public Builder evaluation(Evaluation evaluation) {
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
         * 异步执行器
         */
        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * 构建
         */
        public SimpleFlowDriver build() {
            return new SimpleFlowDriver(
                    evaluation,
                    container,
                    executor);
        }
    }
}