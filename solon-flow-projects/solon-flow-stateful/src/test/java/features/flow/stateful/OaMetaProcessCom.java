package features.flow.stateful;

import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

/**
 * @author noear 2025/3/28 created
 */
public class OaMetaProcessCom implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        String cc = node.getMeta("cc");
        if (Utils.isNotEmpty(cc)) {
            //发送邮件...
            System.out.println("----------已抄送: " + cc + ", on node-id: " + node.getId());
        }
    }
}
