package features.flow.cfg_script;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Task;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * @author noear 2025/1/11 created
 */
public class Case2FlowDriver extends SimpleFlowDriver {
    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        context.result = task.getNode().getId();
        if(task.getNode().getId().equals("n-3")) {
            context.interrupt();
            return;
        }

        super.handleTask(context, task);
    }
}
