package features.flow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.noear.solon.flow.*;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 连接优先级排序测试
 *
 * @author noear 2025/1/11 created
 */
public class LinkTest {
    @Test
    public void case1() {
        List<Link> links = new ArrayList<>();

        links.add(new Link(null, "n1", new LinkSpec("n2").priority(1)));
        links.add(new Link(null, "n1", new LinkSpec("n3").priority(3)));
        links.add(new Link(null, "n1", new LinkSpec("n4").priority(2)));

        Collections.sort(links);

        System.out.println(links);
    }

    @Test
    public void case2() {
        //构建组件容器
        MapContainer container = new MapContainer();
        container.putComponent("DemoCom", (ctx, node) -> {
            System.out.println(node.getId());
        });

        //构建驱动
        SimpleFlowDriver flowDriver = new SimpleFlowDriver(container);

        //构建引擎
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(flowDriver);

        //-----

        //动态构建图，并执行
        Graph graph = new GraphSpec("c1").create(spec -> {
            spec.addNode(NodeSpec.activityOf("n1").task("@DemoCom"));
        });

        engine.eval(graph, "n1");
    }

    @Test
    public void testLinkRemove() {
        // 创建节点并添加多个连接
        NodeSpec node = new NodeSpec("n1", NodeType.ACTIVITY);
        node.linkAdd("n2");
        node.linkAdd("n3");
        node.linkAdd("n4");

        // 验证初始连接数量
        assertEquals(3, node.getLinks().size());

        // 移除指定连接
        node.linkRemove("n3");

        // 验证连接数量减少
        assertEquals(2, node.getLinks().size());

        // 验证正确的连接被移除
        assertEquals("n2", node.getLinks().get(0).getNextId());
        assertEquals("n4", node.getLinks().get(1).getNextId());

        // 测试移除不存在的连接
        node.linkRemove("n5"); // 不应该抛出异常
        assertEquals(2, node.getLinks().size());

        // 测试移除所有连接
        node.linkRemove("n2");
        node.linkRemove("n4");
        assertEquals(0, node.getLinks().size());
    }

    @Test
    public void testLinkRemoveWithGraph() {
        // 构建组件容器
        MapContainer container = new MapContainer();
        container.putComponent("DemoCom", (ctx, node) -> {
            System.out.println("执行节点: " + node.getId());
        });

        // 构建驱动和引擎
        SimpleFlowDriver flowDriver = new SimpleFlowDriver(container);
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(flowDriver);

        // 动态构建图，包含多个连接
        GraphSpec graph = new GraphSpec("test-graph").then(spec -> {
            spec.addNode(NodeSpec.activityOf("n1").task("@DemoCom"));
            spec.addNode(NodeSpec.activityOf("n2").task("@DemoCom"));
            spec.addNode(NodeSpec.activityOf("n3").task("@DemoCom"));
            spec.addNode(NodeSpec.activityOf("n4").task("@DemoCom"));

            // n1 连接到 n2, n3, n4
            spec.getNode("n1").linkAdd("n2");
            spec.getNode("n1").linkAdd("n3");
            spec.getNode("n1").linkAdd("n4");
        });

        // 验证初始状态
        NodeSpec n1 = graph.getNodes().values().stream()
                .filter(n -> "n1".equals(n.getId()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(n1);
        assertEquals(3, n1.getLinks().size());

        // 移除一个连接
        n1.linkRemove("n3");
        assertEquals(2, n1.getLinks().size());

        // 验证移除后的连接
        assertTrue(n1.getLinks().stream().anyMatch(l -> "n2".equals(l.getNextId())));
        assertTrue(n1.getLinks().stream().anyMatch(l -> "n4".equals(l.getNextId())));
        assertFalse(n1.getLinks().stream().anyMatch(l -> "n3".equals(l.getNextId())));
    }
}
