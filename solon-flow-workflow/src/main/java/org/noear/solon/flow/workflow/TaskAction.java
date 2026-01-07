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
    UNKNOWN(0, TaskState.UNKNOWN),
    /**
     * 后退（撤回）
     */
    BACK(1010, TaskState.WAITING), //=>state: WAITING
    /**
     * 跳转后退
     */
    BACK_JUMP(1011, TaskState.WAITING), //=>state: WAITING
    /**
     * 前进（通过）
     */
    FORWARD(1020, TaskState.COMPLETED), //=>state: COMPLETED
    /**
     * 跳转前进
     */
    FORWARD_JUMP(1021, TaskState.COMPLETED), //=>state: COMPLETED
    /**
     * 终止（取消）
     */
    TERMINATE(1030, TaskState.TERMINATED), //=>state: TERMINATED
    /**
     * 重新开始
     */
    RESTART(1040, TaskState.UNKNOWN), //=>state: UNKNOWN
    ;

    private final int code;
    private final TaskState targetState;

    TaskAction(int code, TaskState targetState) {
        this.code = code;
        this.targetState = targetState;
    }

    public int getCode() {
        return code;
    }

    public TaskState getTargetState() {
        return targetState;
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
                return TERMINATE;
            case 1040:
                return RESTART;
            default:
                return UNKNOWN;
        }
    }
}