import {ref, reactive} from 'vue'

/**
 * 节点表单组合式函数
 * 提供节点表单的通用逻辑，包括数据管理、事件处理等
 *
 * @param {Function} emit - 组件的事件发射器，用于触发自定义事件
 * @param emitEvents - 自定义事件列表，用于动态触发事件
 * @returns {Object} 包含表单相关属性和方法的对象
 */
export function useNodeForm(emit, emitEvents = []) {
    // 表单引用，用于访问表单实例方法
    const formRef = ref(null)

    // 表单数据模型，使用 reactive 创建响应式对象
    const formData = reactive({
        id: null,     // 节点ID
        title: null,  // 节点标题
        task: null,   // 任务配置
        when: null,   // 任务条件
        meta: null    // 元数据
    })

    // 当前编辑的节点实例
    let _currentEditNode = null

    /**
     * 重置表单字段
     * 调用表单实例的 resetFields 方法清除验证状态
     */
    function resetFields() {
        formRef.value.resetFields()
    }

    /**
     * 显示节点数据
     * 当需要编辑节点时调用，将节点数据填充到表单中
     *
     * @param {Object} currentEditNode - 当前要编辑的节点对象
     */
    function onShow(currentEditNode) {
        _currentEditNode = currentEditNode
        formData.id = _currentEditNode.id

        // 获取节点数据并填充到表单
        const data = _currentEditNode.getData()
        console.log(data)
        if (data) {
            formData.id = data.id
            formData.title = data.title || ''
            formData.task = data.task || ''
            formData.when = data.when || ''
            formData.meta = data.meta || {}

            // 数据加载完成后触发一次 onChange 以同步数据
            onChange()
        }
    }

    /**
     * 表单数据变更处理
     * 当表单数据发生变化时调用，同步数据到节点并触发相关事件
     */
    function onChange() {
        console.log(formData)
        // 将表单数据同步到节点
        _currentEditNode.setData({
            id: formData.id,
            title: formData.title,
            task: formData.task,
            when: formData.when,
            meta: formData.meta
        }, {
            overwrite: true  // 覆盖原有数据
        })

        // 触发节点数据变更事件
        _currentEditNode.emit("node:data:changed", {})

        // 动态触发所有定义的事件
        emitEvents.forEach(eventName => {
            if (typeof emit === 'function') {
                emit(eventName, formData)
            }
        })

    }

    // 返回表单相关的属性和方法供组件使用
    return {
        formRef,      // 表单引用
        formData,     // 表单数据
        resetFields,  // 重置表单方法
        onShow,       // 显示节点数据方法
        onChange      // 数据变更处理方法
    }
}
