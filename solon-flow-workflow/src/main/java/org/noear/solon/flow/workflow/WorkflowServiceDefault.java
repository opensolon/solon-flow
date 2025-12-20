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
package org.noear.solon.flow.workflow;

import org.noear.snack4.ONode;
import org.noear.solon.flow.*;
import org.noear.solon.lang.Preview;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有状态的服务默认实现
 *
 * @author noear
 * @since 3.4
 * @since 3.5
 */
@Preview("3.4")
public class WorkflowServiceDefault implements WorkflowService {
    private final transient FlowEngine engine;
    private final transient WorkflowDriver driver;
    private final transient ReentrantLock LOCKER = new ReentrantLock();

    public WorkflowServiceDefault(FlowEngine engine, WorkflowDriver driver) {
        this.engine = engine;
        this.driver = driver;
    }

    /// ////////////////////////////////

    @Override
    public FlowEngine engine() {
        return engine;
    }


    /// ////////////////////////

    /**
     * 提交操作（如果当前节点为等待介入）
     */
    @Override
    public boolean postTaskIfWaiting(String graphId, String nodeId, TaskAction operation, FlowContext context) {
        Node node = engine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        return postTaskIfWaiting(node, operation, context);
    }

    @Override
    public boolean postTaskIfWaiting(Graph graph, String nodeId, TaskAction operation, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        return postTaskIfWaiting(node, operation, context);
    }

    @Override
    public boolean postTaskIfWaiting(Node node, TaskAction operation, FlowContext context) {
        Task statefulTask = getTask(node.getGraph(), context);
        if (statefulTask == null) {
            return false;
        }

        if (statefulTask.getState() != TaskState.WAITING) {
            return false;
        }

        if (statefulTask.getNode().getId().equals(node.getId()) == false) {
            return false;
        }

        postTask(statefulTask.getNode(), operation, context);

        return true;
    }

    @Override
    public void postTask(String graphId, String nodeId, TaskAction operation, FlowContext context) {
        Node node = engine.getGraphOrThrow(graphId).getNodeOrThrow(nodeId);
        postTask(node, operation, context);
    }

    @Override
    public void postTask(Graph graph, String nodeId, TaskAction operation, FlowContext context) {
        Node node = graph.getNodeOrThrow(nodeId);
        postTask(node, operation, context);
    }

    @Override
    public void postTask(Node node, TaskAction operation, FlowContext context) {
        LOCKER.lock();

        try {
            postTaskDo(new FlowExchanger(engine, driver, context), node, operation);
        } finally {
            LOCKER.unlock();
        }
    }

