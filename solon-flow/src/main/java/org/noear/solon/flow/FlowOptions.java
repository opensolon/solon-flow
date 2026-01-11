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
package org.noear.solon.flow;

import org.noear.solon.core.util.RankEntity;
import org.noear.solon.flow.intercept.FlowInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流执行选项
 *
 * @author noear
 * @since 3.8.1
 */
public class FlowOptions {
    private final List<RankEntity<FlowInterceptor>> interceptorList = new ArrayList<>();

    public List<RankEntity<FlowInterceptor>> getInterceptorList() {
        return interceptorList;
    }

    protected void interceptorAdd(List<RankEntity<FlowInterceptor>> interceptors) {
        interceptorList.addAll(interceptors);

        if (interceptorList.size() > 0) {
            Collections.sort(interceptorList);
        }
    }

    /**
     * 添加流拦截器
     */
    public FlowOptions interceptorAdd(FlowInterceptor interceptor) {
        return interceptorAdd(interceptor, 0);
    }

    /**
     * 添加流拦截器
     */
    public FlowOptions interceptorAdd(FlowInterceptor interceptor, int index) {
        interceptorList.add(new RankEntity<>(interceptor, index));

        if (interceptorList.size() > 0) {
            Collections.sort(interceptorList);
        }
        return this;
    }
}