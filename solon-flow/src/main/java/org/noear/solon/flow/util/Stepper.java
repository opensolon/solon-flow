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
 * 步进器(start <= x <end)
 *
 * @author noear 2025/10/19 created
 * @since 3.6
 */
public class Stepper implements Iterator {
    public static Stepper from(String str) {
        //"start:end:setp" || "1...9"
        int ellipsisIdx = str.indexOf("...");

        if (ellipsisIdx > 0) {
            String startStr = str.substring(0, ellipsisIdx);
            String endStr = str.substring(ellipsisIdx + 3);

            try {
                int start = Integer.parseInt(startStr);
                int end = Integer.parseInt(endStr);

                return new Stepper(start, end, 1);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("All parameters must be valid integers", e);
            }
        } else {
            String[] terms = str.split(":", 3);

            if (terms.length != 3) {
                throw new IllegalArgumentException("The '$in' stepper style must be: 'start:end:step'");
            }

            try {
                int start = Integer.parseInt(terms[0]);
                int end = Integer.parseInt(terms[1]);
                int step = Integer.parseInt(terms[2]);

                return new Stepper(start, end, step);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("All parameters must be valid integers", e);
            }
        }
    }

    private final int start;
    private final int end;
    private final int step;
    private int nextValue;
    private final boolean hasElements;

    public Stepper(int start, int end, int step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }

        this.start = start;
        this.end = end;
        this.step = step;
        this.nextValue = start;
        this.hasElements = start < end; // 根据关系调整
    }

    @Override
    public boolean hasNext() {
        if (!hasElements) {
            return false;
        }

        // 直接检查下一个值是否在范围内
        return nextValue < end;
    }

    @Override
    public Object next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements in stepper");
        }

        int result = nextValue;

        // 检查并计算下一个值
        if (nextValue <= end - step) {
            nextValue += step;
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