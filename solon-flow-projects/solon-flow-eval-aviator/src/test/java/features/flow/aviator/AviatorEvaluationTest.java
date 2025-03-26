package features.flow.aviator;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.ChainContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.driver.SimpleChainDriver;
import org.noear.solon.flow.evaluation.AviatorEvaluation;

/**
 * @author noear 2025/3/26 created
 */
public class AviatorEvaluationTest {
    @Test
    public void case1() throws Throwable {
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleChainDriver(new AviatorEvaluation()));

        engine.load("classpath:flow/*");

        ChainContext context = new ChainContext();
        context.put("a", 1);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.result);
        assert context.result == null;


        context = new ChainContext();
        context.put("a", 3);
        context.put("b", 2);

        engine.eval("f1", context);
        System.out.println(context.result);
        assert ((Number) context.result).intValue() == 5;
    }
}
