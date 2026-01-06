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
package org.noear.solon.flow.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 步进器迭代器 (区间：[start, end)，步长：step)
 * <p>用于根据字符串描述或参数生成等差数列。例如：1, 3, 5, 7, 9</p>
 *
 * @author noear 2025/10/19 created
 * @since 3.6
 */
public class Stepper implements Iterator<Integer> {

    /**
     * 从字符串解析并创建步进器
     *
     * @param str 支持两种格式:
     *            1. "start...end" (步长默认为 1，例如 "1...9")
     *            2. "start:end:step" (显式步长，例如 "1:10:2")
     * @return 步进器实例
     * @throws IllegalArgumentException 如果参数不是合法的整数或格式错误
     */
    public static Stepper from(String str) throws IllegalArgumentException {
        // 优先尝试解析省略号模式 "start...end"
        int ellipsisIdx = str.indexOf("...");

        if (ellipsisIdx > 0) {
            String startStr = str.substring(0, ellipsisIdx);
            String endStr = str.substring(ellipsisIdx + 3);

            try {
                int start = Integer.parseInt(startStr);
                int end = Integer.parseInt(endStr);
                // 省略号模式固定步长为 1
                return new Stepper(start, end, 1);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Stepper parameters must be valid integers: " + str, e);
            }
        } else {
            // 尝试解析冒号模式 "start:end:step"
            String[] terms = str.split(":", 3);

            if (terms.length != 3) {
                throw new IllegalArgumentException("The stepper style must be 'start...end' or 'start:end:step'");
            }

            try {
                int start = Integer.parseInt(terms[0]);
                int end = Integer.parseInt(terms[1]);
                int step = Integer.parseInt(terms[2]);

                return new Stepper(start, end, step);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Stepper parameters must be valid integers: " + str, e);
            }
        }
    }

    private final int start;
    private final int end;
    private final int step;
    private int nextValue;
    private boolean hasMore;

    public Stepper(int start, int end, int step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }

        this.start = start;
        this.end = end;
        this.step = step;
        this.nextValue = start;
        this.hasMore = start < end;
    }

    @Override
    public boolean hasNext() {
        return hasMore;
    }

    @Override
    public Integer next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements in stepper");
        }

        int result = nextValue;

        if (nextValue < end - step) {
            nextValue += step;
        } else {
            hasMore = false;
        }

        return result;
    }

    @Override
    public String toString() {
        return "Stepper{" +
                "start=" + start +
                ", end=" + end +
                ", step=" + step +
                '}';
    }
}