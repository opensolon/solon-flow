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
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.util.Stepper;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流引擎实现
 *
 * @author noear
 * @since 3.0
 */
public class FlowEngineDefault implements FlowEngine {
    protected final Map<String, Graph> graphMap;
    protected final Map<String, FlowDriver> driverMap;
    protected FlowDriver driverDef;
    protected final List<RankEntity<FlowInterceptor>> interceptorList;
    protected final boolean simplified;

    public FlowEngineDefault(FlowDriver driver, boolean simplified) {
        //默认驱动器
        if (driver == null) {
            driver = SimpleFlowDriver.getInstance();
        }


        this.interceptorList = new ArrayList<>();
        this.driverDef = driver;

        this.simplified = simplified;

        if (simplified) {
            this.graphMap = Collections.emptyMap();
            this.driverMap = Collections.emptyMap();
        } else {
            this.graphMap = new HashMap<>();
            this.driverMap = new HashMap<>();
        }
    }

    @Override
    public FlowDriver getDriver(Graph graph) {
        Assert.notNull(graph, "graph is null");

        if (Assert.isEmpty(graph.getDriver())) {
            return driverDef;
        } else {
            FlowDriver driver = driverMap.get(graph.getDriver());

            if (driver == null) {
                throw new IllegalArgumentException("No driver found for: '" + graph.getDriver() + "'");
            }

            return driver;
        }
    }

