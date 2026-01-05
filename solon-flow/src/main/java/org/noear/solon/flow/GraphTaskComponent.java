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

import org.noear.solon.lang.Preview;

/**
 * 图的任务组件模式
 *
 * @author noear
 * @since 3.8.1
 */
@Preview("3.8.1")
public class GraphTaskComponent implements NamedTaskComponent {
    private final Graph graph;

    public GraphTaskComponent(Graph graph) {
        this.graph = graph;
    }

    @Override
    public String name() {
        return graph.getId();
    }

    @Override
    public String title() {
        return graph.getTitle();
    }

    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        ((FlowContextInternal) context).exchanger().runGraph(graph);
    }
}
