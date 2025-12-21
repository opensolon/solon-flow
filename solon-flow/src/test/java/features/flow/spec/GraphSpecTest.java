package features.flow.spec;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.GraphSpec;
import org.noear.solon.flow.NodeSpec;
import org.noear.solon.flow.NodeType;
import org.noear.solon.flow.LinkSpec;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraphSpec 属性获取方法测试类
 */
public class GraphSpecTest {

    @Test
    public void testGraphSpecGetters() {
        // 创建 GraphSpec 实例
        GraphSpec graphSpec = new GraphSpec("test-id", "test-title", "test-driver");

        // 测试基础属性获取方法
        assertEquals("test-id", graphSpec.getId());
        assertEquals("test-title", graphSpec.getTitle());
        assertEquals("test-driver", graphSpec.getDriver());

        // 测试元数据获取方法
        Map<String, Object> meta = graphSpec.getMeta();
        assertNotNull(meta);
        assertTrue(meta.isEmpty()); // 初始应为空
    }

    @Test
    public void testNodeSpecGetters() {
        // 创建 NodeSpec 实例
        NodeSpec nodeSpec = new NodeSpec("node-1", NodeType.ACTIVITY);
        nodeSpec.title("测试节点")
               .task("test.task")
               .when("${condition} == true")
               .metaPut("key1", "value1");

        // 测试 NodeSpec 获取属性方法
        assertEquals("node-1", nodeSpec.getId());
        assertEquals("测试节点", nodeSpec.getTitle());
        assertEquals(NodeType.ACTIVITY, nodeSpec.getType());
        assertEquals("${condition} == true", nodeSpec.getWhen());
        assertEquals("test.task", nodeSpec.getTask());

        // 测试元数据获取
        Map<String, Object> meta = nodeSpec.getMeta();
        assertEquals("value1", meta.get("key1"));

        // 测试连接获取
        nodeSpec.linkAdd("next-node");
        List<LinkSpec> links = nodeSpec.getLinks();
        assertEquals(1, links.size());
        assertEquals("next-node", links.get(0).getNextId());
    }

    @Test
    public void testLinkSpecGetters() {
        // 创建 LinkSpec 实例
        LinkSpec linkSpec = new LinkSpec("target-node");
        linkSpec.title("测试连接")
                .when("${age} > 18")
                .priority(10)
                .metaPut("link-key", "link-value");

        // 测试 LinkSpec 获取属性方法
        assertEquals("target-node", linkSpec.getNextId());
        assertEquals("测试连接", linkSpec.getTitle());
        assertEquals("${age} > 18", linkSpec.getWhen());
        assertEquals(10, linkSpec.getPriority());

        // 测试元数据获取
        Map<String, Object> meta = linkSpec.getMeta();
        assertEquals("link-value", meta.get("link-key"));
    }
}