    protected void postTaskDo(FlowExchanger exchanger, Node node, TaskAction operation) {
        if (operation == TaskAction.UNKNOWN) {
            throw new IllegalArgumentException("StateOperation is UNKNOWN");
        }

        TaskState newState = TaskState.byOp(operation);

        //更新状态
        if (operation == TaskAction.BACK) {
            //后退
            backHandle(node, exchanger);
        } else if (operation == TaskAction.BACK_JUMP) {
            //跳转后退
            while (true) {
                Task statefulNode = getTask(node.getGraph(), exchanger.context());
                backHandle(statefulNode.getNode(), exchanger);

                //到目标节点了
                if (statefulNode.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else if (operation == TaskAction.RESTART) {
            //撤回全部（重新开始）
            driver.getStateRepository().stateClear(exchanger.context());
        } else if (operation == TaskAction.FORWARD) {
            //前进
            forwardHandle(node, exchanger, newState);
        } else if (operation == TaskAction.FORWARD_JUMP) {
            //跳转前进
            while (true) {
                Task task = getTask(node.getGraph(), exchanger.context());
                forwardHandle(task.getNode(), exchanger, newState);

                //到目标节点了
                if (task.getNode().getId().equals(node.getId())) {
                    break;
                }
            }
        } else {
            //其它（等待或通过或拒绝）
            driver.getStateRepository().statePut(exchanger.context(), node, newState);
        }
    }


    /// ////////////////////////

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> getTasks(String graphId, FlowContext context) {
        return getTasks(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取多个活动节点
     *
     * @param context 流上下文（不需要有参与者配置）
     */
    @Override
    public Collection<Task> getTasks(Graph graph, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(engine, driver, context);

        exchanger.temporary().vars().put(Task.KEY_ACTIVITY_LIST_GET, true);

        engine.eval(graph, graph.getStart(), -1, exchanger);
        Collection<Task> tmp = (Collection<Task>) exchanger.temporary().vars().get(Task.KEY_ACTIVITY_LIST);

        if (tmp == null) {
            return Collections.emptyList();
        } else {
            return tmp;
        }
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task getTask(String graphId, FlowContext context) {
        return getTask(engine.getGraphOrThrow(graphId), context);
    }

    /**
     * 获取当前活动节点
     *
     * @param context 流上下文（要有参与者配置）
     */
    @Override
    public Task getTask(Graph graph, FlowContext context) {
        FlowExchanger exchanger = new FlowExchanger(engine, driver, context);

        engine.eval(graph, graph.getStart(), -1, exchanger);
        return (Task) exchanger.temporary().vars().get(Task.KEY_ACTIVITY_NODE);
    }

    @Override
    public void clearState(String graphId, FlowContext context) {
        this.clearState(engine.getGraphOrThrow(graphId), context);
    }


    @Override
    public void clearState(Graph graph, FlowContext context) {
        driver.getStateRepository().stateClear(context);
    }

    /// ////////////////////////////////


    /**
     * 前进处理
     */
    protected void forwardHandle(Node node, FlowExchanger exchanger, TaskState newState) {
        //如果是完成或跳过，则向前流动
        try {
            driver.postHandleTask(exchanger, node.getTask());
            driver.getStateRepository().statePut(exchanger.context(), node, newState);

            //重新查找下一个可执行节点（可能为自动前进）
            Node nextNode = node.getNextNode();
            if (nextNode != null) {
                if (nextNode.getType() == NodeType.INCLUSIVE || nextNode.getType() == NodeType.PARALLEL) {
                    //如果是流入网关，要通过引擎计算获取下个活动节点（且以图做为参数，可能自动流转到网关外）
                    Task statefulNextNode = getTask(node.getGraph(), exchanger.context());

                    if (statefulNextNode != null) {
                        nextNode = statefulNextNode.getNode();
                    } else {
                        nextNode = null;
                    }
                }

                if (nextNode != null) {
                    if (driver.getStateController().isAutoForward(exchanger.context(), nextNode)) {
                        //如果要自动前进
                        engine.eval(nextNode.getGraph(), nextNode, -1, new FlowExchanger(engine, driver, exchanger.context()));
                    }
                }
            }
        } catch (Throwable e) {
            throw new FlowException("Task handle failed: " + node.getGraph().getId() + " / " + node.getId(), e);
        }
    }

    /**
     * 后退处理
     *
     * @param node      流程节点
     * @param exchanger 流交换器
     */
    protected void backHandle(Node node, FlowExchanger exchanger) {
        //撤回之前的节点
        for (Node n1 : node.getPrevNodes()) {
            //移除状态（要求重来）
            if (n1.getType() == NodeType.ACTIVITY) {
                driver.getStateRepository().stateRemove(exchanger.context(), n1);
            } else if (NodeType.isGateway(n1.getType())) {
                //回退所有子节点
                for (Node n2 : n1.getNextNodes()) {
                    if (n2.getType() == NodeType.ACTIVITY) {
                        driver.getStateRepository().stateRemove(exchanger.context(), n2);
                    }
                }
                //再到前一级
                backHandle(n1, exchanger);
            }
        }
    }

    /// ////////

    /**
     * 转为 yaml
     */
    public String getGraphYaml(Graph graph, FlowContext context) {
        return new Yaml().dump(buildDom(graph, context));
    }

    /**
     * 转为 json
     */
    public String getGraphJson(Graph graph, FlowContext context) {
        return ONode.serialize(buildDom(graph, context));
    }


    protected Map<String, Object> buildDom(Graph graph, FlowContext context) {
        Map<String, Object> domRoot = graph.toMap();

        if (context != null) {
            //输出节点状态（方便前图标注进度）
            Map<String, String> domStateful = new LinkedHashMap<>();
            domRoot.put("stateful", domStateful);

            for (Map.Entry<String, Node> entry : graph.getNodes().entrySet()) {
                TaskState type = driver.getStateRepository().stateGet(context, entry.getValue());
                if (type != null && type != TaskState.UNKNOWN) {
                    domStateful.put(entry.getKey(), type.toString());
                }
            }
        }

        return domRoot;
    }
}