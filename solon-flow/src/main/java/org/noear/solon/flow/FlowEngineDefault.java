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
import org.noear.solon.core.util.RankEntity;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.intercept.ChainInterceptor;
import org.noear.solon.flow.intercept.ChainInvocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流引擎实现
 *
 * @author noear
 * @since 3.0
 */
public class FlowEngineDefault implements FlowEngine {
    protected final Map<String, Chain> chainMap = new ConcurrentHashMap<>();
    protected final Map<String, FlowDriver> driverMap = new ConcurrentHashMap<>();
    protected final List<RankEntity<ChainInterceptor>> interceptorList = new ArrayList<>();

    public FlowEngineDefault() {
        //默认驱动器
        driverMap.put("", new SimpleFlowDriver());
    }

    @Override
    public void addInterceptor(ChainInterceptor interceptor, int index) {
        interceptorList.add(new RankEntity<>(interceptor, index));
        Collections.sort(interceptorList);
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
    public void load(Chain chain) {
        chainMap.put(chain.getId(), chain);
    }

    @Override
    public void unload(String chainId) {
        chainMap.remove(chainId);
    }

    @Override
    public Collection<Chain> getChains() {
        return chainMap.values();
    }

    @Override
    public Chain getChain(String chainId) {
        return chainMap.get(chainId);
    }

    /**
     * 评估
     *
     * @param chainId 链
     * @param context 上下文
     */
    @Override
    public void eval(String chainId, String startId, int depth, FlowContext context) throws FlowException {
        Chain chain = chainMap.get(chainId);
        if (chain == null) {
            throw new IllegalArgumentException("No chain found for id: " + chainId);
        }

        Node startNode;
        if (startId == null) {
            startNode = chain.getStart();
        } else {
            startNode = chain.getNode(startId);
        }

        eval(startNode, depth, context);
    }

    /**
     * 评估
     *
     * @param startNode 开始节点
     * @param depth     执行深度
     * @param context   上下文
     */
    @Override
    public void eval(Node startNode, int depth, FlowContext context) throws FlowException {
        if (startNode == null) {
            throw new IllegalArgumentException("The start node was not found.");
        }

        if (context.engine == null) {
            context.engine = this;
        }

        FlowDriver driver = driverMap.get(startNode.getChain().getDriver());

        if (driver == null) {
            throw new IllegalArgumentException("No driver found for: '" + startNode.getChain().getDriver() + "'");
        }

        //开始执行
        new ChainInvocation(driver, context, startNode, depth, this.interceptorList, this::evalDo).invoke();
    }

    /**
     * 执行评估
     */
    protected void evalDo(ChainInvocation inv) throws FlowException {
        node_run(inv.getDriver(), inv.getContext(), inv.getStartNode(), inv.getEvalDepth());
    }

    /**
     * 条件检测
     */
    private boolean condition_test(FlowDriver driver, FlowContext context, Condition condition, boolean def) throws FlowException {
        if (Utils.isNotEmpty(condition.getDescription())) {
            try {
                return driver.handleTest(context, condition);
            } catch (FlowException e) {
                throw e;
            } catch (Throwable e) {
                throw new FlowException("The test handle failed: " + condition.getChain().getId() + " / " + condition.getDescription(), e);
            }
        } else {
            return def;
        }
    }

    /**
     * 执行任务
     */
    private void task_exec(FlowDriver driver, FlowContext context, Node node) throws FlowException {
        //尝试检测条件；缺省为 true
        if (condition_test(driver, context, node.getWhen(), true)) {
            //起到触发事件的作用 //处理方会“过滤”空任务
            try {
                driver.handleTask(context, node.getTask());
            } catch (FlowException e) {
                throw e;
            } catch (Throwable e) {
                throw new FlowException("The task handle failed: " + node.getChain().getId() + " / " + node.getId(), e);
            }
        }
    }

    /**
     * 运行节点
     */
    private boolean node_run(FlowDriver driver, FlowContext context, Node node, int depth) throws FlowException {
        if (node == null) {
            return false;
        }

        //如果阻断，当前分支不再后流
        if (context.isInterrupted()) {
            //重置阻断（不影响别的分支）
            context.interrupt(false);
            return false;
        }
        //如果停止
        if (context.isStopped()) {
            return false;
        }

        //执行深度控制
        if (depth == 0) {
            return true;
        } else {
            depth--;
        }

        //节点运行之前事件
        driver.onNodeStart(context, node);

        //如果阻断，就不再执行了（onNodeBefore 可能会触发中断）
        if (context.isInterrupted()) {
            //重置阻断（不影响别的分支）
            context.interrupt(false);
            return false;
        }
        //如果停止
        if (context.isStopped()) {
            return false;
        }

        boolean node_end = true;

        switch (node.getType()) {
            case START:
                //转到下个节点
                node_run(driver, context, node.getNextNode(), depth);
                break;
            case END:
                break;
            case ACTIVITY:
                //尝试执行任务（可能为空）
                task_exec(driver, context, node);
                //转到下个节点
                node_run(driver, context, node.getNextNode(), depth);
                break;
            case INCLUSIVE: //包容网关（多选）
                node_end = inclusive_run(driver, context, node, depth);
                break;
            case EXCLUSIVE: //排他网关（单选）
                node_end = exclusive_run(driver, context, node, depth);
                break;
            case PARALLEL: //并行网关（全选）
                node_end = parallel_run(driver, context, node, depth);
                break;
        }

        //节点运行之后事件
        if (node_end) {
            driver.onNodeEnd(context, node);
        }


        return node_end;
    }

    /**
     * 运行包容网关
     */
    private boolean inclusive_run(FlowDriver driver, FlowContext context, Node node, int depth) throws FlowException {
        Stack<Integer> inclusive_stack = context.counter().stack(node.getChain(), "inclusive_run");

        //::流入
        if (node.getPrveLinks().size() > 1) { //如果是多个输入链接（尝试等待）
            if (inclusive_stack.size() > 0) {
                int start_size = inclusive_stack.peek();
                int in_size = context.counter().incr(node.getChain(), node.getId());//运行次数累计
                if (start_size > in_size) { //等待所有支线流入完成
                    return false;
                }

                //聚合结束，取消这个栈节点
                inclusive_stack.pop();
            }
            //如果没有 gt 0，说明之前还没有流出的
        }

        //::流出
        Link def_line = null;
        List<Link> matched_lines = new ArrayList<>();

        for (Link l : node.getNextLinks()) {
            if (l.getCondition().isEmpty()) {
                def_line = l;
            } else {
                if (condition_test(driver, context, l.getCondition(), false)) {
                    matched_lines.add(l);
                }
            }
        }

        if (matched_lines.size() > 0) {
            //记录流出数量
            inclusive_stack.push(matched_lines.size());

            //执行所有满足条件
            for (Link l : matched_lines) {
                node_run(driver, context, l.getNextNode(), depth);
            }
        } else if (def_line != null) {
            //不需要，记录流出数量
            //如果有默认
            node_run(driver, context, def_line.getNextNode(), depth);
        }

        return true;
    }

    /**
     * 运行排他网关
     */
    private boolean exclusive_run(FlowDriver driver, FlowContext context, Node node, int depth) throws FlowException {
        //::流出
        Link def_line = null; //默认线
        for (Link l : node.getNextLinks()) {
            if (l.getCondition().isEmpty()) {
                def_line = l;
            } else {
                if (condition_test(driver, context, l.getCondition(), false)) {
                    //执行第一个满足条件
                    node_run(driver, context, l.getNextNode(), depth);
                    return true; //结束
                }
            }
        }

        if (def_line != null) {
            //如果有默认
            node_run(driver, context, def_line.getNextNode(), depth);
        }

        return true;
    }

    /**
     * 运行并行网关
     */
    private boolean parallel_run(FlowDriver driver, FlowContext context, Node node, int depth) throws FlowException {
        //::流入
        int count = context.counter().incr(node.getChain(), node.getId());//运行次数累计
        if (node.getPrveLinks().size() > count) { //等待所有支线计数完成
            return false;
        }

        //恢复计数
        context.counter().set(node.getChain(), node.getId(), 0);

        //::流出
        for (Node n : node.getNextNodes()) {
            node_run(driver, context, n, depth);
        }

        return true;
    }
}