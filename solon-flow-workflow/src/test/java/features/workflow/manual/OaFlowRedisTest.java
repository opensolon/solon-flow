package features.workflow.manual;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.workflow.*;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.RedisStateRepository;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/3/27 created
 */
@SolonTest
public class OaFlowRedisTest {
    static final Logger log = LoggerFactory.getLogger(OaFlowRedisTest.class);

    final String graphId = "sf1";
    final String instanceId = Utils.uuid();

    ActorStateController stateController;
    RedisStateRepository stateRepository;


    private WorkflowExecutor buildWorkflow() {
        MapContainer container = new MapContainer();
        container.putComponent("OaMetaProcessCom", new OaMetaProcessCom());


        // 创建 Redis 客户端
        RedisClient redisClient = Solon.cfg().getBean("solon.repo.redis", RedisClient.class);
        if (redisClient == null) {
            throw new IllegalStateException("Redis client configuration not found!");
        }

        if (stateController == null) {
            stateController = new ActorStateController();
            stateRepository = new RedisStateRepository(redisClient);
        }

        FlowEngine fe = FlowEngine.newInstance(SimpleFlowDriver.builder()
                .container(container)
                .build());


        fe.load("classpath:flow/*.yml");

        return WorkflowExecutor.of(fe, stateController, stateRepository);
    }

    @Test
    public void case1() throws Throwable {
        //初始化引擎
        WorkflowExecutor workflow = buildWorkflow();

        FlowContext context;
        Task task;

        context = getContext("刘涛");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step1".equals(task.getNode().getId());
        assert TaskState.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        workflow.submitTask(task, TaskAction.FORWARD, context);


        context = getContext("陈鑫");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step3".equals(task.getNode().getId());
        assert TaskState.WAITING == task.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert "step3".equals(task.getNode().getId());
        assert TaskState.WAITING == task.getState(); //等待当前用户处理


        /// ////////////////
        //提交状态
        workflow.submitTask(task, TaskAction.FORWARD, context);


        context = getContext("陈鑫");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        Assertions.assertNull(task); //没有权限


        context = getContext("陈宇");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert task.getNode().getId().startsWith("step4_1");
        assert TaskState.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        workflow.submitTask(task, TaskAction.FORWARD, context);


        context = getContext("吕方");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task != null;
        assert task.getNode().getId().startsWith("step4_2");
        assert TaskState.WAITING == task.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        workflow.submitTask(task, TaskAction.FORWARD, context);


        context = getContext("吕方");
        task = workflow.matchTask(graphId, context);
        log.warn("{}", task);
        assert task == null; //抄送节点

        workflow.stateRepository().stateClear(context);
    }

    private FlowContext getContext(String actor) throws Throwable {
        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", actor);
        return context;
    }

    //@Test //只看看
    public void case2() throws Throwable {
        FlowContext context = FlowContext.of("i1").put("actor", "陈鑫");
        Task task;

        //初始化引擎
        WorkflowExecutor workflow = buildWorkflow();


        task = workflow.matchTask(graphId, context);

        assert "step2".equals(task.getNode().getId());
        assert TaskState.UNKNOWN == task.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交操作
        workflow.submitTask(task, TaskAction.FORWARD, context);

        task = workflow.matchTask(graphId, context);

        assert "step3".equals(task.getNode().getId());
        assert TaskState.WAITING == task.getState(); //等待当前用户处理（有权限操作）
    }
}