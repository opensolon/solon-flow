package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.NodeStates;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulNode;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.operator.BlockStateOperator;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/3/27 created
 */
public class AiBlockFlowTest {
    static final Logger log = LoggerFactory.getLogger(AiBlockFlowTest.class);

    final String chainId = "f1";
    final String instanceId = "i2";

    //初始化引擎
    StatefulFlowEngine flowEngine = buildFlowDriver();

    private StatefulFlowEngine buildFlowDriver() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        StatefulFlowEngine fe = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new BlockStateOperator()) //换了一个
                .stateRepository(new InMemoryStateRepository())
                .container(container)
                .build());


        fe.load("classpath:flow/*.yml");

        return fe;
    }

    @Test
    public void case1() throws Throwable {
        FlowContext context;
        StatefulNode statefulNode;

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.PASS == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.PASS == statefulNode.getState(); //等待当前用户处理


        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");
        assert NodeStates.PASS == statefulNode.getState(); //没有权限


        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");
        assert NodeStates.PASS == statefulNode.getState(); //等待当前用户处理

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert "step5".equals(statefulNode.getNode().getId()); //抄送节点
        assert NodeStates.PASS == statefulNode.getState();

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.evalStep(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点
    }
}