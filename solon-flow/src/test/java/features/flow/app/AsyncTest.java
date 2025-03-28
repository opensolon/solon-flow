package features.flow.app;

import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.test.SolonTest;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.FlowEngineDefault;
import org.noear.solon.flow.NodeDecl;
import org.noear.solon.flow.NodeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SolonTest
public class AsyncTest {
    @Inject
    private AsyncTaskComponent asyncTaskComponent;

    @Test
    public void case7_1() throws Throwable {
        
        FlowEngine flowEngine = new FlowEngineDefault();
        Chain chain = build();
        FlowContext context = new FlowContext();

        // 初始化NodeTransData
        NodeTransData nodeTransData = new NodeTransData();
        context.put("nodeTransData", nodeTransData);
        flowEngine.eval(chain, context);
    }

    private Chain build() {
        NodeDecl nodeDecl = null;
        Chain chain = new Chain("TEST_CASE_007", "TEST_CASE_007");

        chain.addNode(new NodeDecl("01", NodeType.start).linkAdd("02"));

        nodeDecl = new NodeDecl("02", NodeType.activity);
        nodeDecl.task("@asyncTaskComponent");
        nodeDecl.title("视频同步切片（异步）");
        nodeDecl.metaPut("isAsync", 1); // 异步任务
        nodeDecl.metaPut("isBlock", 0); // 堵塞任务
        nodeDecl.metaPut("iterations", 5); // 迭代次数
        chain.addNode(nodeDecl.linkAdd("03"));

        nodeDecl = new NodeDecl("03", NodeType.activity);
        nodeDecl.task("@asyncTaskComponent");
        nodeDecl.title("片段分析");
        nodeDecl.metaPut("isAsync", 0); // 异步任务
        nodeDecl.metaPut("isBlock", 0); // 堵塞任务
        chain.addNode(nodeDecl.linkAdd("04"));

        nodeDecl = new NodeDecl("04", NodeType.activity);
        nodeDecl.task("@asyncTaskComponent");
        nodeDecl.title("异常汇总(堵塞、同步)");
        nodeDecl.metaPut("isAsync", 0); // 异步任务
        nodeDecl.metaPut("isBlock", 1); // 堵塞任务
        chain.addNode(nodeDecl.linkAdd("05"));

        nodeDecl = new NodeDecl("05", NodeType.activity);
        nodeDecl.title("邮件通知");
        nodeDecl.metaPut("isAsync", 0); // 异步任务
        nodeDecl.metaPut("isBlock", 0); // 堵塞任务
        chain.addNode(nodeDecl.linkAdd("06"));

        nodeDecl = new NodeDecl("06", NodeType.end);
        chain.addNode(nodeDecl);

        return chain;
    }
}
