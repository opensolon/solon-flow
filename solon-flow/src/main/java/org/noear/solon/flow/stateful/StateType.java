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
 * 状态类型
 *
 * @author noear
 * @since 3.1
 */
public enum StateType {
    /**
     * 未知
     */
    UNKNOWN(0),
    /**
     * 等待
     */
    WAITING(1001),
    /**
     * 完成（通过）
     */
    COMPLETED(1002),
    /**
     * 终止（否决）
     */
    TERMINATED(1003),
    /**
     * 退回（撤回）
     */
    RETURNED(1004),
    /**
     * 重新开始
     */
    RESTART(1005);

    private final int code;

    StateType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据代码构建
     */
    public static StateType codeOf(int code) {
        switch (code) {
            case 1001:
                return WAITING;
            case 1002:
                return COMPLETED;
            case 1003:
                return TERMINATED;
            case 1004:
                return RESTART;
            case 1005:
                return RESTART;
            default:
                return UNKNOWN;
        }
    }
}