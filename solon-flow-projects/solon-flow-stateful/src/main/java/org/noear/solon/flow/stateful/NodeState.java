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
 * 流节点状态（用整型方便扩展和调整）
 *
 * @author noear
 * @since 3.1
 */
public class NodeState {
    /**
     * 未定义
     */
    public static int UNDEFINED = 0;
    /**
     * 等待
     */
    public static int WAIT = 1001;
    /**
     * 通过
     */
    public static int PASS = 1002;
    /**
     * 中止（拒绝）
     */
    public static int ABORT = 1003;
    /**
     * 回退（撤回）
     */
    public static int BACK = 1004;
    /**
     * 回退全部（重新开始）
     */
    public static int BACK_ALL = 1005;
}