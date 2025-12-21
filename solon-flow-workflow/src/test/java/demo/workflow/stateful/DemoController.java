package demo.workflow.stateful;

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.workflow.StateController;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.StateRepository;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.WorkflowService;

/**
 * @author noear 2025/3/27 created
 */
@Controller
public class DemoController {
    @Inject
    WorkflowService workflow;
    @Inject
    StateRepository stateRepository;
    @Inject
    StateController stateController;

    //操作展示
    @Mapping("display")
    public ModelAndView displayFlow(Context ctx, String instanceId, String graphId) throws Throwable {
        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", ctx.param("actor"));

        //获取展示节点及装态
        Task task = workflow.getTask(graphId, context);// if null: 界面显示只读; no null: 界面显示操作：同意，拒绝，撤回到上一节点，撤回到起始节点（给发起人）
        return null;
    }

    //操作提交
    @Mapping("post")
    public void postFlow(Context ctx, String instanceId, String graphId, String nodeId, int action) throws Throwable {
        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", ctx.param("actor"));

        workflow.postTask(graphId, nodeId, TaskAction.codeOf(action), context);
    }
}