package demo.flow.approve;

import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.driver.*;

/**
 * @author noear 2025/1/13 created
 */
public class ApproveChainDriver extends SimpleFlowDriver {
    @Override
    public void handleTask(FlowExchanger context, Task task) throws Throwable {
        if (isChain(task.getDescription())) {
            //如果跨链调用
            tryAsChainTask(context, task, task.getDescription());
            return;
        }

        if (isComponent(task.getDescription())) {
            //如果用组件运行
            tryAsComponentTask(context, task, task.getDescription());
            return;
        }

        String instance_id = context.get("instance_id");
        String user_id = context.get("user_id");
        String role_id = context.get("role_id");


        String chain_id = task.getNode().getChain().getId();
        String task_id = task.getNode().getId();

        //把状态批量加载到上下文参考（或者通过数据库查找状态）
        TaskState taskState = null;//查询任务装态

        if (taskState == null) {
            //中断（流，不会再往下驱动），等用户操作出状态
            context.interrupt();

            //查询数据库，是否有提醒记录。如果没有，发布通知
            //...

            //如果当前用户匹配这个节点任务
            if(role_id.equals(task.getNode().getMeta("role_id"))){
                //则把这个节点，作为结果（用于展示界面）
                context.put("result", task.getNode());
            }
        }
    }
}
