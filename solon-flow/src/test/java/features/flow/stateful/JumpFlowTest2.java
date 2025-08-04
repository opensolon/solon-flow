package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.Operation;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/8/4 created
 */
@SolonTest
public class JumpFlowTest2 {
    static final Logger log = LoggerFactory.getLogger(JumpFlowTest2.class);

    final String chainId = "test3";
    final String instanceId = Utils.uuid();
    final String actor = "role";

    private FlowStatefulService buildStatefulService() {
        MapContainer container = new MapContainer();

        FlowEngine fe = FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController(actor) {
                    @Override
                    public boolean isOperatable(FlowContext context, Node node) {
                        if ("admin".equals(context.get(actor))) {
                            return true;
                        }

                        return super.isOperatable(context, node);
                    }
                })
                .stateRepository(new InMemoryStateRepository())
                .container(container)
                .build());


        fe.load("classpath:flow/stateful/*.yml");

        return fe.statefulService();
    }

    @Test
    public void case1() {
        FlowStatefulService statefulService = buildStatefulService();

        statefulService.postOperation(new FlowContext(instanceId).put(actor, "admin"), chainId, "n3", Operation.FORWARD_JUMP);

        StatefulTask task = statefulService.getTask(chainId, new FlowContext(instanceId).put(actor, "admin"));

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n4");


        statefulService.postOperation(new FlowContext(instanceId).put(actor, "admin"), chainId, "n1", Operation.BACK_JUMP);

        task = statefulService.getTask(chainId, new FlowContext(instanceId).put(actor, "admin"));

        log.debug(task.toString());
        assert task.getState() == StateType.WAITING;
        assert task.getNode().getId().equals("n0");
    }

    @Test
    public void case2() {
        FlowStatefulService statefulService = buildStatefulService();

        StatefulTask task = statefulService.getTask(chainId, newContext());
        log.debug(task.toString());

        statefulService.postOperation(newContext(), task.getNode(), Operation.FORWARD);
        StatefulTask task2 = statefulService.getTask(chainId, newContext());
        log.debug(task2.toString());

        statefulService.postOperation(newContext(), task.getNode(), Operation.FORWARD);
        StatefulTask task3 = statefulService.getTask(chainId, newContext());
        log.debug(task3.toString());

        //重复提交相同节点后，获取的任务仍是相同的（说明可以重复提交）
        assert task2.getNode().getId().equals(task3.getNode().getId());
    }

    @Test
    public void case3() throws Throwable {
        FlowStatefulService statefulService = buildStatefulService();

        StatefulTask task = statefulService.getTask(chainId, newContext());
        log.debug(task.toString());

        task.runTask(new FlowContext(instanceId).put(actor, "admin"));
    }

    private FlowContext newContext() {
        return new FlowContext(instanceId).put(actor, "admin");
    }
}