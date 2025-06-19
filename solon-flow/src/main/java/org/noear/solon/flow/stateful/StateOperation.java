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
 * 状态操作
 *
 * @author noear
 * @since 3.3
 */
public enum StateOperation {
    /**
     * 未知
     */
    UNKNOWN(0),
    /**
     * 后退（撤回）
     */
    BACK(1001), //=>state: WAITING
    /**
     * 前进（通过）
     */
    FORWARD(1002), //=>state: COMPLETED
    /**
     * 终止（否决）
     */
    TERMINATED(1003), //=>state: TERMINATED
    /**
     * 重新开始
     */
    RESTART(1004), //=>state: UNKNOWN
    ;

    private final int code;

    StateOperation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据代码构建
     */
    public static StateOperation codeOf(int code) {
        switch (code) {
            case 1001:
                return BACK;
            case 1002:
                return FORWARD;
            case 1003:
                return TERMINATED;
            case 1004:
                return RESTART;
            default:
                return UNKNOWN;
        }
    }
}