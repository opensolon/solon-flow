package features.flow.app;

import java.util.concurrent.atomic.AtomicInteger;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.util.RunUtil;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;

@Component("asyncTaskComponent")
public class AsyncTaskComponent implements TaskComponent{

    @Override
    public void run(FlowContext context, Node node) throws Throwable {
        // System.out.println("demoTaskComponent test:" + new Date().getTime());
        // JSONObject nodeData = node.getMeta("nodeData");
        // JSONObject formData = nodeData.getJSONObject("formData");
        Integer isAsync = node.getMeta("isAsync");
        final Integer iterations = node.getMeta("iterations");
        Integer isBlock = node.getMeta("isBlock");
        /**
         * isBlock 表示该节点是堵塞节点，一般用于汇总数据；isAsync表示该节点是异步节点，一般只会存在其中一种
         * 如果isBlock和isAsync都为0，那就是普通的节点，处理完当前任务后直接进入下一步
         */
        if(isBlock!=null && isBlock==1){
            if(context.get("hasFinish")!=null && (Boolean)context.get("hasFinish")==true){
                System.out.println("demoTaskComponent block finish test:["+node.getTitle() + "]" + ((AtomicInteger)context.get("count")).get());
                //next(context,node);
                return; //不阻断，且要到下一步。就用返回（或者下一步前，加个阻断）//返回更好
            }else{
                context.interrupt();
                System.out.println("demoTaskComponent block no finish test:["+node.getTitle() + "]" + ((AtomicInteger)context.get("count")).get());
            }
        }else{
            // 异步测试
            if(isAsync!=null && isAsync==1){
            
                context.interrupt();
                RunUtil.async(() -> {
                    context.put("hasFinish", false);
                    AtomicInteger count = new AtomicInteger(0);
                    context.put("count",count);
                    int i=0;
                    while(i++ <iterations.intValue()){
                        System.out.println("demoTaskComponent async test:["+node.getTitle() + "]" + i);
                        count.incrementAndGet();
                        next(context,node);
                    }
                    context.put("hasFinish", true);
                    next(context,node);
                });
            }else{
                System.out.println("demoTaskComponent test:["+node.getTitle() + "]" + ((AtomicInteger)context.get("count")).get());
                //next(context,node);
                return; //不阻断，且要到下一步。就用返回（或者下一步前，加个阻断）//返回更好
            }
        }
    }

    private void next(FlowContext context, Node node) {
        try {
            context.next(node);
        } catch (Throwable ex) {
            ex.printStackTrace();
            context.stop(); //要不要停止？
        }
    }

}
