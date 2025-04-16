package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author noear 2025/3/27 created
 */
@SolonTest
public class OaStatefulFlowTest {
    static final Logger log = LoggerFactory.getLogger(OaStatefulFlowTest.class);

    final String chainId = "sf1";
    final String instanceId = Utils.uuid();


    private StatefulFlowEngine buildFlowDriver() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        StatefulFlowEngine fe = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController())
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
        StatefulNode statefulNode;


        context = getContext("刘涛");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step1".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        context.put("oaState", 2); //用于扩展状态记录
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理


        /// ////////////////
        //提交状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        context = getContext(null);
        Collection<StatefulNode> nodes = flowEngine.getActivityNodes(chainId, context);
        assert nodes.size() == 2;
        assert 0 == nodes.stream().filter(n -> n.getState() == StateType.WAITING).count();

        context = getContext("陈宇");
        nodes = flowEngine.getActivityNodes(chainId, context);
        assert nodes.size() == 2;
        assert 1 == nodes.stream().filter(n -> n.getState() == StateType.WAITING).count();


        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4");
        assert StateType.UNKNOWN == statefulNode.getState(); //没有权限


        context = getContext("陈宇");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        context = getContext("吕方");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        context = getContext("吕方");
        statefulNode = flowEngine.getActivityNode(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode == null;

        flowEngine.clearState(context);
    }

    private FlowContext getContext(String actor) throws Throwable {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", actor);
        return context;
    }

//    //@Test //只看看
//    public void case2() throws Throwable {
//        FlowContext context;
//        StatefulNode statefulNode;
//
//        context = new FlowContext("i1").put("actor", "陈鑫");
//        statefulNode = flowEngine.getActivityNode(chainId, context);
//
//        assert "step2".equals(statefulNode.getNode().getId());
//        assert StateType.UNKNOWN == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）
//
//        /// ////////////////
//        //提交状态
//        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);
//
//        context = new FlowContext("i1").put("actor", "陈鑫");
//        statefulNode = flowEngine.getActivityNode(chainId, context);
//
//        assert "step3".equals(statefulNode.getNode().getId());
//        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理（有权限操作）
//    }
}