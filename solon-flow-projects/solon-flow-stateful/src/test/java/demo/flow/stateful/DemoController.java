package demo.flow.stateful;

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateRecord;
import org.noear.solon.flow.stateful.StatefulFlowContext;
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
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        context.put("operator", ctx.param("operator"));

        flowEngine.eval(chainId, context);

        //获取展示节点及装态
        StatefulNode activityNode = context.getActivityNode(); // if null: 界面显示只读; no null: 界面显示操作：同意，拒绝，撤回到上一节点，撤回到起始节点（给发起人）
        //获得历史状态记录
        List<StateRecord> records = flowEngine.getStateRecords(context);
        return null;
    }

    //操作提交
    @Mapping("post")
    public void postFlow(Context ctx, String instanceId, String chainId, String nodeId, int nodeState) throws Throwable {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        context.put("operator", ctx.param("operator"));

        Node node = flowEngine.getChain(chainId).getNode(nodeId);
        flowEngine.postNodeState(context, node, nodeState);
    }
}