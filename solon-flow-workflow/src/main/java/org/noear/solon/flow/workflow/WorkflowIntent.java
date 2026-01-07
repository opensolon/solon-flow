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

import org.noear.solon.lang.Internal;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流执行命令
 *
 * @author noear
 * @since 3.8.1
 */
@Internal
public class WorkflowIntent { //用 public 可以正常作为脚本参数
    protected static final String INTENT_KEY = WorkflowIntent.class.getSimpleName();

    protected List<Task> nextTasks = new ArrayList<>();
    protected Task task;
    protected final IntentType type;

    protected WorkflowIntent(IntentType type) {
        this.type = type;
    }

    protected enum IntentType {
        UNKNOWN,
        Get_TASK,
        GET_NEXT_TASKS,
        POST_TASK,
        POST_TASK_IF_WAITING,
    }
}