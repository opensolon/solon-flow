package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.driver.SimpleFlowDriver;
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

    final String graphId = "sf1";
    final String instanceId = Utils.uuid();

    ActorStateController stateController = new ActorStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    private FlowStatefulService buildStatefulService() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());

        FlowEngine fe = FlowEngine.newInstance(SimpleFlowDriver.builder()
                .container(container)
                .build());


        fe.load("classpath:flow/*.yml");

        return fe.forStateful();
    }

    @Test
    public void case1() throws Throwable {
        //初始化引擎
        FlowStatefulService statefulService = buildStatefulService();

        FlowContext context;
        StatefulTask task;


        context = getContext("刘涛");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step1".equals(task.getNode().getId());
        assert StateType.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        context.put("oaState", 2); //用于扩展状态记录
        statefulService.postTask(task.getNode(), StateOp.FORWARD, context);


        context = getContext("陈鑫");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step3".equals(task.getNode().getId());
        assert StateType.WAITING == task.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step3".equals(task.getNode().getId());
        assert StateType.WAITING == task.getState(); //等待当前用户处理



        /// ////////////////

        String yaml = statefulService.engine().getGraph(graphId).toYaml(context);
        System.out.println("------------");
        System.out.println(yaml);
        System.out.println("------------");


        /// ////////////////
        //提交状态
        statefulService.postTask(task.getNode(), StateOp.FORWARD, context);


        context = getContext(null);
        Collection<StatefulTask> nodes = statefulService.getTasks(graphId, context);
        assert nodes.size() == 2;
        assert 0 == nodes.stream().filter(n -> n.getState() == StateType.WAITING).count();

        context = getContext("陈宇");
        nodes = statefulService.getTasks(graphId, context);
        assert nodes.size() == 2;
        assert 1 == nodes.stream().filter(n -> n.getState() == StateType.WAITING).count();


        context = getContext("陈鑫");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert task.getNode().getId().startsWith("step4");
        assert StateType.UNKNOWN == task.getState(); //没有权限


        /// ////////////////

        yaml = statefulService.engine().getGraph(graphId).toYaml(context);
        System.out.println("------------");
        System.out.println(yaml);
        System.out.println("------------");


        context = getContext("陈宇");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert task.getNode().getId().startsWith("step4_1");
        assert StateType.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        statefulService.postTask(task.getNode(), StateOp.FORWARD, context);


        context = getContext("吕方");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert task.getNode().getId().startsWith("step4_2");
        assert StateType.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        statefulService.postTask(task.getNode(), StateOp.FORWARD, context);


        context = getContext("吕方");
        task = statefulService.getTask(graphId, context);
        log.warn("{}", task);
        assert task == null;

        statefulService.clearState(graphId, context);
    }

    private FlowContext getContext(String actor) throws Throwable {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", actor);
        return context;
    }

    //@Test //只看看
    public void case2() throws Throwable {
        FlowContext context = FlowContext.of("i1", stateController, stateRepository).put("actor", "陈鑫");
        StatefulTask statefulNode;

        //初始化引擎
        FlowStatefulService statefulService = buildStatefulService();

        statefulNode = statefulService.getTask(graphId, context);

        assert "step2".equals(statefulNode.getNode().getId());
        assert StateType.UNKNOWN == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交操作
        statefulService.postTask(statefulNode.getNode(), StateOp.FORWARD, context);

        statefulNode = statefulService.getTask(graphId, context);

        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理（有权限操作）
    }
}