package demo.flow.stateful;

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.stateful.StateRecord;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulNode;

import java.util.List;

/**
 * @author noear 2025/3/27 created
 */
@Controller
public class DemoController {
    @Inject
    StatefulFlowEngine flowEngine;

    //操作展示
    @Mapping("display")
    public ModelAndView displayFlow(Context ctx, String instanceId, String chainId) throws Throwable {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", ctx.param("actor"));

        //获取展示节点及装态
        StatefulNode activityNode = flowEngine.getActivityNode(chainId, context);// if null: 界面显示只读; no null: 界面显示操作：同意，拒绝，撤回到上一节点，撤回到起始节点（给发起人）
        //获得历史状态记录
        List<StateRecord> records = flowEngine.getRepository().getStateRecords(context);
        return null;
    }

    //操作提交
    @Mapping("post")
    public void postFlow(Context ctx, String instanceId, String chainId, String nodeId, int nodeState) throws Throwable {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", ctx.param("actor"));

        flowEngine.postActivityState(context, chainId, nodeId, nodeState);
    }
}