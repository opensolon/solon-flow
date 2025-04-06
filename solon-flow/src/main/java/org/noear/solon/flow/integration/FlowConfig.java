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
package org.noear.solon.flow.integration;

import org.noear.solon.Utils;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.core.AppContext;
import org.noear.solon.flow.FlowDriver;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.intercept.ChainInterceptor;

import java.util.List;

/**
 * 流配置器
 *
 * @author noear
 * @since 3.1
 */
@Configuration
public class FlowConfig {
    @Condition(onMissingBean = FlowEngine.class)
    @Bean
    public FlowEngine flowEngine() {
        return FlowEngine.newInstance();
    }

    @Bean
    public void flowEngineInit(FlowEngine flowEngine, AppContext context) {
        List<String> chainList = context.cfg().getList("solon.flow");

        if (Utils.isEmpty(chainList)) {
            //默认
            flowEngine.load("classpath:flow/*.yml");
            flowEngine.load("classpath:flow/*.json");
        } else {
            //按配置加载
            for (String chainUri : chainList) {
                flowEngine.load(chainUri);
            }
        }

        context.subWrapsOfType(FlowDriver.class, bw -> {
            flowEngine.register(bw.name(), bw.raw());
        });

        context.subWrapsOfType(ChainInterceptor.class, bw -> {
            flowEngine.addInterceptor(bw.raw(), bw.index());
        });
    }
}
