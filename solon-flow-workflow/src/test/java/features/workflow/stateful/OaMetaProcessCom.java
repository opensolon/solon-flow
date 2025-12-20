package features.workflow.stateful;

import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

import java.util.Date;

/**
 * @author noear 2025/3/28 created
 */
@Component("oaMetaProcessCom")
public class OaMetaProcessCom implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        System.out.println("----------执行OaMetaProcessCom: " + new Date().getTime());
        String cc = node.getMetaAsString("cc");
        if (Utils.isNotEmpty(cc)) {
            //发送邮件...
            System.out.println("----------已抄送: " + cc + ", on node-id: " + node.getId());
        }
    }
}
