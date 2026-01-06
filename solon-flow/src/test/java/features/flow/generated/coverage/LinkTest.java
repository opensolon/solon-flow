package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.noear.solon.flow.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Link 单元测试
 */
class LinkTest {

    private Graph graph;
    private LinkSpec linkSpec;

    @BeforeEach
    void setUp() {
        // 创建图
        graph = Graph.create("test-graph", spec -> {
            spec.addStart("n1").linkAdd("n2");
            spec.addActivity("n2").linkAdd("n3");
            spec.addEnd("n3");
        });

        // 创建连接规格
        linkSpec = new LinkSpec("n2");
        linkSpec.title("Test Link");
        linkSpec.when("condition == true");
        linkSpec.metaPut("key1", "value1");
        linkSpec.priority(5);
    }

    @Test
    void testLinkCreation() {
        Link link = new Link(graph, "n1", linkSpec);

        assertEquals("n1", link.getPrevId());
        assertEquals("n2", link.getNextId());
        assertEquals("Test Link", link.getTitle());
        assertEquals(graph, link.getGraph());
    }

    @Test
    void testConditionAccess() {
        Link link = new Link(graph, "n1", linkSpec);
        ConditionDesc condition = link.getWhen();

        assertNotNull(condition);
        assertEquals("condition == true", condition.getDescription());

        // 测试过时的condition方法（向后兼容）
        ConditionDesc condition2 = link.getCondition();
        assertEquals(condition, condition2);
    }

    @Test
    void testMetaDataOperations() {
        Link link = new Link(graph, "n1", linkSpec);
        Map<String, Object> metas = link.getMetas();

        assertEquals(1, metas.size());
        assertEquals("value1", link.getMeta("key1"));
        assertEquals("value1", link.getMetaAs("key1"));
        assertEquals("value1", link.getMetaOrDefault("key1", "default"));
        assertEquals("default", link.getMetaOrDefault("nonExistent", "default"));
    }

    @Test
    void testNodeResolution() {
        Link link = new Link(graph, "n1", linkSpec);

        Node prevNode = link.getPrevNode();
        Node nextNode = link.getNextNode();

        assertNotNull(prevNode);
        assertNotNull(nextNode);
        assertEquals("n1", prevNode.getId());
        assertEquals("n2", nextNode.getId());
    }

    @Test
    void testPriorityComparison() {
        LinkSpec highSpec = new LinkSpec("n2");
        highSpec.priority(10);
        Link highLink = new Link(graph, "n1", highSpec);

        LinkSpec lowSpec = new LinkSpec("n3");
        lowSpec.priority(1);
        Link lowLink = new Link(graph, "n1", lowSpec);

        // 高优先级应该排在前面
        assertTrue(highLink.compareTo(lowLink) < 0);
        assertTrue(lowLink.compareTo(highLink) > 0);

        // 相同优先级
        LinkSpec sameSpec = new LinkSpec("n2");
        sameSpec.priority(5);
        Link sameLink = new Link(graph, "n1", sameSpec);
    }

    @Test
    void testToString() {
        Link link = new Link(graph, "n1", linkSpec);
        String str = link.toString();

        assertNotNull(str);
        assertTrue(str.contains("n1"));
        assertTrue(str.contains("n2"));
        assertTrue(str.contains("Test Link"));
        assertTrue(str.contains("condition == true"));
    }

    @Test
    void testLinkWithComponentCondition() {
        ConditionComponent component = context -> true;
        LinkSpec specWithComponent = new LinkSpec("n2");
        specWithComponent.when(component);

        Link link = new Link(graph, "n1", specWithComponent);
        assertSame(component, link.getWhen().getComponent());
    }

    @Test
    void testEmptyMetaAndCondition() {
        LinkSpec emptySpec = new LinkSpec("n2");
        Link link = new Link(graph, "n1", emptySpec);

        assertNotNull(link.getMetas());
        assertTrue(link.getMetas().isEmpty());
        assertTrue(link.getWhen().isEmpty());
    }

    @Test
    void testLinkAttachment() {
        Link link = new Link(graph, "n1", linkSpec);

        // Link 类没有直接的 attachment 字段，但可以通过 ConditionDesc 的 attachment
        Object attachment = new Object();
        link.getWhen().attachment = attachment;
        assertSame(attachment, link.getWhen().attachment);
    }
}