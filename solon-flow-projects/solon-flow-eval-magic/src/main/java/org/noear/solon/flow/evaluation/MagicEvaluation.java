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

import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowContext;
import org.ssssssss.script.MagicScript;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;

/**
 * Magic 评估器
 *
 * @author noear
 * @since 3.1
 */
public class MagicEvaluation implements Evaluation {
    @Override
    public boolean runTest(FlowContext context, String code) {
        MagicScriptContext scriptContext = new MagicScriptContext();
        for (Map.Entry<String, Object> entry : context.model().entrySet()) {
            scriptContext.set(entry.getKey(), entry.getValue());
        }


        return (Boolean) MagicScript.create("return " + code + ";", null)
                .execute(scriptContext);
    }

    @Override
    public void runTask(FlowContext context, String code) {
        MagicScriptContext scriptContext = new MagicScriptContext();
        for (Map.Entry<String, Object> entry : context.model().entrySet()) {
            scriptContext.set(entry.getKey(), entry.getValue());
        }


        MagicScript.create(code, null)
                .execute(scriptContext);
    }
}