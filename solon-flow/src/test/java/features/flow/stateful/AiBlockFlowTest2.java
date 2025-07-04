package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulFlowEngineDefault;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试在断点操控场景下，ActorStateController 与 BlockStateController 的区别
 *
 * @author noear 2025/3/27 created
 */
public class AiBlockFlowTest2 {
    static final Logger log = LoggerFactory.getLogger(AiBlockFlowTest2.class);

    final String chainId = "sf1";
    final String instanceId = "i2";

    private StatefulFlowEngine buildFlowDriver() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        StatefulFlowEngine fe = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController(){
                    @Override
                    public boolean isAutoForward(FlowContext context, Node node) {
                        return super.isAutoForward(context, node)
                                || node.getMetaOrDefault("auto",false)
                                || context.getOrDefault("all_auto",false);
                    }
                }) //换了一个
                .stateRepository(new InMemoryStateRepository())
                .container(container)
                .build());


        fe.load("classpath:flow/*.yml");

        return fe;
    }

    @Test
    public void case1() throws Throwable {
        //初始化引擎
        StatefulFlowEngine flowEngine = buildFlowDriver();

        FlowContext context;
        StatefulTask statefulNode;

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step1".equals(statefulNode.getNode().getId());

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step4_2".equals(statefulNode.getNode().getId());


        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step4_1".equals(statefulNode.getNode().getId());


        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = flowEngine.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点
    }
}