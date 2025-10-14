package features.flow.decl;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.ChainDecl;
import org.noear.solon.flow.NodeDecl;
import org.noear.solon.flow.NodeType;
import org.noear.solon.flow.LinkDecl;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChainDecl 属性获取方法测试类
 */
public class ChainDeclTest {

    @Test
    public void testChainDeclGetters() {
        // 创建 ChainDecl 实例
        ChainDecl chainDecl = new ChainDecl("test-id", "test-title", "test-driver");

        // 测试基础属性获取方法
        assertEquals("test-id", chainDecl.getId());
        assertEquals("test-title", chainDecl.getTitle());
        assertEquals("test-driver", chainDecl.getDriver());

        // 测试元数据获取方法
        Map<String, Object> meta = chainDecl.getMeta();
        assertNotNull(meta);
        assertTrue(meta.isEmpty()); // 初始应为空
    }

    @Test
    public void testNodeDeclGetters() {
        // 创建 NodeDecl 实例
        NodeDecl nodeDecl = new NodeDecl("node-1", NodeType.ACTIVITY);
        nodeDecl.title("测试节点")
               .task("test.task")
               .when("${condition} == true")
               .metaPut("key1", "value1");

        // 测试 NodeDecl 获取属性方法
        assertEquals("node-1", nodeDecl.getId());
        assertEquals("测试节点", nodeDecl.getTitle());
        assertEquals(NodeType.ACTIVITY, nodeDecl.getType());
        assertEquals("${condition} == true", nodeDecl.getWhen());
        assertEquals("test.task", nodeDecl.getTask());

        // 测试元数据获取
        Map<String, Object> meta = nodeDecl.getMeta();
        assertEquals("value1", meta.get("key1"));

        // 测试链接获取
        nodeDecl.linkAdd("next-node");
        List<LinkDecl> links = nodeDecl.getLinks();
        assertEquals(1, links.size());
        assertEquals("next-node", links.get(0).getNextId());
    }

    @Test
    public void testLinkDeclGetters() {
        // 创建 LinkDecl 实例
        LinkDecl linkDecl = new LinkDecl("target-node");
        linkDecl.title("测试链接")
                .when("${age} > 18")
                .priority(10)
                .metaPut("link-key", "link-value");

        // 测试 LinkDecl 获取属性方法
        assertEquals("target-node", linkDecl.getNextId());
        assertEquals("测试链接", linkDecl.getTitle());
        assertEquals("${age} > 18", linkDecl.getWhen());
        assertEquals(10, linkDecl.getPriority());

        // 测试元数据获取
        Map<String, Object> meta = linkDecl.getMeta();
        assertEquals("link-value", meta.get("link-key"));
    }
}