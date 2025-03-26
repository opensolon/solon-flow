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

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.io.EmptyWriter;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.noear.solon.flow.Evaluation;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Beetl 评估器
 *
 * @author noear
 * @since 3.1
 */
public class BeetlEvaluation implements Evaluation {
    public static final Evaluation INSTANCE = new BeetlEvaluation();

    private GroupTemplate template;
    private StringTemplateResourceLoader loader = new StringTemplateResourceLoader();

    private BeetlEvaluation() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("/");
            Configuration cfg = Configuration.defaultConfiguration();
            cfg.setStatementStart("@");
            cfg.setStatementEnd(null);

            template = new GroupTemplate(resourceLoader, cfg, BeetlEvaluation.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean runCondition(String code, Map<String, Object> context) {
        Writer writer = new EmptyWriter();
        Map values = template.runScript("return " + code + ";", context, writer, loader);
        return (Boolean) values.get("return");
    }

    @Override
    public void runTask(String code, Map<String, Object> context) {
        Writer writer = new EmptyWriter();
        template.runScript(code, context, writer, loader);
    }
}