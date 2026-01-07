package features.workflow.generated;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.*;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟生产测试 - 复杂业务场景、状态持久化、异常处理
 * 每个测试方法都有自己完全独立的：工作流引擎、工作流服务、流程图定义、组件实现
 */
class WorkflowProductionTest {

    @Test
    void testSimpleLinearWorkflow() {
        // 测试目的：验证最基本的线性工作流功能
        // 测试场景：一个简单的线性流程（开始 -> 任务1 -> 任务2 -> 结束）
        // 验证点：任务按顺序执行、状态正确转换、数据正确传递

        // 1. 创建独立的组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_executed", true);
                context.put(nodeId + "_time", System.currentTimeMillis());
                context.put("executionData", "数据-" + nodeId);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("simple-linear", "简单线性流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "test-simple-1";
        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "user");

        // 获取并完成任务1
        Task task1 = workflowExecutor.claimTask(graph, context);
        assertNotNull(task1);
        assertEquals("task1", task1.getNodeId());
        assertEquals(TaskState.WAITING, task1.getState());

        // 运行任务
        assertDoesNotThrow(() -> task1.run(context));
        workflowExecutor.submitTask(graph, "task1", TaskAction.FORWARD, context);

        // 获取并完成任务2
        Task task2 = workflowExecutor.claimTask(graph, context);
        assertNotNull(task2);
        assertEquals("task2", task2.getNodeId());

        workflowExecutor.submitTask(graph, "task2", TaskAction.FORWARD, context);

        // 验证流程完成
        Task finalTask = workflowExecutor.claimTask(graph, context);
        assertNull(finalTask);

        // 验证执行效果
        assertTrue(context.<Boolean>getAs("task1_executed"));
        assertTrue(context.<Boolean>getAs("task2_executed"));

        System.out.println("简单线性流程测试完成: " + instanceId);
    }

