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

import org.noear.solon.flow.AbstractFlowContext;
import org.noear.solon.flow.stateful.StateController;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.lang.Preview;

/**
 * 无状态流上下文（不支持序列化）
 *
 * @author noear
 * @since 3.0
 * @since 3.5
 */
@Preview("3.0")
public class StatelessFlowContext extends AbstractFlowContext {

    @Override
    public boolean isStateful() {
        return false;
    }

    @Override
    public StateController stateController() {
        return null;
    }

    @Override
    public StateRepository stateRepository() {
        return null;
    }
}