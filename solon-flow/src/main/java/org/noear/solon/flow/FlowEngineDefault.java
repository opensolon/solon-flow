/*
 * Copyright 2017-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.flow;

import org.noear.solon.Utils;
import org.noear.solon.core.util.Assert;
import org.noear.solon.core.util.RankEntity;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.noear.solon.flow.intercept.FlowInvocation;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.FlowStatefulServiceDefault;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.util.Stepper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流引擎实现
 *
 * @author noear
 * @since 3.0
 */
public class FlowEngineDefault implements FlowEngine {
    static final Logger log = LoggerFactory.getLogger(FlowEngineDefault.class);

    protected final Map<String, Graph> graphMap = new ConcurrentHashMap<>();
    protected final Map<String, FlowDriver> driverMap = new ConcurrentHashMap<>();
    protected final List<RankEntity<FlowInterceptor>> interceptorList = new ArrayList<>();

    public FlowEngineDefault() {
        this(null);
    }

    public FlowEngineDefault(FlowDriver driver) {
        //默认驱动器
        if (driver == null) {
            driver = new SimpleFlowDriver();
        }

        driverMap.put("", driver);
    }

    @Override
    public FlowDriver getDriver(Graph graph) {
        Assert.notNull(graph, "graph is null");

        FlowDriver driver = driverMap.get(graph.getDriver());

        if (driver == null) {
            throw new IllegalArgumentException("No driver found for: '" + graph.getDriver() + "'");
        }

        return driver;
    }

    @Override
    public <T extends FlowDriver> T getDriverAs(Graph graph, Class<T> driverClass) {
        FlowDriver driver = getDriver(graph);
        if (driverClass.isInstance(driver)) {
            return (T) driver;
        } else {
            throw new IllegalArgumentException("No " + driverClass.getSimpleName() + " found for: '" + graph.getDriver() + "'");
        }
    }

    private FlowStatefulService statefulService;

    @Override
    public FlowStatefulService forStateful() {
        if (statefulService == null) {
            statefulService = new FlowStatefulServiceDefault(this);
        }

        return statefulService;
    }

    @Override
    public void addInterceptor(FlowInterceptor interceptor, int index) {
        interceptorList.add(new RankEntity<>(interceptor, index));
        Collections.sort(interceptorList);
    }

    @Override
    public void removeInterceptor(FlowInterceptor interceptor) {
        for (RankEntity<FlowInterceptor> i : interceptorList) {
            if (i.target == interceptor) {
                interceptorList.remove(i);
                break;
            }
        }
    }

    @Override
    public void register(String name, FlowDriver driver) {
        if (driver != null) {
            driverMap.put(name, driver);
        }
    }

    @Override
    public void unregister(String name) {
        if (Utils.isNotEmpty(name)) {
            driverMap.remove(name);
        }
    }

    @Override
    public void load(Graph graph) {
        graphMap.put(graph.getId(), graph);
    }

    @Override
    public void unload(String graphId) {
        graphMap.remove(graphId);
    }

    @Override
    public Collection<Graph> getGraphs() {
        return graphMap.values();
    }

    @Override
    public Graph getGraph(String graphId) {
        return graphMap.get(graphId);
    }

    /**
     * 评估
     *
     * @param startNode 开始节点
     * @param depth     执行深度
     * @param exchanger 交换器
     */
    @Override
    public void eval(Graph graph, Node startNode, int depth, FlowExchanger exchanger) throws FlowException {
        if (startNode == null) {
            startNode = graph.getStart();
        }

        //准备工作
        prepare(exchanger);

        FlowDriver driver = getDriver(startNode.getGraph());

        //开始执行
        FlowExchanger bak = exchanger.context().getAs(FlowExchanger.TAG); //跨图调用时，可能会有
        try {
            if (bak != exchanger) {
                exchanger.context().put(FlowExchanger.TAG, exchanger);
            }

            new FlowInvocation(driver, exchanger, startNode, depth, this.interceptorList, this::evalDo).invoke();
        } finally {
            if (bak != exchanger) {
                if (bak == null) {
                    exchanger.context().remove(FlowExchanger.TAG);
                } else {
                    exchanger.context().put(FlowExchanger.TAG, bak);
                }
            }
        }
    }

