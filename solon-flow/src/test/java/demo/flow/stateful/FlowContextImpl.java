package demo.flow.stateful;

import org.noear.solon.flow.AbstractFlowContext;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulSupporter;

/**
 *
 * @author noear 2025/8/15 created
 *
 */
public class FlowContextImpl extends AbstractFlowContext implements FlowContext, StatefulSupporter {
    @Override
    public boolean isStateful() {
        return false;
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
