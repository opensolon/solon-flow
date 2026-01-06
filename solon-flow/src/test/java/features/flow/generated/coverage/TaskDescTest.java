package features.flow.generated.coverage;

import org.noear.solon.flow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskDesc 单元测试
 */
class TaskDescTest {

    private Graph graph;
    private Node node;

    @BeforeEach
    void setUp() {
        graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("a");
            spec.addActivity("a").linkAdd("e");
            spec.addEnd("e");
        });

        node = graph.getNode("a");
    }

    @Test
    void testTaskDescCreation() {
        TaskDesc desc = new TaskDesc(node, "console.log('hello')");

        assertSame(node, desc.getNode());
        assertEquals("console.log('hello')", desc.getDescription());
        assertNull(desc.getComponent());
        assertFalse(desc.isEmpty());
    }

    @Test
    void testTaskDescWithComponent() {
        TaskComponent component = (context, node) -> {};
        TaskDesc desc = new TaskDesc(node, null, component);

        assertSame(node, desc.getNode());
        assertNull(desc.getDescription());
        assertSame(component, desc.getComponent());
        assertFalse(desc.isEmpty());
    }

    @Test
    void testEmptyTaskDesc() {
        // 空描述和空组件
        TaskDesc empty1 = new TaskDesc(node, null);
        assertTrue(empty1.isEmpty());

        TaskDesc empty2 = new TaskDesc(node, "");
        assertTrue(empty2.isEmpty());

        TaskDesc empty3 = new TaskDesc(node, "   ");
        assertTrue(empty3.isEmpty());

        // 有描述或组件就不为空
        TaskDesc notEmpty1 = new TaskDesc(node, "task");
        assertFalse(notEmpty1.isEmpty());

        TaskComponent component = (context, node) -> {};
        TaskDesc notEmpty2 = new TaskDesc(node, null, component);
        assertFalse(notEmpty2.isEmpty());
    }

    @Test
    void testStaticIsNotEmptyMethod() {
        // null检查
        assertFalse(TaskDesc.isNotEmpty(null));

        // 空检查
        TaskDesc empty = new TaskDesc(node, null);
        assertFalse(TaskDesc.isNotEmpty(empty));

        // 非空检查
        TaskDesc notEmpty = new TaskDesc(node, "task");
        assertTrue(TaskDesc.isNotEmpty(notEmpty));
    }

    @Test
    void testAttachment() {
        TaskDesc desc = new TaskDesc(node, "task");

        // 测试附件功能
        Object attachment = new Object();
        desc.attachment = attachment;
        assertSame(attachment, desc.attachment);
    }

    @Test
    void testToString() {
        // 非空描述
        TaskDesc desc1 = new TaskDesc(node, "task1()");
        String str1 = desc1.toString();
        assertTrue(str1.contains("a"));
        assertTrue(str1.contains("task1()"));

        // 空描述
        TaskDesc desc2 = new TaskDesc(node, null);
        String str2 = desc2.toString();
        assertTrue(str2.contains("a"));
        assertTrue(str2.contains("null"));
    }

    @Test
    void testTaskDescWithDifferentNodes() {
        Node startNode = graph.getNode("s");
        Node endNode = graph.getNode("e");

        TaskDesc desc1 = new TaskDesc(startNode, "start task");
        TaskDesc desc2 = new TaskDesc(endNode, "end task");

        assertEquals("s", desc1.getNode().getId());
        assertEquals("e", desc2.getNode().getId());
    }
}