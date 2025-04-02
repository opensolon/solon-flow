package features.flow.stateful;

import org.noear.solon.flow.stateful.StateRecord;

/**
 * @author noear 2025/4/2 created
 */
public class StateRecordExt extends StateRecord {
    private int oaState;
    public StateRecordExt() {
        //用于反序列化
    }

    public StateRecordExt(String chainId, String nodeId, int nodeState, long created, int oaState) {
        super(chainId, nodeId, nodeState, created);
        this.oaState = oaState;
    }

    public int getOaState() {
        return oaState;
    }
}
