<template>
    <div class="formDialog" :style="{ 'display': state.isOpen ? 'block' : 'none' }" >
        <div class="header">
            <div class="title">节点：{{ state.title || "" }}</div>
            <div class="closeBtn" @click="toClose">
                <font-awesome-icon icon="fa-solid fa-xmark" />
            </div>
        </div>
        <div class="content">
            <Component ref="nodeFormRef" @change="onChange" :is="myComponent"></Component>
        </div>
    </div>
</template>
<script setup>
import { ref,reactive, nextTick,watch } from 'vue'
import StartNodeForm from '../nodeForm/StartNodeForm.vue'
import EndNodeForm from '../nodeForm/EndNodeForm.vue'
import InclusiveNodeForm from '../nodeForm/InclusiveNodeForm.vue'
import ExclusiveNodeForm from '../nodeForm/ExclusiveNodeForm.vue'
import ParallelNodeForm from '../nodeForm/ParallelNodeForm.vue'
import ActivityNodeForm from '../nodeForm/ActivityNodeForm.vue'

const nodeTypeFormMap = { // 节点类型与表单组件的映射关系，用于根据节点类型动态加载对应的表单组件
    start: StartNodeForm, // 开始节点的表单组件
    end: EndNodeForm, // 结束节点的表单组件
    inclusive: InclusiveNodeForm, // 包容网关节点的表单组件
    exclusive: ExclusiveNodeForm, // 排他网关节点的表单组件
    parallel: ParallelNodeForm, // 并行网关节点的表单组件
    activity: ActivityNodeForm, // 活动节点的表单组件
    default: StartNodeForm // 默认表单组件
}
let myComponent = ref(null) // 动态加载的表单组件
const state = reactive({ 
    isOpen: false, // 表单对话框的状态，默认为关闭状态
    title: null,
}) 
const nodeFormRef = ref(null) // 表单组件的引用]
let _graph = null
let _currentEditNode = null // 当前编辑的节点

function toClose() {
    state.isOpen = false
    if(nodeFormRef.value.resetFields){
        nodeFormRef.value.resetFields()
    }
    
}
function show(graph,currentEditNode) { 
    state.isOpen = true
    _graph = graph
    _currentEditNode = currentEditNode
    myComponent=nodeTypeFormMap[_currentEditNode.shape]
    nextTick(() => {
        
        nodeFormRef.value.onShow(_currentEditNode)
    })
}

function onChange(formData) { // 当表单数据发生变化时，更新当前节点的数据，并触发节点数据变化事件
    state.title = formData.title || ""
}

defineExpose({
    toClose,
    show
})
</script>
<style lang="less">
.formDialog {
    width: 350px;
    max-height: 500px;
    overflow-y: auto;
    background-color: #fff;
    border-radius: 10px;
    padding: 10px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    position: absolute;
    top: 30px;
    right: 30px;

    &::-webkit-scrollbar {
        display: none;
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #e8e8e8;
        padding-bottom: 10px;
        margin-bottom: 10px;
    }

    .closeBtn {
        width: 18px;
        height: 18px;
        color: #949494;
        border-radius: 50%;

        font-size: 14px;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        display: flex;

        &:hover {

            -moz-box-shadow: 1px 1px 7px 1px rgba(181, 183, 199, 0.73);
            box-shadow: 1px 1px 7px 1px rgba(181, 183, 199, 0.73);
        }
    }
}
</style>