    @Override
    public void addInterceptor(FlowInterceptor interceptor, int index) {
        interceptorList.add(new RankEntity<>(interceptor, index));

        if (interceptorList.size() > 0) {
            Collections.sort(interceptorList);
        }
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
            if (name == null) {
                driverDef = driver;
            } else {
                driverMap.put(name, driver);
            }
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
     * @param graph     图
     * @param exchanger 交换器
     */
    @Override
    public void eval(Graph graph, FlowExchanger exchanger, FlowOptions options) throws FlowException {
        //开始执行
        Node lastNode = exchanger.context().trace().lastNode(graph);
        FlowExchanger bak = exchanger.context().exchanger();

        if (options == null) {
            options = new FlowOptions();
        }

        options.interceptorAdd(interceptorList);

        try {
            exchanger.context().exchanger(exchanger);
            exchanger.context().stopped(false); //每次执行前，重置下
            new FlowInvocation(exchanger, options, lastNode, this::evalDo).invoke();
        } finally {
            exchanger.context().exchanger(bak);
        }
    }

    /**
     * 执行评估
     */
    protected void evalDo(FlowInvocation inv, FlowOptions options) throws FlowException {
        node_run(inv.getExchanger(), options, inv.getStartNode().getGraph().getStart(), inv.getStartNode());
    }

    /**
     * 节点运行开始时
     */
    protected boolean onNodeStart(FlowExchanger exchanger, FlowOptions options, Node node) {
        if (exchanger.isReverting() == false) {
            //恢复完成，才执行拦截
            for (RankEntity<FlowInterceptor> interceptor : options.getInterceptorList()) {
                interceptor.target.onNodeStart(exchanger.context(), node);
            }

            exchanger.driver().onNodeStart(exchanger, node);


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
        }

        return true;
    }

    /**
     * 节点运行结束时
     */
    protected boolean onNodeEnd(FlowExchanger exchanger, FlowOptions options, Node node) {
        if (exchanger.isReverting() == false) {
            for (RankEntity<FlowInterceptor> interceptor : options.getInterceptorList()) {
                interceptor.target.onNodeEnd(exchanger.context(), node);
            }

            exchanger.driver().onNodeEnd(exchanger, node);


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
        }

        return true;
    }

    /**
     * 条件检测
     */
    protected boolean condition_test(FlowExchanger exchanger, ConditionDesc condition, boolean def) throws FlowException {
        if (condition.isEmpty()) {
            return def;
        }

        try {
            return exchanger.driver().handleCondition(exchanger, condition);
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
    protected boolean task_exec(FlowExchanger exchanger, FlowOptions options, Node node) throws FlowException {
        if (exchanger.isReverting()) {
            //恢复中，则跳过
            return true;
        }

        //任务之前，流入之后
        if (onNodeStart(exchanger, options, node) == false) {
            return false;
        }

        /// ///////////////////

        //尝试检测条件；缺省为 true
        if (condition_test(exchanger, node.getWhen(), true)) {
            //起到触发事件的作用 //处理方会“过滤”空任务
            try {
                exchanger.driver().handleTask(exchanger, node.getTask());
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

        /// ///////////////////

        //任务之后，流出之前
        return onNodeEnd(exchanger, options, node);
    }

    /**
     * 运行节点
     */
    protected void node_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        if (node == null) {
            return;
        }

        //如果停止
        if (exchanger.isStopped()) {
            return;
        }

        //如果阻断，当前分支不再后流
        if (exchanger.isInterrupted()) {
            //重置阻断（不影响别的分支）
            exchanger.interrupt(false);
            return;
        }

        //检测恢复情况
        if (exchanger.isReverting()) {
            if (node.getId().equals(startNode.getId()) && node.getGraph().getId().equals(startNode.getGraph().getId())) {
                //恢复完成（恢复到同图同节点）
                exchanger.reverting(false);
            }
        } else {
            //提前记录，方便下次进来
            exchanger.recordNode(node.getGraph(), node);
        }

        //步进控制
        if (exchanger.isReverting() == false) {
            if (exchanger.nextSetp(node) == false) {
                exchanger.stop();
                return;
            }
        }

        switch (node.getType()) {
            case START:
                start_run(exchanger, options, node, startNode);
                break;
            case END:
                end_run(exchanger, options, node, startNode);
                break;
            case ACTIVITY:
                activity_run(exchanger, options, node, startNode);
                break;
            case INCLUSIVE: //包容网关（多选）
                inclusive_run(exchanger, options, node, startNode);
                break;
            case EXCLUSIVE: //排他网关（单选）
                exclusive_run(exchanger, options, node, startNode);
                break;
            case PARALLEL: //并行网关（全选）
                parallel_run(exchanger, options, node, startNode);
                break;
            case LOOP:
                loop_run(exchanger, options, node, startNode);
                break;
        }
    }

    protected void start_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
        //任务之前，流入之后
        if (onNodeStart(exchanger, options, node) == false) {
            return;
        }

        //任务之后，流出之前
        if (onNodeEnd(exchanger, options, node) == false) {
            return;
        }

        //::流出
        for (Link l : node.getNextLinks()) {
            if (condition_test(exchanger, l.getWhen(), true)) {
                node_run(exchanger, options, l.getNextNode(), startNode);
            }
        }
    }

    protected void end_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
        //任务之前，流入之后
        if (onNodeStart(exchanger, options, node) == false) {
            return;
        }

        //任务之后，流出之前
        if (onNodeEnd(exchanger, options, node) == false) {
            return;
        }
    }

    protected void activity_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
        //尝试执行任务（可能为空）
        if (task_exec(exchanger, options, node) == false) {
            return;
        }

        //::流出
        activity_run_out(exchanger, options, node, startNode);
    }

    protected void activity_run_out(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
        for (Link l : node.getNextLinks()) {
            if (condition_test(exchanger, l.getWhen(), true)) {
                node_run(exchanger, options, l.getNextNode(), startNode);
            }
        }
    }

    /**
     * 运行包容网关
     */
    protected void inclusive_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        if (inclusive_run_in(exchanger, options, node, startNode) == false) {
            return;
        }

        //尝试执行任务（可能为空）
        if (task_exec(exchanger, options, node) == false) {
            return;
        }


        inclusive_run_out(exchanger, options, node, startNode);
    }

