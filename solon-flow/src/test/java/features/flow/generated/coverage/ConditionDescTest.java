package features.flow.generated.coverage;

import org.noear.solon.flow.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionDesc 单元测试
 */
class ConditionDescTest {

    @Test
    void testConditionDescCreation() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        ConditionDesc desc = new ConditionDesc(graph, "a > 10");

        assertSame(graph, desc.getGraph());
        assertEquals("a > 10", desc.getDescription());
        assertNull(desc.getComponent());
        assertFalse(desc.isEmpty());
    }

    @Test
    void testConditionDescWithComponent() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        ConditionComponent component = context -> true;
        ConditionDesc desc = new ConditionDesc(graph, null, component);

        assertSame(graph, desc.getGraph());
        assertNull(desc.getDescription());
        assertSame(component, desc.getComponent());
        assertFalse(desc.isEmpty());
    }

    @Test
    void testEmptyConditionDesc() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        // 空描述和空组件
        ConditionDesc empty1 = new ConditionDesc(graph, null);
        assertTrue(empty1.isEmpty());

        ConditionDesc empty2 = new ConditionDesc(graph, "");
        assertTrue(empty2.isEmpty());

        ConditionDesc empty3 = new ConditionDesc(graph, "   ");
        assertTrue(empty3.isEmpty());

        // 有描述或组件就不为空
        ConditionDesc notEmpty1 = new ConditionDesc(graph, "condition");
        assertFalse(notEmpty1.isEmpty());

        ConditionComponent component = context -> true;
        ConditionDesc notEmpty2 = new ConditionDesc(graph, null, component);
        assertFalse(notEmpty2.isEmpty());
    }

    @Test
    void testStaticIsNotEmptyMethod() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        // null检查
        assertFalse(ConditionDesc.isNotEmpty(null));

        // 空检查
        ConditionDesc empty = new ConditionDesc(graph, null);
        assertFalse(ConditionDesc.isNotEmpty(empty));

        // 非空检查
        ConditionDesc notEmpty = new ConditionDesc(graph, "condition");
        assertTrue(ConditionDesc.isNotEmpty(notEmpty));
    }

    @Test
    void testAttachment() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        ConditionDesc desc = new ConditionDesc(graph, "condition");

        // 测试附件功能
        Object attachment = new Object();
        desc.attachment = attachment;
        assertSame(attachment, desc.attachment);
    }

    @Test
    void testToString() {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        // 非空描述
        ConditionDesc desc1 = new ConditionDesc(graph, "a > 10");
        String str1 = desc1.toString();
        assertTrue(str1.contains("a > 10"));

        // 空描述
        ConditionDesc desc2 = new ConditionDesc(graph, null);
        String str2 = desc2.toString();
        assertTrue(str2.contains("null"));
    }
}