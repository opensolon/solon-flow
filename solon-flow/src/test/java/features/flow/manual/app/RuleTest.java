package features.flow.manual.app;

import demo.flow.rule.BookOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.test.SolonTest;

/**
 * @author noear 2025/1/29 created
 */
@Slf4j
@SolonTest
public class RuleTest {
    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/rule/bookDiscount.yml");

        BookOrder bookOrder = new BookOrder();
        bookOrder.setOriginalPrice(10);

        FlowContext ctx = FlowContext.of();
        ctx.put("order", bookOrder);

        flowEngine.eval("book_discount", ctx);

        //价格没变，还是10块
        assert bookOrder.getRealPrice() == 10;
    }

    @Test
    public void case2() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load(Graph.fromUri("classpath:flow/rule/bookDiscount.yml"));

        BookOrder bookOrder = new BookOrder();
        bookOrder.setOriginalPrice(120);

        FlowContext ctx = FlowContext.of();
        ctx.put("order", bookOrder);

        flowEngine.eval("book_discount", ctx);

        //省了20块
        assert bookOrder.getRealPrice() == 100;
    }

    @Test
    public void case4() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load(Graph.fromUri("classpath:flow/rule/bookDiscount.yml"));

        BookOrder bookOrder = new BookOrder();
        bookOrder.setOriginalPrice(500);

        FlowContext ctx = FlowContext.of();
        ctx.put("order", bookOrder);

        flowEngine.eval("book_discount", ctx);

        //价格变了，省了100块
        assert bookOrder.getRealPrice() == 400;
    }
}