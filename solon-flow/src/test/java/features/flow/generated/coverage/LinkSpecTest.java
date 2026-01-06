package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.noear.solon.flow.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LinkSpec 单元测试
 */
class LinkSpecTest {

    private LinkSpec linkSpec;

    @BeforeEach
    void setUp() {
        linkSpec = new LinkSpec("target-node");
    }

    @Test
    void testLinkSpecCreation() {
        assertEquals("target-node", linkSpec.getNextId());
        assertNull(linkSpec.getTitle());
        assertNull(linkSpec.getMeta());
        assertNull(linkSpec.getWhen());
        assertNull(linkSpec.getWhenComponent());
        assertEquals(0, linkSpec.getPriority());
    }

    @Test
    void testTitleConfiguration() {
        linkSpec.title("连接标题");
        assertEquals("连接标题", linkSpec.getTitle());

        LinkSpec result = linkSpec.title("新标题");
        assertSame(linkSpec, result);
        assertEquals("新标题", linkSpec.getTitle());
    }

    @Test
    void testMetaDataConfiguration() {
        // 测试Map方式
        Map<String, Object> meta = new HashMap<>();
        meta.put("key1", "value1");
        meta.put("key2", 123);

        linkSpec.meta(meta);
        assertNotNull(linkSpec.getMeta());
        assertEquals(2, linkSpec.getMeta().size());
        assertEquals("value1", linkSpec.getMeta().get("key1"));

        // 测试单个添加
        linkSpec.metaPut("key3", true);
        assertEquals(3, linkSpec.getMeta().size());
        assertEquals(true, linkSpec.getMeta().get("key3"));

        // 链式调用
        LinkSpec result = linkSpec.metaPut("key4", "value4");
        assertSame(linkSpec, result);
    }

    @Test
    void testConditionConfiguration() {
        // 字符串条件
        linkSpec.when("a > b");
        assertEquals("a > b", linkSpec.getWhen());
        assertNull(linkSpec.getWhenComponent());

        // 组件条件
        ConditionComponent component = context -> true;
        linkSpec.when(component);
        assertSame(component, linkSpec.getWhenComponent());

        // 链式调用
        LinkSpec result = linkSpec.when("new condition");
        assertSame(linkSpec, result);
    }

    @Test
    void testDeprecatedConditionMethod() {
        // 测试过时的condition方法
        linkSpec.condition("deprecated condition");
        assertEquals("deprecated condition", linkSpec.getWhen());

        // 链式调用
        LinkSpec result = linkSpec.condition("another");
        assertSame(linkSpec, result);
    }

    @Test
    void testPriorityConfiguration() {
        linkSpec.priority(10);
        assertEquals(10, linkSpec.getPriority());

        LinkSpec result = linkSpec.priority(20);
        assertSame(linkSpec, result);
        assertEquals(20, linkSpec.getPriority());
    }

    @Test
    void testToString() {
        linkSpec.title("测试链接")
                .metaPut("key", "value")
                .when("condition")
                .priority(5);

        String str = linkSpec.toString();
        assertNotNull(str);
        assertTrue(str.contains("target-node"));
        assertTrue(str.contains("测试链接"));
        assertTrue(str.contains("condition"));
        assertTrue(str.contains("key"));
    }

    @Test
    void testEmptyLinkSpec() {
        LinkSpec empty = new LinkSpec("empty");
        assertNotNull(empty.getNextId());
        assertNull(empty.getTitle());
        assertNull(empty.getMeta());
        assertNull(empty.getWhen());
        assertNull(empty.getWhenComponent());
        assertEquals(0, empty.getPriority());

        String str = empty.toString();
        assertTrue(str.contains("empty"));
    }

    @Test
    void testLinkSpecWithComponentCondition() {
        ConditionComponent component = context -> {
            return context.getAs("flag") != null;
        };

        linkSpec.when(component)
                .title("组件条件链接");

        assertSame(component, linkSpec.getWhenComponent());
        assertEquals("组件条件链接", linkSpec.getTitle());
    }

    @Test
    void testMultipleMetaOperations() {
        // 初始为null
        assertNull(linkSpec.getMeta());

        // 第一次添加
        linkSpec.metaPut("key1", "value1");
        assertNotNull(linkSpec.getMeta());
        assertEquals(1, linkSpec.getMeta().size());

        // 第二次添加
        linkSpec.metaPut("key2", "value2");
        assertEquals(2, linkSpec.getMeta().size());

        // 使用Map覆盖
        Map<String, Object> newMeta = new HashMap<>();
        newMeta.put("key3", "value3");
        linkSpec.meta(newMeta);
        assertEquals(1, linkSpec.getMeta().size());
        assertEquals("value3", linkSpec.getMeta().get("key3"));
    }
}