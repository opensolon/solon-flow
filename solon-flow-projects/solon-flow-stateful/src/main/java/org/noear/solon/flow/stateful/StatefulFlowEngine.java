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

import org.noear.solon.flow.FlowEngineDefault;
import org.noear.solon.lang.Preview;

import java.util.List;

/**
 * 有状态的流引擎（也可以用于无状态）
 *
 * @author noear
 * @since 3.1
 */
@Preview("3.1")
public class StatefulFlowEngine extends FlowEngineDefault {
    private FlowStateRepository stateRepository;

    public StatefulFlowEngine(StatefulSimpleFlowDriver driver) {
        super();
        this.stateRepository = driver.getStateRepository();
        register(driver);
    }

    /**
     * 获取状态
     */
    public int getState(StatefulFlowContext context, String chainId, String nodeId) {
        return stateRepository.getState(context, chainId, nodeId);
    }

    /**
     * 获取状态记录
     */
    public List<FlowStateRecord> getStateRecords(StatefulFlowContext context) {
        return stateRepository.getStateRecords(context);
    }

    /**
     * 提交状态
     */
    public void postState(StatefulFlowContext context, String chainId, String nodeId, int state) {
        stateRepository.postState(context, chainId, nodeId, state, this);
    }
}
