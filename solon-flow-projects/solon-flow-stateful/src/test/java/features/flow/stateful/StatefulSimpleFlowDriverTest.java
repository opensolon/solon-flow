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

    @Test
    public void case1() throws Throwable {
        MapContainer container = new MapContainer();
        container.putComponent("oa_task", new OaTask());

        //初始化引擎
        StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new SimpleStateOperator())
                .stateRepository(new InMemoryStateRepository())
                .container(container)
                .build());

        flowEngine.load("classpath:demo/*.yml");

        StatefulFlowContext context;
        StatefulNode statefulNode;

        context = getContext("陈鑫");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        //二次测试
        context = getContext("陈鑫");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step2".equals(statefulNode.getNode().getId());
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交状态
        flowEngine.postState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);

        context = getContext("陈鑫");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理


        /// ////////////////
        //提交状态
        flowEngine.postState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("陈鑫");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4");
        assert NodeStates.UNDEFINED == statefulNode.getState(); //没有权限


        context = getContext("陈宇");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("吕跃");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");
        assert NodeStates.WAIT == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        flowEngine.postState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);


        context = getContext("吕跃");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert "step5".equals(statefulNode.getNode().getId()); //抄送节点
        assert NodeStates.UNDEFINED == statefulNode.getState();

        /// ////////////////
        //提交状态
        flowEngine.postState(context, "f1", statefulNode.getNode().getId(), NodeStates.PASS);

        context = getContext("吕跃");
        flowEngine.eval("f1", context);
        statefulNode = context.getTaskNode();
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点
    }

    private StatefulFlowContext getContext(String operator) throws Throwable {
        StatefulFlowContext context = new StatefulFlowContext("i1");
        context.put("operator", operator);
        return context;
    }
}