package features.flow.generated.coverage;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.util.Stepper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stepper 单元测试
 */
class StepperTest {

    @Test
    void testStepperCreation() {
        Stepper stepper = new Stepper(1, 10, 2);
        assertNotNull(stepper);
    }

    @Test
    void testInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Stepper(1, 10, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Stepper(1, 10, -1);
        });
    }

    @Test
    void testStepperIteration() {
        Stepper stepper = new Stepper(1, 5, 1);
        List<Integer> results = new ArrayList<>();

        while (stepper.hasNext()) {
            results.add((Integer) stepper.next());
        }

        assertEquals(Arrays.asList(1, 2, 3, 4), results);

        // 验证迭代结束后没有更多元素
        assertFalse(stepper.hasNext());
        assertThrows(NoSuchElementException.class, stepper::next);
    }

    @Test
    void testStepperWithStepGreaterThanOne() {
        Stepper stepper = new Stepper(0, 10, 3);
        List<Integer> results = new ArrayList<>();

        while (stepper.hasNext()) {
            results.add((Integer) stepper.next());
        }

        assertEquals(Arrays.asList(0, 3, 6, 9), results);
    }

    @Test
    void testStepperStartGreaterThanEnd() {
        Stepper stepper = new Stepper(5, 1, 1);
        assertFalse(stepper.hasNext());

        // 当start > end时，hasNext应该立即返回false
        assertThrows(NoSuchElementException.class, stepper::next);
    }

    @Test
    void testStepperEdgeCases() {
        // 测试步长等于范围
        Stepper stepper1 = new Stepper(1, 2, 1);
        assertTrue(stepper1.hasNext());
        assertEquals(1, stepper1.next());
        assertFalse(stepper1.hasNext());

        // 测试步长大于范围
        Stepper stepper2 = new Stepper(1, 2, 2);
        assertTrue(stepper2.hasNext()); // 初始值为第一步
        stepper2.next();
        assertFalse(stepper2.hasNext());

        // 测试负值
        Stepper stepper3 = new Stepper(-5, 0, 2);
        List<Integer> results = new ArrayList<>();
        while (stepper3.hasNext()) {
            results.add((Integer) stepper3.next());
        }
        assertEquals(Arrays.asList(-5, -3, -1), results);
    }

    @Test
    void testFromStringWithEllipsisNotation() {
        Stepper stepper = Stepper.from("1...5");
        assertNotNull(stepper);

        List<Integer> results = new ArrayList<>();
        while (stepper.hasNext()) {
            results.add((Integer) stepper.next());
        }

        assertEquals(Arrays.asList(1, 2, 3, 4), results);
    }

    @Test
    void testFromStringWithColonNotation() {
        Stepper stepper = Stepper.from("0:10:2");
        assertNotNull(stepper);

        List<Integer> results = new ArrayList<>();
        while (stepper.hasNext()) {
            results.add((Integer) stepper.next());
        }

        assertEquals(Arrays.asList(0, 2, 4, 6, 8), results);
    }

    @Test
    void testFromStringInvalidFormat() {
        // 无效的格式
        assertThrows(IllegalArgumentException.class, () -> {
            Stepper.from("invalid");
        });

        // 缺少参数
        assertThrows(IllegalArgumentException.class, () -> {
            Stepper.from("1:2");
        });

        // 非数字参数
        assertThrows(IllegalArgumentException.class, () -> {
            Stepper.from("a:b:c");
        });

        // 无效的省略号格式
        assertThrows(IllegalArgumentException.class, () -> {
            Stepper.from("a...b");
        });
    }

    @ParameterizedTest
    @CsvSource({
            "1...5, '1,2,3,4'",
            "0:10:2, '0,2,4,6,8'",
            "5:15:5, '5,10'",
            "-3...3, '-3,-2,-1,0,1,2'"
    })
    void testFromStringVariousFormats(String input, String expected) {
        Stepper stepper = Stepper.from(input);
        List<String> results = new ArrayList<>();

        while (stepper.hasNext()) {
            results.add(stepper.next().toString());
        }

        String actual = String.join(",", results);
        assertEquals(expected, actual);
    }

    @Test
    void testToString() {
        Stepper stepper = new Stepper(1, 10, 2);
        String str = stepper.toString();

        assertNotNull(str);
        assertTrue(str.contains("Stepper"));
        assertTrue(str.contains("start=1"));
        assertTrue(str.contains("end=10"));
        assertTrue(str.contains("step=2"));
    }

    @Test
    void testStepperReusability() {
        // 创建新的stepper并迭代
        Stepper stepper = new Stepper(1, 4, 1);

        // 第一次迭代
        List<Integer> firstPass = new ArrayList<>();
        while (stepper.hasNext()) {
            firstPass.add((Integer) stepper.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), firstPass);

        // 注意：Stepper不是可重置的，迭代后不能重新使用
        assertFalse(stepper.hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1...1", "5:5:1", "10:5:2"})
    void testEmptySteppers(String input) {
        Stepper stepper = Stepper.from(input);
        assertFalse(stepper.hasNext());
    }
}