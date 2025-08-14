package features.flow.cfg_iterator.com;

import org.noear.solon.annotation.Component;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;


@Component("downloadBanduAttachment")
public class DownloadBanduAttachment implements TaskComponent {
    @Override
    public void run(FlowExchanger context, Node node) throws Throwable {
        String courseId = context.get("courseId");
        context.put(this.getClass().getSimpleName() + ":" + courseId, courseId);
    }
}
