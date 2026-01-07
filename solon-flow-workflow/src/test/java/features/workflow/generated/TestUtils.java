package features.workflow.generated;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.UUID;

/**
 * 测试工具类
 */
class TestUtils {

    /**
     * 创建简单的线性流程图
     */
    static Graph createLinearGraph(String id, int taskCount) {
        return Graph.create(id, spec -> {
            spec.addStart("start").title("开始");

            String prevId = "start";
            for (int i = 1; i <= taskCount; i++) {
                String taskId = "task" + i;
                spec.addActivity(taskId).title("任务" + i)
                        .metaPut("actor", "user" + ((i % 2) + 1));

                // 更新前一个节点的连接
                spec.getNode(prevId).linkRemove(prevId.equals("start") ? null : "task" + (i-1))
                        .linkAdd(taskId);

                prevId = taskId;
            }

            spec.addEnd("end").title("结束");
            spec.getNode(prevId).linkAdd("end");
        });
    }

    /**
     * 创建带条件的审批流程图
     */
    static Graph createConditionalGraph(String id) {
        return Graph.create(id, spec -> {
            spec.addStart("start").linkAdd("apply");

            spec.addActivity("apply").title("申请")
                    .metaPut("actor", "applicant")
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .linkAdd("approve", link -> link.when("${amount} <= 5000").title("小额审批"))
                    .linkAdd("manager-review", link -> link.when("${amount} > 5000").title("大额审批"));

            spec.addActivity("approve").title("直接批准")
                    .linkAdd("end");

            spec.addActivity("manager-review").title("经理审批")
                    .metaPut("actor", "manager")
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });
    }

    /**
     * 创建测试用的工作流服务
     */
    static WorkflowExecutor createTestworkflowExecutor(FlowEngine engine) {
        return WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );
    }

    /**
     * 创建唯一的测试上下文
     */
    static FlowContext createUniqueContext() {
        return FlowContext.of("test-" + UUID.randomUUID().toString().substring(0, 8));
    }
}