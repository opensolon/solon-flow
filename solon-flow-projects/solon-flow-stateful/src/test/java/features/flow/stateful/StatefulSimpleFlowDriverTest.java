package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/3/27 created
 */
public class StatefulSimpleFlowDriverTest {
    static final Logger log = LoggerFactory.getLogger(StatefulSimpleFlowDriverTest.class);

    //初始化引擎
    StatefulFlowEngine flowEngine = buildFlowDriver();

    private StatefulFlowEngine buildFlowDriver() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        StatefulFlowEngine fe = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new SimpleStateOperator())
                .stateRepository(new InMemoryStateRepository())
                .container(container)
                .build());

        try {
            fe.load("classpath:demo/*.yml");

            return fe;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void case1() throws Throwable {
        StatefulFlowContext context;
        StatefulNode statefulNode;

        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        //二次测试
        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);

        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理


        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4");
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限


        context = getContext("陈宇");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("吕方");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("吕方");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert "step5".equals(statefulNode.getNode().getId()); //抄送节点
        assert NodeStates.UNDEFINED == statefulNode.getState();

        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);

        context = getContext("吕方");
        statefulNode = flowEngine.getActivityNode("f1", context);
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点
    }

    private StatefulFlowContext getContext(String actor) throws Throwable {
        StatefulFlowContext context = new StatefulFlowContext("i1");
        context.put("actor", actor);
        return context;
    }

    //@Test //只看看
    public void case2() throws Throwable {
        StatefulFlowContext context;
        StatefulNode statefulNode;

        context = new StatefulFlowContext("i1").put("actor", "陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);

        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交状态
        flowEngine.postNodeState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);

        context = new StatefulFlowContext("i1").put("actor", "陈鑫");
        statefulNode = flowEngine.getActivityNode("f1", context);

        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理（有权限操作）
    }
}