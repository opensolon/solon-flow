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

import org.noear.solon.flow.FlowContextDefault;

/**
 * 有状态的流上下文
 *
 * @author noear
 * @since 3.5
 */
public class StatefulFlowContext extends FlowContextDefault {
    private StateController stateController;
    private StateRepository stateRepository;

    public StatefulFlowContext(String instanceId, StateController stateController, StateRepository stateRepository) {
        super(instanceId);
        this.stateController = stateController;
        this.stateRepository = stateRepository;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public StateController getStateController() {
        return stateController;
    }

    @Override
    public StateRepository getStateRepository() {
        return stateRepository;
    }
}
