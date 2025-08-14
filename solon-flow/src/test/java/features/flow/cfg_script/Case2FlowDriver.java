package features.flow.cfg_script;

import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.stateless.StatelessFlowDriver;

/**
 * @author noear 2025/1/11 created
 */
public class Case2FlowDriver extends StatelessFlowDriver {
    @Override
    public void handleTask(FlowExchanger context, Task task) throws Throwable {
        context.put("result", task.getNode().getId());
        if(task.getNode().getId().equals("n-3")) {
            context.interrupt();
            return;
        }

        super.handleTask(context, task);
    }
}
