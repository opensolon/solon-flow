package demo.workflow.stateful;

import org.noear.redisx.RedisClient;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.repository.RedisStateRepository;

import java.util.List;

/**
 * @author noear 2025/4/7 created
 */
public class RedisStateRepositoryEx extends RedisStateRepository {
    public RedisStateRepositoryEx(RedisClient client) {
        super(client);
    }

    public RedisStateRepositoryEx(RedisClient client, String statePrefix) {
        super(client, statePrefix);
    }

    /**
     * 提交活动状态时
     */
    public void onPostOperation(FlowContext context, Node node, TaskAction operation) {
        String instanceId = context.getInstanceId();
        String graphId = node.getGraph().getId();
        String activityNodeId = node.getId();
        String actor = context.getAs("actor"); //需要什么通过 context 传递
        long created = System.currentTimeMillis();

        //业务保存
    }

    /**
     * 获取活动状态列表
     */
    public List getActivityStateList(FlowContext context) {
        String instanceId = context.getInstanceId();
        return null; //业务查询
    }
}