    /**
     * 准备工作
     */
    protected void prepare(FlowExchanger exchanger) {
        if (exchanger.engine == null) {
            exchanger.engine = this;
        }
    }

    /**
     * 执行评估
     */
    protected void evalDo(FlowInvocation inv) throws FlowException {
        node_run(inv.getDriver(), inv.getExchanger(), inv.getStartNode().getGraph().getStart(), inv.getStartNode(), inv.getEvalDepth());
    }

    /**
     * 节点运行开始时
     */
    protected void onNodeStart(FlowDriver driver, FlowExchanger exchanger, Node node) {
        if (exchanger.isReverting() == false) {
            //恢复完成，才执行拦截
            for (RankEntity<FlowInterceptor> interceptor : interceptorList) {
                interceptor.target.onNodeStart(exchanger.context(), node);
            }

            driver.onNodeStart(exchanger, node);
        }
    }

    /**
     * 节点运行结束时
     */
    protected void onNodeEnd(FlowDriver driver, FlowExchanger exchanger, Node node) {
        for (RankEntity<FlowInterceptor> interceptor : interceptorList) {
            interceptor.target.onNodeEnd(exchanger.context(), node);
        }

        driver.onNodeEnd(exchanger, node);
    }

    /**
     * 条件检测
     */
    protected boolean condition_test(FlowDriver driver, FlowExchanger exchanger, Condition condition, boolean def) throws FlowException {
        if (condition.isEmpty()) {
            return def;
        }

        try {
            return driver.handleCondition(exchanger, condition);
        } catch (FlowException e) {
            throw e;
        } catch (Throwable e) {
            throw new FlowException("The condition handle failed: " + condition.getGraph().getId() + " / " + condition.getDescription(), e);
        }
    }

    /**
     * 执行任务
     *
     * @return 是否继续（或是否成功）
     */
    protected boolean task_exec(FlowDriver driver, FlowExchanger exchanger, Node node) throws FlowException {
        if (exchanger.isReverting()) {
            //恢复中，则跳过
            return true;
        }

        //尝试检测条件；缺省为 true
        if (condition_test(driver, exchanger, node.getWhen(), true)) {
            //起到触发事件的作用 //处理方会“过滤”空任务
            try {
                driver.handleTask(exchanger, node.getTask());
            } catch (FlowException e) {
                throw e;
            } catch (Throwable e) {
                throw new FlowException("The task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
            }
        }

        //如果停止
        if (exchanger.isStopped()) {
            return false;
        }

        //如果阻断，就不再执行了（onNodeBefore 可能会触发中断）
        if (exchanger.isInterrupted()) {
            //重置阻断（不影响别的分支）
            exchanger.interrupt(false);
            return false;
        }

        return true;
    }

    /**
     * 运行节点
     */
    protected boolean node_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        if (node == null) {
            return false;
        }

        //如果停止
        if (exchanger.isStopped()) {
            return false;
        }

        //如果阻断，当前分支不再后流
        if (exchanger.isInterrupted()) {
            //重置阻断（不影响别的分支）
            exchanger.interrupt(false);
            return false;
        }

        //检测恢复情况
        if (exchanger.isReverting()) {
            if (node.getId().equals(startNode.getId())) {
                //恢复完成（相对之前的 startNode 方案，新方案能还原上层状态）
                exchanger.reverting(false);
            }
        } else {
            ((AbstractFlowContext) exchanger.context()).lastNode(node);
        }

        //执行深度控制
        if (depth == 0) {
            return true;
        } else if (exchanger.isReverting() == false) {
            //恢复后再计算深度
            depth--;
        }

        //节点运行之前事件
        onNodeStart(driver, exchanger, node);

        //如果停止
        if (exchanger.isStopped()) {
            return false;
        }

        //如果阻断，就不再执行了（onNodeBefore 可能会触发中断）
        if (exchanger.isInterrupted()) {
            //重置阻断（不影响别的分支）
            exchanger.interrupt(false);
            return false;
        }


        boolean node_end = true;

        switch (node.getType()) {
            case START:
                //转到下个节点
                node_run(driver, exchanger, node.getNextNode(), startNode, depth);
                break;
            case END:
                break;
            case ACTIVITY:
                node_end = activity_run(driver, exchanger, node, startNode, depth);
                break;
            case INCLUSIVE: //包容网关（多选）
                node_end = inclusive_run(driver, exchanger, node, startNode, depth);
                break;
            case EXCLUSIVE: //排他网关（单选）
                exclusive_run(driver, exchanger, node, startNode, depth);
                break;
            case PARALLEL: //并行网关（全选）
                node_end = parallel_run(driver, exchanger, node, startNode, depth);
                break;
            case LOOP:
                node_end = loop_run(driver, exchanger, node, startNode, depth);
                break;
        }

        //节点运行之后事件
        if (node_end) {
            onNodeEnd(driver, exchanger, node);
        }


        return node_end;
    }

