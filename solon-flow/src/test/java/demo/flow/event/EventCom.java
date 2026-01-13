package demo.flow.event;

import org.noear.dami2.Dami;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowExchanger;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

/**
 *
 * @author noear 2026/1/13 created
 *
 */
public class EventCom implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        if (false == context.<Boolean>getAs("pay.succeeded")) {
            //如果未支付成功，则停止当前流程，并监听支付成功事件
            context.stop();

            Dami.bus().listen("pay.success", event -> {
                FlowExchanger exchanger = context.exchanger();
                context.put("pay.succeeded", true);

                //重新执行当前流程（即恢复执行）
                exchanger.engine().eval(exchanger.graph(), context);
            });
        } else {
            //如果已支付成功，做即定任务。或继续执行下一个节点
        }
    }
}
