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

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 临时的（内部使用）
 *
 * @author noear
 * @since 3.0
 */
public class Temporary {
    static final String ROOT = "_ROOT";

    //记数器
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    //栈
    private final Map<String, Stack> stacks = new ConcurrentHashMap<>();
    //变量
    private final Map<String, Object> vars = new ConcurrentHashMap<>();


    /**
     * 栈获取
     */
    public <T> Stack<T> stack(Chain chain, String key) {
        return stacks.computeIfAbsent(chain.getId() + "/" + key, k -> new Stack<>());
    }


    /**
     * 计数获取
     */
    public int count(Chain chain, String key) {
        return counts.computeIfAbsent(chain.getId() + "/" + key, k -> new AtomicInteger(0))
                .get();
    }

    /**
     * 计数获取
     */
    public int count(String key) {
        return counts.computeIfAbsent(ROOT + "/" + key, k -> new AtomicInteger(0))
                .get();
    }

    /**
     * 计数设置
     */
    public void countSet(Chain chain, String key, int value) {
        counts.computeIfAbsent(chain.getId() + "/" + key, k -> new AtomicInteger(0))
                .set(value);
    }

    /**
     * 计数设置
     */
    public void countSet(String key, int value) {
        counts.computeIfAbsent(ROOT + "/" + key, k -> new AtomicInteger(0))
                .set(value);
    }

    /**
     * 计数增量
     */
    public int countIncr(Chain chain, String key) {
        return counts.computeIfAbsent(chain.getId() + "/" + key, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * 计数增量
     */
    public int countIncr(String key) {
        return counts.computeIfAbsent(ROOT + "/" + key, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * 计数增量
     *
     * @param delta 要添加的数值
     */
    public int countIncr(String key, int delta) {
        return counts.computeIfAbsent(ROOT + "/" + key, k -> new AtomicInteger(0))
                .addAndGet(delta);
    }

    /**
     * 变量集
     */
    public Map<String, Object> vars() {
        return vars;
    }

    @Override
    public String toString() {
        return "{" +
                "counts=" + counts +
                ", stacks=" + stacks +
                '}';
    }
}