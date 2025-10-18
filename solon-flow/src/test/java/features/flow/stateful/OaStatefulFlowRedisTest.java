package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.RedisStateRepository;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/3/27 created
 */
@SolonTest
public class OaStatefulFlowRedisTest {
    static final Logger log = LoggerFactory.getLogger(OaStatefulFlowRedisTest.class);

    final String chainId = "sf1";
    final String instanceId = Utils.uuid();

    ActorStateController stateController;
    RedisStateRepository stateRepository;


    private FlowStatefulService buildStatefulService() {
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

        return fe.forStateful();
    }

    @Test
    public void case1() throws Throwable {
        //初始化引擎
        FlowStatefulService statefulService = buildStatefulService();

        FlowContext context;
        StatefulTask statefulNode;

        context = getContext("刘涛");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step1".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getContext("陈鑫");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        //二次测试
        context = getContext("陈鑫");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理


        /// ////////////////
        //提交状态
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getContext("陈鑫");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4");
        assert StateType.UNKNOWN == statefulNode.getState(); //没有权限


        context = getContext("陈宇");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_1");
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getContext("吕方");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().startsWith("step4_2");
        assert StateType.WAITING == statefulNode.getState(); //等待当前用户处理

        /// ////////////////
        //提交状态
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        context = getContext("吕方");
        statefulNode = statefulService.getTask(chainId, context);
        log.warn("{}", statefulNode);
        assert statefulNode == null; //抄送节点

        statefulService.clearState(chainId, context);
    }

    private FlowContext getContext(String actor) throws Throwable {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", actor);
        return context;
    }

    //@Test //只看看
    public void case2() throws Throwable {
        FlowContext context = FlowContext.of("i1", stateController, stateRepository).put("actor", "陈鑫");
        StatefulTask statefulTask;

        //初始化引擎
        FlowStatefulService statefulService = buildStatefulService();


        statefulTask = statefulService.getTask(chainId, context);

        assert "step2".equals(statefulTask.getNode().getId());
        assert StateType.UNKNOWN == statefulTask.getState(); //没有权限启动任务（因为没有配置操作员）

        /// ////////////////
        //提交操作
        statefulService.postOperation(statefulTask.getNode(), Operation.FORWARD, context);

        statefulTask = statefulService.getTask(chainId, context);

        assert "step3".equals(statefulTask.getNode().getId());
        assert StateType.WAITING == statefulTask.getState(); //等待当前用户处理（有权限操作）
    }
}