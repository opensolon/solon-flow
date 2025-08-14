package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/3/27 created
 */
public class AiBlockFlowTest {
    static final Logger log = LoggerFactory.getLogger(AiBlockFlowTest.class);

    final String chainId = "sf1";
    final String instanceId = "i2";

    private FlowStatefulService buildStatefulService() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        FlowEngine fe = FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController(){
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

        return fe.statefulService();
    }

    @Test
    public void case1() throws Throwable {
        //初始化引擎
        FlowStatefulService statefulService = buildStatefulService();

        FlowContext context;
        StatefulTask statefulNode;

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step1".equals(statefulNode.getNode().getId());

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());


        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");


        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert "step5".equals(statefulNode.getNode().getId()); //抄送节点

        /// ////////////////

        context = new FlowContext(instanceId);
        statefulNode = statefulService.stepForward(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点
    }
}