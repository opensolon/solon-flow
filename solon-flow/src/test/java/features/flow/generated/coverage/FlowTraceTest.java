package features.flow.generated.coverage;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import org.noear.solon.flow.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowTrace 单元测试
 */
class FlowTraceTest {

    private FlowTrace trace;
    private Graph graph;

    @BeforeEach
    void setUp() {
        trace = new FlowTrace();
        graph = Graph.create("test-graph", spec -> {
            spec.addStart("s").title("开始").linkAdd("a1");
            spec.addActivity("a1").title("活动1").linkAdd("e");
            spec.addEnd("e").title("结束");
        });
    }

    @Test
    void testEnabledByDefault() {
        assertTrue(trace.isEnabled());
    }

    @Test
    void testEnableDisable() {
        trace.enable(false);
        assertFalse(trace.isEnabled());

        trace.enable(true);
        assertTrue(trace.isEnabled());
    }

    @Test
    void testRecordNodeId() {
        trace.recordNodeId(graph, "s");

        Collection<NodeRecord> records = trace.lastRecords();
        assertEquals(1, records.size());

        NodeRecord record = records.iterator().next();
        assertEquals("test-graph", record.getGraphId());
        assertEquals("s", record.getId());
        assertEquals("开始", record.getTitle());
        assertEquals(NodeType.START, record.getType());
    }

    @Test
    void testRecordNode() {
        Node node = graph.getNode("a1");
        trace.recordNode(graph, node);

        NodeRecord record = trace.lastRecord("test-graph");
        assertNotNull(record);
        assertEquals("test-graph", record.getGraphId());
        assertEquals("a1", record.getId());
        assertEquals("活动1", record.getTitle());
        assertEquals(NodeType.ACTIVITY, record.getType());
        assertTrue(record.getTimestamp() > 0);
    }

    @Test
    void testRecordNullNode() {
        trace.recordNode(graph, graph.getNode("a1"));
        assertNotNull(trace.lastRecord("test-graph"));

        trace.recordNode(graph, null);
        assertNull(trace.lastRecord("test-graph"));
    }

    @Test
    void testRecordNodeIdNull() {
        trace.recordNodeId(graph, "a1");
        assertNotNull(trace.lastRecord("test-graph"));

        trace.recordNodeId(graph, null);
        assertNull(trace.lastRecord("test-graph"));
    }

    @Test
    void testLastRecord() {
        // 初始为null
        assertNull(trace.lastRecord("test-graph"));

        // 记录后可以获取
        trace.recordNode(graph, graph.getNode("a1"));
        NodeRecord record = trace.lastRecord("test-graph");
        assertNotNull(record);
        assertEquals("a1", record.getId());

        // 禁用跟踪后返回null
        trace.enable(false);
        assertNull(trace.lastRecord("test-graph"));
    }

    @Test
    void testLastNode() {
        // 初始返回开始节点
        Node lastNode = trace.lastNode(graph);
        assertEquals("s", lastNode.getId());

        // 记录后返回记录的节点
        trace.recordNode(graph, graph.getNode("a1"));
        lastNode = trace.lastNode(graph);
        assertEquals("a1", lastNode.getId());

        // 禁用跟踪后返回开始节点
        trace.enable(false);
        lastNode = trace.lastNode(graph);
        assertEquals("s", lastNode.getId());
    }

    @Test
    void testLastNodeId() {
        // 初始为null
        assertNull(trace.lastNodeId("test-graph"));

        // 记录后返回节点ID
        trace.recordNode(graph, graph.getNode("a1"));
        assertEquals("a1", trace.lastNodeId("test-graph"));

        // 禁用跟踪后返回null
        trace.enable(false);
        assertNull(trace.lastNodeId("test-graph"));
    }

    @Test
    void testIsEnd() {
        // 记录非结束节点
        trace.recordNode(graph, graph.getNode("a1"));
        assertFalse(trace.isEnd("test-graph"));

        // 记录结束节点
        trace.recordNode(graph, graph.getNode("e"));
        assertTrue(trace.isEnd("test-graph"));

        // 禁用跟踪后返回false
        trace.enable(false);
        assertFalse(trace.isEnd("test-graph"));
    }

    @Test
    void testRootGraphId() {
        // 初始为null
        assertNull(trace.lastRecord(null));

        // 记录第一个节点时设置根图ID
        trace.recordNode(graph, graph.getNode("a1"));
        assertNotNull(trace.lastRecord(null)); // null使用根图ID

        // 另一个图
        Graph graph2 = Graph.create("graph2", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        trace.recordNode(graph2, graph2.getNode("s"));
        assertEquals("test-graph", trace.lastRecord(null).getGraphId()); // 根图ID不变
    }

    @Test
    void testClear() {
        trace.recordNode(graph, graph.getNode("a1"));
        assertNotNull(trace.lastRecord("test-graph"));

        trace.clear();
        assertNull(trace.lastRecord("test-graph"));
    }

    @Test
    void testLastRecordsCollection() {
        // 初始为空
        assertTrue(trace.lastRecords().isEmpty());

        // 记录多个图的节点
        trace.recordNode(graph, graph.getNode("a1"));

        Graph graph2 = Graph.create("graph2", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });
        trace.recordNode(graph2, graph2.getNode("s"));

        Collection<NodeRecord> records = trace.lastRecords();
        assertEquals(2, records.size());

        // 禁用跟踪后返回空集合
        trace.enable(false);
        assertFalse(trace.isEnabled());
    }

    @Test
    void testNullGraphHandling() {
        assertThrows(NullPointerException.class, () -> {
            trace.recordNode(null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            trace.recordNodeId(null, "node");
        });
    }
}