package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.noear.solon.flow.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeSpec 单元测试
 */
class NodeSpecTest {

    private NodeSpec nodeSpec;

    @BeforeEach
    void setUp() {
        nodeSpec = NodeSpec.activityOf("test-node");
    }

    @Test
    void testNodeSpecCreation() {
        assertEquals("test-node", nodeSpec.getId());
        assertEquals(NodeType.ACTIVITY, nodeSpec.getType());
    }

    @Test
    void testStaticFactoryMethods() {
        NodeSpec start = NodeSpec.startOf("start");
        assertEquals(NodeType.START, start.getType());

        NodeSpec end = NodeSpec.endOf("end");
        assertEquals(NodeType.END, end.getType());

        NodeSpec activity = NodeSpec.activityOf("activity");
        assertEquals(NodeType.ACTIVITY, activity.getType());

        NodeSpec inclusive = NodeSpec.inclusiveOf("inclusive");
        assertEquals(NodeType.INCLUSIVE, inclusive.getType());

        NodeSpec exclusive = NodeSpec.exclusiveOf("exclusive");
        assertEquals(NodeType.EXCLUSIVE, exclusive.getType());

        NodeSpec parallel = NodeSpec.parallelOf("parallel");
        assertEquals(NodeType.PARALLEL, parallel.getType());

        NodeSpec loop = NodeSpec.loopOf("loop");
        assertEquals(NodeType.LOOP, loop.getType());
    }

    @Test
    void testThenMethod() {
        NodeSpec result = nodeSpec.then(spec -> {
            spec.title("Modified Title");
            spec.metaPut("key", "value");
        });

        assertSame(nodeSpec, result);
        assertEquals("Modified Title", nodeSpec.getTitle());
        assertEquals("value", nodeSpec.getMeta().get("key"));
    }

    @Test
    void testTitleConfiguration() {
        nodeSpec.title("测试节点");
        assertEquals("测试节点", nodeSpec.getTitle());
    }

    @Test
    void testMetaDataConfiguration() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("key1", "value1");
        meta.put("key2", 123);

        nodeSpec.meta(meta);
        nodeSpec.metaPut("key3", true);

        Map<String, Object> result = nodeSpec.getMeta();
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
        assertEquals(true, result.get("key3"));
    }

    @Test
    void testLinkManagement() {
        // 添加链接
        nodeSpec.linkAdd("next1", link -> {
            link.title("第一个链接");
            link.when("condition1");
        });

        nodeSpec.linkAdd("next2");
        nodeSpec.linkAdd("next3", null); // 使用null配置

        assertEquals(3, nodeSpec.getLinks().size());
        assertEquals("next1", nodeSpec.getLinks().get(0).getNextId());
        assertEquals("第一个链接", nodeSpec.getLinks().get(0).getTitle());

        // 移除链接
        nodeSpec.linkRemove("next2");
        assertEquals(2, nodeSpec.getLinks().size());

        // 验证剩余链接
        assertEquals("next1", nodeSpec.getLinks().get(0).getNextId());
        assertEquals("next3", nodeSpec.getLinks().get(1).getNextId());
    }

    @Test
    void testConditionConfiguration() {
        // 字符串条件
        nodeSpec.when("a > 10");
        assertEquals("a > 10", nodeSpec.getWhen());
        assertNull(nodeSpec.getWhenComponent());

        // 组件条件
        ConditionComponent component = context -> true;
        nodeSpec.when(component);
        assertSame(component, nodeSpec.getWhenComponent());
    }

    @Test
    void testTaskConfiguration() {
        // 字符串任务
        nodeSpec.task("console.log('hello')");
        assertEquals("console.log('hello')", nodeSpec.getTask());
        assertNull(nodeSpec.getTaskComponent());

        // 组件任务
        TaskComponent component = (context, node) -> {};
        nodeSpec.task(component);
        assertSame(component, nodeSpec.getTaskComponent());
    }

    @Test
    void testToString() {
        nodeSpec.title("测试")
                .metaPut("key", "value")
                .when("condition")
                .task("task")
                .linkAdd("next");

        String str = nodeSpec.toString();
        assertNotNull(str);
        assertTrue(str.contains("test-node"));
        assertTrue(str.contains("测试"));
        assertTrue(str.contains("condition"));
        assertTrue(str.contains("task"));
        assertTrue(str.contains("key"));
    }

    @Test
    void testEmptyNodeSpec() {
        NodeSpec empty = NodeSpec.activityOf("empty");
        assertNotNull(empty.getId());
        assertNotNull(empty.getType());
        assertNull(empty.getTitle());
        assertNotNull(empty.getMeta());
        assertTrue(empty.getMeta().isEmpty());
        assertNotNull(empty.getLinks());
        assertTrue(empty.getLinks().isEmpty());
        assertNull(empty.getWhen());
        assertNull(empty.getWhenComponent());
        assertNull(empty.getTask());
        assertNull(empty.getTaskComponent());
    }

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void testAllNodeTypesWithSpec(NodeType type) {
        NodeSpec spec = new NodeSpec("test", type);
        assertEquals(type, spec.getType());
    }

    @Test
    void testLinkSpecIntegration() {
        nodeSpec.linkAdd("target", link -> {
            link.title("测试链接")
                    .metaPut("linkKey", "linkValue")
                    .when("linkCondition")
                    .priority(10);
        });

        LinkSpec link = nodeSpec.getLinks().get(0);
        assertEquals("target", link.getNextId());
        assertEquals("测试链接", link.getTitle());
        assertEquals("linkValue", link.getMeta().get("linkKey"));
        assertEquals("linkCondition", link.getWhen());
        assertEquals(10, link.getPriority());
    }

    @Test
    void testDeprecatedConditionMethod() {
        // 测试过时的condition方法（向后兼容）
        nodeSpec.linkAdd("target", link -> {
            link.condition("deprecated"); // 应该委托给when方法
        });

        LinkSpec link = nodeSpec.getLinks().get(0);
        assertEquals("deprecated", link.getWhen());
    }
}