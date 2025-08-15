package demo.flow.stateful;

import org.noear.solon.flow.AbstractFlowContext;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulSupporter;

/**
 * 示例
 */
public class ApprovalFlowContext extends AbstractFlowContext implements FlowContext, StatefulSupporter {
    private final String instanceId;
    public ApprovalFlowContext(String instanceId){
        super(instanceId);
        this.instanceId = instanceId;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public StatefulSupporter statefulSupporter() {
        return this;
    }


    @Override
    public boolean isOperatable(Node node) {
        return false;
    }

    @Override
    public boolean isAutoForward(Node node) {
        return StatefulSupporter.super.isAutoForward(node);
    }

    @Override
    public StateType stateGet(Node node) {
        return null;
    }

    @Override
    public void statePut(Node node, StateType state) {

    }

    @Override
    public void stateRemove(Node node) {

    }

    @Override
    public void stateClear() {

    }
}