    //包容网关
    protected boolean inclusive_run_in(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
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
    protected void inclusive_run_out(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        Stack<Integer> inclusive_stack = exchanger.temporary().stack(node.getGraph(), "inclusive_run");

        //::流出
        List<Link> matched_lines = new ArrayList<>();

        for (Link l : node.getNextLinks()) {
            if (condition_test(exchanger, l.getWhen(), true)) {
                matched_lines.add(l);
            }
        }

        if (matched_lines.size() > 0) {
            //记录流出数量
            inclusive_stack.push(matched_lines.size());

            //执行所有满足条件
            for (Link l : matched_lines) {
                node_run(exchanger, options, l.getNextNode(), startNode);
            }
        }
    }

    /**
     * 运行排他网关
     */
    protected void exclusive_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        //尝试执行任务（可能为空）
        if (task_exec(exchanger, options, node) == false) {
            return;
        }

        //::流出
        exclusive_run_out(exchanger, options, node, startNode);
    }

    protected void exclusive_run_out(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        //::流出
        Link def_line = null; //默认线
        for (Link l : node.getNextLinks()) {
            if (l.getWhen().isEmpty()) {
                def_line = l;
            } else {
                if (condition_test(exchanger, l.getWhen(), false)) {
                    //执行第一个满足条件
                    node_run(exchanger, options, l.getNextNode(), startNode);
                    return; //结束
                }
            }
        }

        if (def_line != null) {
            //如果有默认
            node_run(exchanger, options, def_line.getNextNode(), startNode);
        }
    }

    /**
     * 运行并行网关
     */
    protected void parallel_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        if (parallel_run_in(exchanger, options, node, startNode) == false) {
            return;
        }

        //尝试执行任务（可能为空）
        if (task_exec(exchanger, options, node) == false) {
            return;
        }

        parallel_run_out(exchanger, options, node, startNode);
    }

    protected boolean parallel_run_in(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        //::流入
        int count = exchanger.temporary().countIncr(node.getGraph(), node.getId());//运行次数累计
        if (node.getPrevLinks().size() > count) { //等待所有支线计数完成
            return false;
        }

        return true;
    }

    protected void parallel_run_out(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) throws FlowException {
        //恢复计数
        exchanger.temporary().countSet(node.getGraph(), node.getId(), 0);

        //::流出
        if (exchanger.driver().getExecutor() == null || node.getNextNodes().size() < 2) { //没有2个，也没必要用线程池
            //单线程
            for (Node n : node.getNextNodes()) {
                node_run(exchanger, options, n, startNode);
            }
        } else {
            //多线程
            CountDownLatch cdl = new CountDownLatch(node.getNextNodes().size());
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            for (Node n : node.getNextNodes()) {
                exchanger.driver().getExecutor().execute(() -> {
                    try {
                        if (errorRef.get() != null) {
                            return;
                        }

                        node_run(exchanger, options, n, startNode);
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
    }

    protected void loop_run(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
        if (Utils.isEmpty(node.getMetaAsString("$for"))) {
            //流入（结束）
            if (loop_run_in(exchanger, options, node, startNode) == false) {
                return;
            }

            //尝试执行任务（可能为空）
            if (task_exec(exchanger, options, node) == false) {
                return;
            }

            //流出
            //node_run(exchanger, node.getNextNode(), startNode);
            activity_run_out(exchanger, options, node, startNode);
        } else {
            //尝试执行任务（可能为空）
            if (task_exec(exchanger, options, node) == false) {
                return;
            }

            //流出（开始）
            loop_run_out(exchanger, options, node, startNode);
        }
    }

    protected boolean loop_run_in(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
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

    protected void loop_run_out(FlowExchanger exchanger, FlowOptions options, Node node, Node startNode) {
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
            //node_run(exchanger, node.getNextNode(), startNode);
            activity_run_out(exchanger, options, node, startNode);
        }
    }
}