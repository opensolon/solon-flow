package features.flow.cfg_script;

import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.TaskDesc;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * @author noear 2025/1/11 created
 */
public class Case2FlowDriver extends SimpleFlowDriver {
    @Override
    public void handleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
        exchanger.context().put("result", task.getNode().getId());
        if(task.getNode().getId().equals("n-3")) {
            exchanger.interrupt();
            return;
        }

        super.handleTask(exchanger, task);
    }
}
