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

/**
 * 任务动作
 *
 * @author noear
 * @since 3.3
 */
public enum TaskAction {
    /**
     * 未知
     */
    UNKNOWN(0),
    /**
     * 后退（撤回）
     */
    BACK(1010), //=>state: WAITING
    /**
     * 跳转后退
     */
    BACK_JUMP(1011), //=>state: WAITING
    /**
     * 前进（通过）
     */
    FORWARD(1020), //=>state: COMPLETED
    /**
     * 跳转前进
     */
    FORWARD_JUMP(1021), //=>state: COMPLETED
    /**
     * 终止（取消）
     */
    TERMINATED(1030), //=>state: TERMINATED
    /**
     * 重新开始
     */
    RESTART(1040), //=>state: UNKNOWN
    ;

    private final int code;

    TaskAction(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据代码构建
     */
    public static TaskAction codeOf(int code) {
        switch (code) {
            case 1010:
                return BACK;
            case 1011:
                return BACK_JUMP;
            case 1020:
                return FORWARD;
            case 1021:
                return FORWARD_JUMP;
            case 1030:
                return TERMINATED;
            case 1040:
                return RESTART;
            default:
                return UNKNOWN;
        }
    }
}