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
package org.noear.solon.flow.workflow;

import org.noear.solon.flow.Graph;
import org.noear.solon.lang.Internal;
import org.noear.solon.lang.NonSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流执行命令
 *
 * @author noear
 * @since 3.8.1
 */
@Internal
public class WorkflowIntent implements NonSerializable { //用 public 可以正常作为脚本参数
    protected static final String INTENT_KEY = WorkflowIntent.class.getSimpleName();

    protected final Graph rootGraph;
    protected final IntentType type;

    protected List<Task> nextTasks = new ArrayList<>();
    protected Task task;

    protected WorkflowIntent(Graph rootGraph, IntentType type) {
        this.rootGraph = rootGraph;
        this.type = type;
    }

    protected enum IntentType {
        UNKNOWN,
        CLAIM_TASK, //认领
        FIND_TASK, //查找
        FIND_NEXT_TASKS, //查找
        SUBMIT_TASK, //提交
        SUBMIT_TASK_IF_WAITING, //提交并检测是等等状态
    }
}