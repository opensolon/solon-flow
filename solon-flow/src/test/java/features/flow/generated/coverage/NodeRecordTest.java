package features.flow.generated.coverage;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.noear.solon.flow.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeRecord 单元测试
 */
class NodeRecordTest {

    private Graph graph;
    private Node node;

    @BeforeEach
    void setUp() {
        graph = Graph.create("test-graph", spec -> {
            spec.addStart("s").title("开始节点").linkAdd("a1");
            spec.addActivity("a1").title("活动节点").linkAdd("e");
            spec.addEnd("e").title("结束节点");
        });

        node = graph.getNode("a1");
    }

    @Test
    void testNodeRecordCreation() {
        NodeRecord record = new NodeRecord(node);

        assertEquals("test-graph", record.getGraphId());
        assertEquals("a1", record.getId());
        assertEquals("活动节点", record.getTitle());
        assertEquals(NodeType.ACTIVITY, record.getType());
        assertTrue(record.getTimestamp() > 0);
    }

    @Test
    void testIsEndMethod() {
        // 活动节点不是结束节点
        NodeRecord activityRecord = new NodeRecord(node);
        assertFalse(activityRecord.isEnd());

        // 开始节点不是结束节点
        NodeRecord startRecord = new NodeRecord(graph.getNode("s"));
        assertFalse(startRecord.isEnd());

        // 结束节点
        NodeRecord endRecord = new NodeRecord(graph.getNode("e"));
        assertTrue(endRecord.isEnd());
    }

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void testAllNodeTypes(NodeType type) {
        NodeSpec spec = new NodeSpec("test", type);
        Graph singleGraph = Graph.create("single", g -> {
            g.addNode(spec);
        });

        Node testNode = singleGraph.getNode("test");
        NodeRecord record = new NodeRecord(testNode);

        assertEquals(type, record.getType());
        assertEquals(type == NodeType.END, record.isEnd());
    }

    @Test
    void testDefaultConstructor() {
        // 测试默认构造函数（用于反序列化）
        NodeRecord record = new NodeRecord();

        assertNull(record.getGraphId());
        assertNull(record.getId());
        assertNull(record.getTitle());
        assertNull(record.getType());
        assertEquals(0, record.getTimestamp());
        assertFalse(record.isEnd());
    }

    @Test
    void testTimestamp() throws InterruptedException {
        long before = System.currentTimeMillis();
        Thread.sleep(10); // 确保时间戳不同
        NodeRecord record = new NodeRecord(node);
        long after = System.currentTimeMillis();

        assertTrue(record.getTimestamp() >= before);
        assertTrue(record.getTimestamp() <= after);
    }

    @Test
    void testDifferentNodes() {
        Node startNode = graph.getNode("s");
        Node endNode = graph.getNode("e");

        NodeRecord startRecord = new NodeRecord(startNode);
        NodeRecord endRecord = new NodeRecord(endNode);

        assertEquals("s", startRecord.getId());
        assertEquals("开始节点", startRecord.getTitle());
        assertEquals(NodeType.START, startRecord.getType());

        assertEquals("e", endRecord.getId());
        assertEquals("结束节点", endRecord.getTitle());
        assertEquals(NodeType.END, endRecord.getType());
        assertTrue(endRecord.isEnd());
    }

    @Test
    void testNodeWithoutTitle() {
        Graph noTitleGraph = Graph.create("no-title", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        Node noTitleNode = noTitleGraph.getNode("s");
        NodeRecord record = new NodeRecord(noTitleNode);

        assertNull(record.getTitle());
        assertEquals("s", record.getId());
        assertEquals(NodeType.START, record.getType());
    }
}