    protected boolean activity_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) {
        //尝试执行任务（可能为空）
        if (task_exec(driver, exchanger, node) == false) {
            return false;
        }

        //流出（原始态）
        //return node_run(driver, exchanger, node.getNextNode(), depth);
       return activity_run_out(driver, exchanger, node, startNode, depth);
    }

    //活动节点
    protected boolean activity_run_out(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth){
        //::流出
        for (Link l : node.getNextLinks()) {
            if (condition_test(driver, exchanger, l.getWhen(), true)) {
                node_run(driver, exchanger, l.getNextNode(), startNode, depth);
            }
        }

        return true;
    }

    /**
     * 运行包容网关
     */
    protected boolean inclusive_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        if (inclusive_run_in(driver, exchanger, node, startNode, depth) == false) {
            return false;
        }

        //尝试执行任务（可能为空）
        if (task_exec(driver, exchanger, node) == false) {
            return false;
        }


        return inclusive_run_out(driver, exchanger, node, startNode, depth);
    }

    //包容网关
    protected boolean inclusive_run_in(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        Stack<Integer> inclusive_stack = exchanger.temporary().stack(node.getGraph(), "inclusive_run");

        //::流入
        if (node.getPrevLinks().size() > 1) { //如果是多个输入连接（尝试等待）
            if (inclusive_stack.size() > 0) {
                int start_size = inclusive_stack.peek();
                int in_size = exchanger.temporary().countIncr(node.getGraph(), node.getId());//运行次数累计
                if (start_size > in_size) { //等待所有支线流入完成
                    return false;
                }

                //聚合结束，取消这个栈节点
                inclusive_stack.pop();
            }
            //如果没有 gt 0，说明之前还没有流出的
        }

        return true;
    }

    //包容网关
    protected boolean inclusive_run_out(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        Stack<Integer> inclusive_stack = exchanger.temporary().stack(node.getGraph(), "inclusive_run");

        //::流出
        List<Link> matched_lines = new ArrayList<>();

        for (Link l : node.getNextLinks()) {
            if (condition_test(driver, exchanger, l.getWhen(), true)) {
                matched_lines.add(l);
            }
        }

        if (matched_lines.size() > 0) {
            //记录流出数量
            inclusive_stack.push(matched_lines.size());

            //执行所有满足条件
            for (Link l : matched_lines) {
                node_run(driver, exchanger, l.getNextNode(), startNode, depth);
            }
        }

        return true;
    }

    /**
     * 运行排他网关
     */
    protected boolean exclusive_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        //尝试执行任务（可能为空）
        if (task_exec(driver, exchanger, node) == false) {
            return false;
        }

        //::流出
        return exclusive_run_out(driver, exchanger, node, startNode, depth);
    }

    protected boolean exclusive_run_out(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        //::流出
        Link def_line = null; //默认线
        for (Link l : node.getNextLinks()) {
            if (l.getWhen().isEmpty()) {
                def_line = l;
            } else {
                if (condition_test(driver, exchanger, l.getWhen(), false)) {
                    //执行第一个满足条件
                    node_run(driver, exchanger, l.getNextNode(), startNode, depth);
                    return true; //结束
                }
            }
        }

        if (def_line != null) {
            //如果有默认
            node_run(driver, exchanger, def_line.getNextNode(), startNode, depth);
        }

        return true;
    }

    /**
     * 运行并行网关
     */
    protected boolean parallel_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        if (parallel_run_in(driver, exchanger, node, startNode, depth) == false) {
            return false;
        }

        //尝试执行任务（可能为空）
        if (task_exec(driver, exchanger, node) == false) {
            return false;
        }

        return parallel_run_out(driver, exchanger, node, startNode, depth);
    }

    protected boolean parallel_run_in(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        //::流入
        int count = exchanger.temporary().countIncr(node.getGraph(), node.getId());//运行次数累计
        if (node.getPrevLinks().size() > count) { //等待所有支线计数完成
            return false;
        }

        return true;
    }

    protected boolean parallel_run_out(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) throws FlowException {
        //恢复计数
        exchanger.temporary().countSet(node.getGraph(), node.getId(), 0);

        //::流出
        if (exchanger.context().executor() == null || node.getNextNodes().size() < 2) { //没有2个，也没必要用线程池
            //单线程
            for (Node n : node.getNextNodes()) {
                node_run(driver, exchanger, n, startNode, depth);
            }
        } else {
            //多线程
            CountDownLatch cdl = new CountDownLatch(node.getNextNodes().size());
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            for (Node n : node.getNextNodes()) {
                exchanger.context().executor().execute(() -> {
                    try {
                        if (errorRef.get() != null) {
                            return;
                        }

                        node_run(driver, exchanger, n, startNode, depth);
                    } catch (Throwable ex) {
                        errorRef.set(ex);
                    } finally {
                        cdl.countDown();
                    }
                });
            }

            //等待
            try {
                cdl.await();
            } catch (InterruptedException ignore) {
                //
            }

            //异常处理
            if (errorRef.get() != null) {
                if (errorRef.get() instanceof FlowException) {
                    throw (FlowException) errorRef.get();
                } else {
                    throw new FlowException(errorRef.get());
                }
            }
        }

        return true;
    }

    protected boolean loop_run(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) {
        if (Utils.isEmpty(node.getMetaAsString("$for"))) {
            //流入（结束）
            if (loop_run_in(driver, exchanger, node, startNode, depth) == false) {
                return false;
            }

            //尝试执行任务（可能为空）
            if (task_exec(driver, exchanger, node) == false) {
                return false;
            }

            //流出
            return node_run(driver, exchanger, node.getNextNode(), startNode, depth);
        } else {
            //尝试执行任务（可能为空）
            if (task_exec(driver, exchanger, node) == false) {
                return false;
            }

            //流出（开始）
            return loop_run_out(driver, exchanger, node, startNode, depth);
        }
    }

    protected boolean loop_run_in(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) {
        Stack<Iterator> loop_stack = exchanger.temporary().stack(node.getGraph(), "loop_run");

        //::流入
        if (loop_stack.size() > 0) {
            Iterator inIter = loop_stack.peek();
            if (inIter.hasNext()) { //等待遍历完成
                return false;
            }

            //聚合结束，取消这个栈节点
            loop_stack.pop();
        }
        //如果没有 gt 0，说明之前还没有流出的

        return true;
    }

    protected boolean loop_run_out(FlowDriver driver, FlowExchanger exchanger, Node node, Node startNode, int depth) {
        String forKey = node.getMetaAsString("$for");
        Object inKey = node.getMeta("$in");
        Object inObj = null;

        if (inKey instanceof List) {
            //常量集合
            inObj = inKey;
        } else if (inKey instanceof String) {
            String inKeyStr = inKey.toString();

            if (inKeyStr.indexOf(':') < 0 && inKeyStr.indexOf("...") < 0) {
                //变量
                inObj = exchanger.context().getAs(inKeyStr);
            } else {
                //步进器："start:end:setp"
                inObj = Stepper.from(inKeyStr);
            }
        } else {
            throw new FlowException("The '$in' must be a list or a string");
        }

        Iterator inIter = null;
        if (inObj instanceof Iterator) {
            inIter = (Iterator) inObj;
        } else if (inObj instanceof Iterable) {
            inIter = ((Iterable) inObj).iterator();
        } else {
            throw new FlowException(inKey + " is not a collection");
        }


        Stack<Iterator> loop_stack = exchanger.temporary().stack(node.getGraph(), "loop_run");
        loop_stack.push(inIter);

        //::流出
        while (inIter.hasNext()) {
            Object item = inIter.next();
            exchanger.context().put(forKey, item);
            node_run(driver, exchanger, node.getNextNode(), startNode, depth);
        }

        return true;
    }
}