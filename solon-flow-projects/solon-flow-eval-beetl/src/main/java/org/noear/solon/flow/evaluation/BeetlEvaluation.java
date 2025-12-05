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
import org.noear.solon.flow.FlowContext;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Beetl 评估器
 *
 * @author noear
 * @since 3.1
 */
public class BeetlEvaluation implements Evaluation, Closeable {
    private final GroupTemplate engine;
    private final StringTemplateResourceLoader templateLoader = new StringTemplateResourceLoader();
    private final ClasspathResourceLoader resourceLoader;

    public BeetlEvaluation() {
        try {
            resourceLoader = new ClasspathResourceLoader("/");
            Configuration cfg = Configuration.defaultConfiguration();
            cfg.setStatementStart("@");
            cfg.setStatementEnd(null);

            engine = new GroupTemplate(resourceLoader, cfg, BeetlEvaluation.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean runCondition(FlowContext context, String code) {
        Writer writer = new EmptyWriter();
        Map values = engine.runScript("return " + code + ";", context.model(), writer, templateLoader);
        return (Boolean) values.get("return");
    }

    @Override
    public void runTask(FlowContext context, String code) {
        Writer writer = new EmptyWriter();
        engine.runScript(code, context.model(), writer, templateLoader);
    }

    @Override
    public void close() throws IOException {
        templateLoader.close();
        resourceLoader.close();
        engine.close();
    }
}