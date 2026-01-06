package features.flow.generated.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.noear.solon.flow.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeType 单元测试
 */
class NodeTypeTest {

    @Test
    void testNodeTypeCodes() {
        assertEquals(0, NodeType.UNKNOWN.getCode());
        assertEquals(1, NodeType.START.getCode());
        assertEquals(2, NodeType.END.getCode());
        assertEquals(11, NodeType.ACTIVITY.getCode());
        assertEquals(21, NodeType.EXCLUSIVE.getCode());
        assertEquals(31, NodeType.INCLUSIVE.getCode());
        assertEquals(32, NodeType.PARALLEL.getCode());
        assertEquals(33, NodeType.LOOP.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"START", "start", "Start"})
    void testNameOfCaseInsensitive(String name) {
        assertEquals(NodeType.START, NodeType.nameOf(name));
        assertEquals(NodeType.START, NodeType.nameOf(name, NodeType.UNKNOWN));
    }

    @Test
    void testNameOfWithDefault() {
        // 有效名称
        assertEquals(NodeType.ACTIVITY, NodeType.nameOf("ACTIVITY", NodeType.UNKNOWN));

        // 无效名称返回默认值
        assertEquals(NodeType.UNKNOWN, NodeType.nameOf("INVALID", NodeType.UNKNOWN));

        // null返回默认值
        assertEquals(NodeType.UNKNOWN, NodeType.nameOf(null, NodeType.UNKNOWN));

        // 空字符串返回默认值
        assertEquals(NodeType.UNKNOWN, NodeType.nameOf("", NodeType.UNKNOWN));
    }

    @Test
    void testDeprecatedIteratorType() {
        // iterator 应该映射到 LOOP（带有警告）
        assertEquals(NodeType.LOOP, NodeType.nameOf("iterator"));
        assertEquals(NodeType.LOOP, NodeType.nameOf("iterator", NodeType.UNKNOWN));
    }

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void testIsGatewayMethod(NodeType type) {
        boolean isGateway = NodeType.isGateway(type);

        switch (type) {
            case EXCLUSIVE:
            case INCLUSIVE:
            case PARALLEL:
            case LOOP:
                assertTrue(isGateway, type + " should be a gateway");
                break;
            default:
                assertFalse(isGateway, type + " should not be a gateway");
                break;
        }
    }

    @Test
    void testAllNodeTypes() {
        // 验证所有枚举值
        NodeType[] values = NodeType.values();
        assertEquals(8, values.length); // UNKNOWN, START, END, ACTIVITY, EXCLUSIVE, INCLUSIVE, PARALLEL, LOOP

        // 验证顺序
        assertEquals(NodeType.UNKNOWN, values[0]);
        assertEquals(NodeType.START, values[1]);
        assertEquals(NodeType.END, values[2]);
        assertEquals(NodeType.ACTIVITY, values[3]);
        assertEquals(NodeType.EXCLUSIVE, values[4]);
        assertEquals(NodeType.INCLUSIVE, values[5]);
        assertEquals(NodeType.PARALLEL, values[6]);
        assertEquals(NodeType.LOOP, values[7]);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "exclusive", "inclusive", "parallel", "loop",
            "EXCLUSIVE", "INCLUSIVE", "PARALLEL", "LOOP",
            "Exclusive", "Inclusive", "Parallel", "Loop"
    })
    void testGatewayTypes(String typeName) {
        NodeType type = NodeType.nameOf(typeName);
        assertTrue(NodeType.isGateway(type), typeName + " should be identified as gateway");
    }

    @Test
    void testUnknownType() {
        assertEquals(NodeType.ACTIVITY, NodeType.nameOf("NON_EXISTENT"));
        assertEquals(NodeType.UNKNOWN, NodeType.nameOf("NON_EXISTENT", NodeType.UNKNOWN));
    }
}