package features.flow.cfg_test;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author noear 2025/3/20 created
 */
public class FlowTest {
    @Test
    public void case1() throws Throwable {
        MapContainer mapContainer = new MapContainer();
        mapContainer.putComponent("a", (c, o) -> {
            ((List) c.getAs("log")).add(o.getTitle());
        });

        FlowEngine flow = FlowEngine.newInstance();
        flow.register(new SimpleFlowDriver(mapContainer));
        flow.load(Chain.parseByUri("classpath:flow/flow_case8.chain.yml"));

        FlowContext context = FlowContext.of();
        context.put("log", new ArrayList<>());
        context.put("dataType", "1");

        flow.eval("f8", context);

        String log = context.getAs("log").toString();
        System.out.println(log);
        assert "[数据预处理, 元数据填充, 瞬时数据, 构建转发数据, Http转发, Mqtt转发]".equals(log);


        System.out.println("---------------------");

        context = FlowContext.of();
        context.put("log", new ArrayList<>());
        context.put("dataType", "type1");

        flow.eval("f8", context);

         log = context.getAs("log").toString();
        System.out.println(log);
        assert "[数据预处理, 元数据填充, 汇总数据, 构建转发数据, Http转发, Mqtt转发, 汇总统计]".equals(log);
    }

    @Test
    public void for_case1() throws Throwable {
        FlowEngine flow = FlowEngine.newInstance();
        flow.load(Chain.parseByUri("classpath:flow/for_case1.chain.yml"));

        flow.eval("for_case1", FlowContext.of());
    }
}