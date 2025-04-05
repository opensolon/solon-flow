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
package org.noear.solon.flow.stateful;

/**
 * 流节点技术状态（状态可分为：技术状态，业务状态）
 *
 * @author noear
 * @since 3.1
 */
public interface NodeState {
    /**
     * 未知
     */
    int UNKNOWN = 0;
    /**
     * 等待
     */
    int WAITING = 1001;
    /**
     * 完成（通过）
     */
    int COMPLETED = 1002;
    /**
     * 终止（否决）
     */
    int TERMINATED = 1003;
    /**
     * 退回（撤回）
     */
    int RETURNED = 1004;
    /**
     * 重新开始
     */
    int RESTART = 1005;
}