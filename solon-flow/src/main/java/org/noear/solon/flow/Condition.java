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

import org.noear.solon.Utils;


/**
 * 条件（一般用于分支条件）
 *
 * @author noear
 * @since 3.0
 * */
public class Condition {
    /**
     * 是否为非空
     */
    public static boolean isNotEmpty(Condition c) {
        return c != null && c.isEmpty() == false;
    }

    private final String description;
    private final Chain chain;

    /**
     * 附件（按需定制使用）
     */
    public Object attachment;//如果做扩展解析，用作存储位；（不解析，定制性更强）

    /**
     * @param description 条件描述
     */
    public Condition(Chain chain, String description) {
        this.chain = chain;
        this.description = description;
    }

    /**
     * 获取链
     */
    public Chain getChain() {
        return chain;
    }

    /**
     * 描述（示例："(a,>,12) and (b,=,1)" 或 "a=12 && b=1" 或 "[{l:'a',p:'>',r:'12'}...]"）
     */
    public String getDescription() {
        return description;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return Utils.isEmpty(description);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{" +
                    "description=null" +
                    '}';
        } else {
            return "{" +
                    "description='" + description + '\'' +
                    '}';
        }
    }
}