package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Node 单元测试
 */
class NodeTest {

    private Graph graph;
    private NodeSpec nodeSpec;
    private List<LinkSpec> links;

    @BeforeEach
    void setUp() {
        // 创建一个简单的图
        graph = Graph.create("test-graph", spec -> {
            spec.addStart("s").linkAdd("a1");
            spec.addActivity("a1").linkAdd("a2");
            spec.addActivity("a2").linkAdd("e");
            spec.addEnd("e");
        });

        // 创建节点规格
        nodeSpec = NodeSpec.activityOf("test-node");
        nodeSpec.title("Test Node");
        nodeSpec.metaPut("key1", "value1");
        nodeSpec.metaPut("key2", 123);
        nodeSpec.when("condition == true");
        nodeSpec.task("task1()");

        // 创建连接
        links = new ArrayList<>();
        LinkSpec linkSpec = new LinkSpec("next-node");
        linkSpec.title("Test Link");
        linkSpec.when("linkCondition == true");
        linkSpec.metaPut("linkKey", "linkValue");
        links.add(linkSpec);
    }

    @Test
    void testLinksAndNodesRelationships() {
        // 创建一个更复杂的图来测试节点关系
        Graph complexGraph = Graph.create("complex-graph", spec -> {
            spec.addStart("s").linkAdd("a1");
            spec.addActivity("a1").linkAdd("a2").linkAdd("a3");
            spec.addActivity("a2").linkAdd("e");
            spec.addActivity("a3").linkAdd("e");
            spec.addEnd("e");
        });

        Node a1 = complexGraph.getNode("a1");
        Node a2 = complexGraph.getNode("a2");
        Node a3 = complexGraph.getNode("a3");

        // 测试前向连接
        List<Link> nextLinks = a1.getNextLinks();
        assertEquals(2, nextLinks.size());

        // 测试后向节点
        List<Node> nextNodes = a1.getNextNodes();
        assertEquals(2, nextNodes.size());
        assertTrue(nextNodes.contains(a2) || nextNodes.contains(a3));

        // 测试单个下一个节点
        Node singleNext = a2.getNextNode();
        assertEquals("e", singleNext.getId());

        // 测试前向节点
        List<Node> prevNodes = a2.getPrevNodes();
        assertEquals(1, prevNodes.size());
        assertEquals("a1", prevNodes.get(0).getId());

        // 测试前向连接
        List<Link> prevLinks = a2.getPrevLinks();
        assertEquals(1, prevLinks.size());
        assertEquals("a1", prevLinks.get(0).getPrevId());
    }

    @Test
    void testStartAndEndNodeSpecialCases() {
        Graph testGraph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("e");
            spec.addEnd("e");
        });

        Node startNode = testGraph.getNode("s");
        Node endNode = testGraph.getNode("e");

        // 开始节点没有前向连接和节点
        assertTrue(startNode.getPrevLinks().isEmpty());
        assertTrue(startNode.getPrevNodes().isEmpty());

        // 结束节点没有后向连接和节点
        assertTrue(endNode.getNextLinks().isEmpty());
        assertTrue(endNode.getNextNodes().isEmpty());
        assertNull(endNode.getNextNode());
    }
}