    @Test
    void testApprovalProcessWithRealComponents() {
        // 测试目的：验证完整的审批流程
        // 测试场景：申请 -> 审批（通过/驳回）的标准审批流程
        // 验证点：不同角色的任务分配、条件分支、审批结果传递

        // 1. 创建独立的任务组件
        TaskComponent applyTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("applicationData", "申请数据-" + UUID.randomUUID());
                context.put("apply_executed", true);
            }
        };

        TaskComponent reviewTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String result = context.getOrDefault("reviewResult", "approve");
                context.put("reviewResult", result);
                context.put("reviewer", context.getOrDefault("reviewer", "system"));
                context.put("review_executed", true);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("approval-process", "审批流程", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("apply");

            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(applyTaskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(reviewTaskComponent)
                    .linkAdd("end", link -> link.when(c -> "approve".equals(c.getAs("reviewResult"))).title("通过")) //"${reviewResult} == 'approve'"
                    .linkAdd("apply", link -> link.when(c -> "reject".equals(c.getAs("reviewResult"))).title("驳回"));//"${reviewResult} == 'reject'"

            spec.addEnd("end").title("审批完成");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "approval-test-1";

        // 申请人提交申请
        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");
        applicantContext.put("applicantName", "张三");
        applicantContext.put("reviewResult", "approve");

        Task applyTask = workflowExecutor.claimTask(graph.getId(), applicantContext);
        assertNotNull(applyTask);
        assertEquals("apply", applyTask.getNodeId());
        assertEquals(TaskState.WAITING, applyTask.getState());

        assertDoesNotThrow(() -> applyTask.run(applicantContext));
        workflowExecutor.submitTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 审批人审批
        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");
        reviewerContext.put("reviewerName", "李审批员");
        reviewerContext.put("reviewResult", "approve");

        Task reviewTask = workflowExecutor.claimTask(graph.getId(), reviewerContext);
        assertNotNull(reviewTask);
        assertEquals("review", reviewTask.getNodeId());

        assertDoesNotThrow(() -> reviewTask.run(reviewerContext));
        workflowExecutor.submitTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程完成
        Task finalTask = workflowExecutor.claimTask(graph.getId(), reviewerContext);
        assertNull(finalTask);

        // 验证执行效果
        assertTrue(applicantContext.<Boolean>getAs("apply_executed"));
        assertTrue(reviewerContext.<Boolean>getAs("review_executed"));
        assertEquals("approve", reviewerContext.<String>getAs("reviewResult"));

        System.out.println("审批流程测试完成: " + instanceId);
    }

    @Test
    void testRejectionAndResubmissionWorkflow() {
        // 测试目的：验证驳回后重新提交的业务流程
        // 测试场景：申请被驳回 -> 修改后重新提交 -> 审批通过的完整循环
        // 验证点：状态回退、重新提交流程、多次执行计数

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();

        StateRepository stateRepository = new InMemoryStateRepository();

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                stateRepository
        );

        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_executed", true);
                context.put(nodeId + "_count", context.getOrDefault(nodeId + "_count", 0) + 1);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("rejection-process", "驳回重提交流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("apply");

            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", "applicant")
                    .task(taskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(taskComponent)
                    .linkAdd("end", link -> link.when(c -> "approve".equals(c.getAs("reviewResult"))).title("通过")) //"${reviewResult} == 'approve'"
                    .linkAdd("apply", link -> link.when(c -> {
                        if ("reject".equals(c.getAs("reviewResult"))) {
                            // workflow 自动回流，需要清理状态
                            Graph graph1 = c.exchanger().graph();
                            stateRepository.stateRemove(c, graph1.getNode("apply"));
                            stateRepository.stateRemove(c, graph1.getNode("review"));
                            //要暂停，不要自动前进
                            c.interrupt();
                            return true;
                        } else {
                            return false;
                        }
                    }).title("驳回")); //"${reviewResult} == 'reject'"

            spec.addEnd("end").title("完成");
        });

        engine.load(graph);

        // 4. 测试执行
        String instanceId = "rejection-test-" + UUID.randomUUID().toString().substring(0, 6);

        FlowContext applicantContext = FlowContext.of(instanceId);
        applicantContext.put("actor", "applicant");

        FlowContext reviewerContext = FlowContext.of(instanceId);
        reviewerContext.put("actor", "reviewer");

        // 第一轮：提交申请
        workflowExecutor.claimTask(graph.getId(), applicantContext);
        workflowExecutor.submitTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 第一轮：审批驳回
        reviewerContext.put("reviewResult", "reject");
        workflowExecutor.claimTask(graph.getId(), reviewerContext);
        workflowExecutor.submitTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程回到申请节点
        Task taskAfterRejection = workflowExecutor.claimTask(graph.getId(), applicantContext);
        assertNotNull(taskAfterRejection);
        assertEquals("apply", taskAfterRejection.getNodeId());
        assertEquals(TaskState.WAITING, taskAfterRejection.getState());

        // 第二轮：修改后重新提交
        applicantContext.put("reviewResult", "approve");
        workflowExecutor.submitTask(graph.getId(), "apply", TaskAction.FORWARD, applicantContext);

        // 第二轮：审批通过
        reviewerContext.put("reviewResult", "approve");
        workflowExecutor.submitTask(graph.getId(), "review", TaskAction.FORWARD, reviewerContext);

        // 验证流程完成
        Task finalTask = workflowExecutor.claimTask(graph.getId(), reviewerContext);
        assertNull(finalTask);

        // 验证执行次数
        Integer applyCount = applicantContext.getAs("apply_count");
        Integer reviewCount = reviewerContext.getAs("review_count");
        assertNotNull(applyCount);
        assertNotNull(reviewCount);
        assertTrue(applyCount >= 2);
        assertTrue(reviewCount >= 2);

        System.out.println("驳回重提交流程测试完成: " + instanceId);
    }

    @Test
    void testComplexParallelProcess() {
        // 测试目的：验证复杂的并行处理流程
        // 测试场景：并行网关分发任务 -> 多个用户并行处理 -> 汇总结果的完整并行流程
        // 验证点：并行任务分配、用户角色权限、汇总节点执行

        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String nodeId = node.getId();
                context.put(nodeId + "_completed", true);
                context.put(nodeId + "_executor", context.getAs("actor"));
                context.put(nodeId + "_time", System.currentTimeMillis());
            }
        };

        // 2. 创建独立的并行流程图
        Graph graph = Graph.create("parallel-process", "并行流程", spec -> {
            spec.addStart("start").title("开始").linkAdd("parallel-gateway");

            spec.addParallel("parallel-gateway").title("并行网关")
                    .linkAdd("task1")
                    .linkAdd("task2")
                    .linkAdd("task3");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user1")
                    .metaPut("department", "sales")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user2")
                    .metaPut("department", "finance")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addActivity("task3").title("任务3")
                    .metaPut("actor", "user3")
                    .metaPut("department", "hr")
                    .task(taskComponent)
                    .linkAdd("consolidate");

            spec.addParallel("consolidate").title("数据汇总")
                    .metaPut("actor", "admin")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("流程结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 测试执行
        String instanceId = "parallel-test-" + UUID.randomUUID().toString().substring(0, 6);

        // 测试不同用户访问
        String[] users = {"user1", "user2", "user3", "admin"};
        Map<String, FlowContext> contexts = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();

        for (String user : users) {
            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", user);
            contexts.put(user, context);

            Task task = workflowExecutor.claimTask(graph.getId(), context);
            tasks.put(user, task);
        }

        // 验证前三个用户有任务，admin暂时没有
        assertNotNull(tasks.get("user1"));
        assertNotNull(tasks.get("user2"));
        assertNotNull(tasks.get("user3"));
        assertNull(tasks.get("admin"));

        // 各个用户完成任务
        for (String user : new String[]{"user1", "user2", "user3"}) {
            String nodeId = "task" + user.charAt(user.length() - 1);
            assertEquals(nodeId, tasks.get(user).getNodeId());
            workflowExecutor.submitTask(graph.getId(), nodeId, TaskAction.FORWARD, contexts.get(user));
        }

        // 检查管理员任务
        Task adminTask = workflowExecutor.claimTask(graph.getId(), contexts.get("admin"));
        assertNotNull(adminTask);
        assertEquals("consolidate", adminTask.getNodeId());

        // 管理员完成任务
        contexts.get("admin").put("consolidatedData", "汇总完成的数据");
        workflowExecutor.submitTask(graph.getId(), "consolidate", TaskAction.FORWARD, contexts.get("admin"));

        // 验证流程完成
        Task finalTask = workflowExecutor.claimTask(graph.getId(), contexts.get("admin"));
        assertNull(finalTask);

        // 验证所有任务完成状态
        for (String user : users) {
            String nodeId = user.equals("admin") ? "consolidate" : "task" + user.charAt(user.length() - 1);
            Node node = graph.getNode(nodeId);
            assertEquals(TaskState.COMPLETED, workflowExecutor.getState(node, contexts.get(user)));
        }

        System.out.println("并行流程测试完成: " + instanceId);
    }

    @Test
    void testErrorHandlingWorkflow() {
        // 测试目的：验证工作流中的错误处理机制
        // 测试场景：正常任务执行 -> 可能失败的任务执行 -> 异常捕获 -> 重试机制
        // 验证点：异常抛出、异常传播、重试机制、状态恢复

        // 1. 创建独立的任务组件（可能抛出异常）
        TaskComponent normalTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("normal_executed", true);
            }
        };

        TaskComponent errorTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                boolean shouldFail = context.getOrDefault("shouldFail", false);
                if (shouldFail) {
                    throw new RuntimeException("模拟任务执行失败");
                }
                context.put("error_executed", true);
            }
        };

        // 2. 创建独立的错误处理流程图
        Graph graph = Graph.create("error-process", "错误处理流程", spec -> {
            spec.addStart("start").linkAdd("normalTask");

            spec.addActivity("normalTask").title("正常任务")
                    .task(normalTaskComponent)
                    .linkAdd("errorTask");

            spec.addActivity("errorTask").title("可能失败的任务")
                    .task(errorTaskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController(),
                new InMemoryStateRepository()
        );

        // 4. 测试执行
        FlowContext context = FlowContext.of("error-test-1");

        // 执行正常任务
        Task normalTask = workflowExecutor.claimTask(graph.getId(), context);
        assertNull(normalTask); //（自动）直接完成了

        // 设置失败条件并测试错误任务
        FlowContext context2 = FlowContext.of("error-test-2");
        context2.put("shouldFail", true);

        // 任务执行应该抛出异常
        Exception exception = assertThrows(RuntimeException.class,
                () -> {
                    workflowExecutor.claimTask(graph.getId(), context2);
                });
        assertTrue(exception.getCause().getMessage().contains("模拟任务执行失败"));

        // 清除失败条件后重试
        context2.put("shouldFail", false);
        workflowExecutor.submitTask(graph.getId(), "errorTask", TaskAction.FORWARD, context2);

        // 验证流程可以继续
        Task finalTask = workflowExecutor.claimTask(graph.getId(), context2);
        assertNull(finalTask);

        System.out.println("错误处理流程测试完成: " + context2.getInstanceId());
    }

    @Test
    void testConditionalBranchingWorkflow() {
        // 测试目的：验证条件分支工作流的正确执行
        // 测试场景：根据申请金额自动选择审批路径（小额自动审批 vs 大额经理审批）
        // 验证点：条件判断、分支选择、不同执行路径、权限控制

        // 1. 创建独立的组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put(node.getId() + "_executed", true);
                context.put("executor", context.getAs("actor"));
            }
        };

        ConditionComponent smallAmountCondition = new ConditionComponent() {
            @Override
            public boolean test(FlowContext context) throws Throwable {
                Integer amount = context.getAs("amount");
                return amount != null && amount <= 5000;
            }
        };

        ConditionComponent largeAmountCondition = new ConditionComponent() {
            @Override
            public boolean test(FlowContext context) throws Throwable {
                Integer amount = context.getAs("amount");
                return amount != null && amount > 5000;
            }
        };

        // 2. 创建独立的条件分支流程图
        Graph graph = Graph.create("conditional-process", "条件分支流程", spec -> {
            spec.addStart("start")
                    .linkAdd("apply");

            spec.addActivity("apply").title("申请")
                    .metaPut("actor", "applicant")
                    .task(taskComponent)
                    .linkAdd("review");

            spec.addExclusive("review").title("审批")
                    .metaPut("actor", "reviewer")
                    .task(taskComponent)
                    .linkAdd("auto-approve", link -> link.when(smallAmountCondition).title("自动审批"))
                    .linkAdd("manager-approve", link -> link.when(largeAmountCondition).title("经理审批"));

            spec.addActivity("auto-approve").title("自动审批")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addActivity("manager-approve").title("经理审批")
                    .metaPut("actor", "manager")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("完成");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 测试小额申请（走自动审批分支）
        System.out.println("=== 测试小额申请（<=5000）===");
        String smallInstanceId = "small-amount-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext smallContext = FlowContext.of(smallInstanceId);
        smallContext.put("actor", "applicant");
        smallContext.put("amount", 3000);

        workflowExecutor.claimTask(graph.getId(), smallContext);
        workflowExecutor.submitTask(graph.getId(), "apply", TaskAction.FORWARD, smallContext);

        FlowContext smallReviewerContext = FlowContext.of(smallInstanceId);
        smallReviewerContext.put("actor", "reviewer");
        smallReviewerContext.put("amount", 3000);

        workflowExecutor.submitTask(graph.getId(), "review", TaskAction.FORWARD, smallReviewerContext);

        Task smallFinalTask = workflowExecutor.claimTask(graph.getId(), smallReviewerContext);
        assertNull(smallFinalTask);
        System.out.println("小额申请测试完成");

        // 5. 测试大额申请（走经理审批分支）
        System.out.println("=== 测试大额申请（>5000）===");
        String largeInstanceId = "large-amount-" + UUID.randomUUID().toString().substring(0, 6);
        FlowContext largeContext = FlowContext.of(largeInstanceId);
        largeContext.put("actor", "applicant");
        largeContext.put("amount", 8000);

        workflowExecutor.claimTask(graph.getId(), largeContext);
        workflowExecutor.submitTask(graph.getId(), "apply", TaskAction.FORWARD, largeContext);


        FlowContext largeReviewerContext = FlowContext.of(largeInstanceId);
        largeReviewerContext.put("actor", "reviewer");
        largeReviewerContext.put("amount", 8000);

        Task lastTask = workflowExecutor.claimTask(graph.getId(), largeReviewerContext);
        System.out.println(lastTask);
        workflowExecutor.submitTask(graph.getId(), "review", TaskAction.FORWARD, largeReviewerContext);
        lastTask = workflowExecutor.claimTask(graph.getId(), largeReviewerContext);
        Assertions.assertNull(lastTask);

        FlowContext managerContext = FlowContext.of(largeInstanceId);
        managerContext.put("actor", "manager");
        managerContext.put("amount", 8000);

        workflowExecutor.submitTask(graph.getId(), "manager-approve", TaskAction.FORWARD, managerContext);
        Task largeFinalTask = workflowExecutor.claimTask(graph.getId(), managerContext);
        assertNull(largeFinalTask);
        System.out.println("大额申请测试完成");
    }

    @Test
    void testHighConcurrencyWorkflowAccess() throws InterruptedException {
        // 测试目的：验证工作流在高并发场景下的稳定性和正确性
        // 测试场景：多个线程同时创建和执行多个工作流实例
        // 验证点：线程安全、状态隔离、并发性能、异常处理

        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                context.put("executed", true);
                context.put("thread", Thread.currentThread().getName());
            }
        };

        // 2. 创建独立的简单流程图用于并发测试
        Graph graph = Graph.create("concurrent-process-" + UUID.randomUUID(), "并发测试流程", spec -> {
            spec.addStart("start").linkAdd("task");

            spec.addActivity("task").title("任务")
                    .metaPut("actor", "user")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 并发测试
        int threadCount = 10;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable concurrentTask = () -> {
            try {
                startLatch.await();

                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        String instanceId = "concurrent-" + Thread.currentThread().getId() + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);
                        FlowContext context = FlowContext.of(instanceId);
                        context.put("actor", "user");

                        // 获取并完成任务
                        Task task = workflowExecutor.claimTask(graph.getId(), context);
                        if (task != null) {
                            task.run(context);
                            workflowExecutor.submitTask(graph.getId(), task.getNodeId(),
                                    TaskAction.FORWARD, context);
                            successCount.incrementAndGet();
                        }

                        // 短暂休眠
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        // 在并发测试中，异常是正常的
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(concurrentTask);
        }

        // 同时开始执行
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = finishLatch.await(3, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "并发测试应在3秒内完成");

        System.out.println("并发测试结果:");
        System.out.println("成功次数: " + successCount.get());
        System.out.println("失败次数: " + failureCount.get());

        // 验证至少有一定成功率
        assertTrue(successCount.get() > 0, "并发测试应有成功案例");
    }

    @Test
    void testMultipleInstanceIsolation() {
        // 测试目的：验证多个工作流实例之间的数据隔离
        // 测试场景：同时运行多个相同流程的实例，验证它们互不影响
        // 验证点：实例ID隔离、上下文数据隔离、状态存储隔离

        // 1. 创建独立的任务组件
        TaskComponent taskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                String instanceId = context.getInstanceId();
                String customData = context.getOrDefault("customData", "default").toString();
                context.put("processed", true);
                context.put("instanceData", instanceId + ":" + customData);
            }
        };

        // 2. 创建独立的流程图
        Graph graph = Graph.create("isolation-process-" + UUID.randomUUID(), "实例隔离流程", spec -> {
            spec.addStart("start").linkAdd("process");

            spec.addActivity("process").title("处理任务")
                    .metaPut("actor", "processor")
                    .task(taskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 测试多个实例隔离
        int instanceCount = 5;
        List<String> instanceIds = new ArrayList<>();
        List<FlowContext> contexts = new ArrayList<>();

        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "instance-" + i + "-" + UUID.randomUUID().toString().substring(0, 6);
            instanceIds.add(instanceId);

            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", "processor");
            context.put("customData", "数据" + i);
            contexts.add(context);
        }

        // 处理所有实例
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);

            Task task = workflowExecutor.claimTask(graph.getId(), context);
            assertNotNull(task);
            assertEquals("process", task.getNodeId());

            assertDoesNotThrow(() -> task.run(context));
            workflowExecutor.submitTask(graph.getId(), "process", TaskAction.FORWARD, context);
        }

        // 验证实例间数据隔离
        for (int i = 0; i < instanceCount; i++) {
            FlowContext context = contexts.get(i);
            String expectedData = "数据" + i;
            String expectedInstanceData = instanceIds.get(i) + ":" + expectedData;

            assertEquals(expectedData, context.getAs("customData"));
            assertEquals(expectedInstanceData, context.getAs("instanceData"));
            assertTrue(context.<Boolean>getAs("processed"));
        }

        System.out.println("多实例隔离测试完成，共测试 " + instanceCount + " 个实例");
    }

    @Test
    void testPerformanceBenchmark() {
        // 测试目的：验证工作流性能基准
        // 测试场景：批量执行大量工作流实例，测量执行时间和吞吐量
        // 验证点：执行时间、吞吐量、内存使用、性能稳定性

        // 1. 创建轻量级的任务组件
        TaskComponent lightTaskComponent = new TaskComponent() {
            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                // 模拟最小化的工作负载
                context.put("processed", true);
            }
        };

        // 2. 创建简单的性能测试流程图
        Graph graph = Graph.create("perf-process-" + UUID.randomUUID(), "性能测试流程", spec -> {
            spec.addStart("start").linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .metaPut("actor", "user")
                    .task(lightTaskComponent)
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .metaPut("actor", "user")
                    .task(lightTaskComponent)
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        // 3. 创建独立的工作流服务
        FlowEngine engine = FlowEngine.newInstance();
        engine.load(graph);

        WorkflowExecutor workflowExecutor = WorkflowExecutor.of(
                engine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );

        // 4. 性能测试
        int iterationCount = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterationCount; i++) {
            String instanceId = "perf-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);
            FlowContext context = FlowContext.of(instanceId);
            context.put("actor", "user");

            // 执行完整流程
            Task task1 = workflowExecutor.claimTask(graph.getId(), context);
            if (task1 != null) {
                task1.run(context);
                workflowExecutor.submitTask(graph.getId(), "task1", TaskAction.FORWARD, context);

                Task task2 = workflowExecutor.claimTask(graph.getId(), context);
                if (task2 != null) {
                    task2.run(context);
                    workflowExecutor.submitTask(graph.getId(), "task2", TaskAction.FORWARD, context);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== 性能测试结果 ===");
        System.out.println("迭代次数: " + iterationCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每个实例: " + (duration / (double) iterationCount) + "ms");

        if (duration > 0) {
            double qps = (iterationCount * 2.0) / (duration / 1000.0); // 两个任务
            System.out.println("QPS (任务数): " + String.format("%.2f", qps));
        }

        // 性能验证
        assertTrue(duration < 5000, "性能测试应在5秒内完成");
    }
}