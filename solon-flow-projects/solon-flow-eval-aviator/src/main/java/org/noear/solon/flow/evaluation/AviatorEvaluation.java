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
package org.noear.solon.flow.evaluation;

import com.googlecode.aviator.AviatorEvaluator;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowContext;

/**
 * Aviator 评估器
 *
 * @author noear
 * @since 3.1
 */
public class AviatorEvaluation implements Evaluation {
    @Override
    public boolean runTest(FlowContext context, String code) {
        return (Boolean) AviatorEvaluator.execute(code, context.model());
    }

    @Override
    public void runTask(FlowContext context, String code) {
        AviatorEvaluator.execute(code, context.model());
    }
}