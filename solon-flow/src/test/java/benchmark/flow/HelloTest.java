package benchmark.flow;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;

/**
 * @author noear 2025/6/18 created
 */
public class HelloTest {
    @Test
    public void parse1() {
        //预热
        for (int i = 0; i < 10; i++) {
            Graph.fromText(case1_yml);
        }

        //测试
        int count = 10_000; //flow(on macbook): 1.2s 跑完
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Graph.fromText(case1_yml);
        }
        long time1 = System.currentTimeMillis() - start;
        System.out.println("parse1:" + time1);
    }

    //没有 io
    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load(Graph.fromText(case1_yml));

        FlowContext context = FlowContext.of();
        context.put("a", 3);
        context.put("b", 4);

        //预热
        for (int i = 0; i < 10; i++) {
            flowEngine.eval("case1", context);
            case1_java(3, 4);
        }

        //测试
        int count = 1_000_000; //flow(on macbook): 1.2s 跑完
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            flowEngine.eval("case1", context);
        }
        long time_flow = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            case1_java(3, 4);
        }
        long time_java = System.currentTimeMillis() - start;

        System.out.println("case1-flow:" + time_flow + ", java:" + time_java);
    }

    private int case1_java(int a, int b) {
        if (a > b) {
            return a + b;
        } else {
            return a - b;
        }
    }

    /// ///////////////////////

    //有 io
    @Test
    public void case2() {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load(Graph.fromText(case2_yml));

        //预热
        for (int i = 0; i < 10; i++) {
            flowEngine.eval("case2");
            case2_java();
        }

        //测试
        int count = 100_000; //flow: 0.6s 跑完
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            flowEngine.eval("case2");
        }
        long time_flow = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            case2_java();
        }
        long time_java = System.currentTimeMillis() - start;

        System.out.println("case2-flow:" + time_flow + ", java:" + time_java);
    }

    private void case2_java(){
        System.out.println("Hello World");
    }

    static String case1_yml = "id: \"case1\"\n" +
            "layout:\n" +
            "  - type: exclusive\n" +
            "    link:\n" +
            "      - when: a > b\n" +
            "        nextId: sum\n" +
            "      - nextId: sub\n" +
            "  - id: sum\n" +
            "    title: \"加法\"\n" +
            "    task: 'context.put(\"result\", a + b);'\n" +
            "    link: end\n" +
            "  - id: sub\n" +
            "    title: \"减法\"\n" +
            "    task: 'context.put(\"result\", a - b);'\n" +
            "    link: end\n" +
            "  - id: end\n" +
            "    type: end";

    static String case2_yml = "id: \"case2\"\n" +
            "layout:\n" +
            "  - task: |\n" +
            "      System.out.println(\"Hello World\");";
}