package demo.flow.stateful;

import org.noear.solon.flow.AbstractFlowContext;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulSupporter;
import org.noear.solon.flow.stateful.controller.ActorStateController;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例
 */
public class ApprovalFlowContext extends AbstractFlowContext implements FlowContext, StatefulSupporter {
    private final String instanceId;
    private final ActorStateController stateController = new ActorStateController("actor");
    private final Map<String, StateType> stateMap = new HashMap<>();

    public ApprovalFlowContext(String instanceId) {
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
        return stateController.isOperatable(this, node);
    }

    @Override
    public boolean isAutoForward(Node node) {
        return stateController.isAutoForward(this, node);
    }

    public Map<String, StateType> stateMap() {
        return stateMap;
    }

    @Override
    public StateType stateGet(Node node) {
        return stateMap.get(node.getId());
    }

    @Override
    public void statePut(Node node, StateType state) {
        stateMap.put(node.getId(), state);
    }

    @Override
    public void stateRemove(Node node) {
        stateMap.remove(node.getId());
    }

    @Override
    public void stateClear() {
        stateMap.clear();
    }
}
