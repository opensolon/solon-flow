package features.flow;

import org.junit.jupiter.api.Test;
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

        links.add(new Link(null, "n1", new LinkDecl("n2").priority(1)));
        links.add(new Link(null, "n1", new LinkDecl("n3").priority(3)));
        links.add(new Link(null, "n1", new LinkDecl("n4").priority(2)));

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
        Graph graph = new GraphDecl("c1").create(decl -> {
            decl.addNode(NodeDecl.activityOf("n1").task("@DemoCom"));
        });

        engine.eval(graph, "n1");
    }
}
