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
package org.noear.solon.flow.workflow;

import org.noear.solon.flow.Container;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowDriver;
import org.noear.solon.lang.Preview;

/**
 * 工作流驱动器
 *
 * @author noear
 * @since 3.8
 */
@Preview("3.8")
public interface WorkflowDriver extends FlowDriver {
    /**
     * 获取状态控制器
     */
    StateController getStateController();

    /**
     * 获取状态仓库（持久化）
     */
    StateRepository getStateRepository();


    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private Evaluation evaluation;
        private Container container;
        private StateController stateController;
        private StateRepository stateRepository;

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
         * 设置状态控制
         */
        public Builder stateController(StateController stateController) {
            this.stateController = stateController;
            return this;
        }

        /**
         * 设置状态仓库
         */
        public Builder stateRepository(StateRepository stateRepository) {
            this.stateRepository = stateRepository;
            return this;
        }

        /**
         * 构建
         */
        public WorkflowDriver build() {
            return new WorkflowDriverDefault(
                    evaluation,
                    container,
                    stateController,
                    stateRepository);
        }
    }
}
