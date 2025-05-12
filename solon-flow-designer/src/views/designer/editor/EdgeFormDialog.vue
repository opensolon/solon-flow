<template>
<div class="formDialog" :style="{ 'display': state.isOpen ? 'block' : 'none' }" >
    <div class="header">
        <div class="title">{{ formData.title || "" }}</div>
        <div class="closeBtn" @click="toClose">
            <font-awesome-icon icon="fa-solid fa-xmark" />
        </div>
    </div>
    <div class="content">
        <a-form :model="formData" layout="vertical">
            <a-form-item label="下一个节点ID" name="nextId">
                {{ formData.nextId }}
            </a-form-item>
            <a-form-item label="标题">
                <a-input v-model:value="formData.title" @change="onChange"/>
            </a-form-item>
            <a-form-item label="条件">
                <ScriptInputField v-model:value="formData.condition" @change="onChange">
                </ScriptInputField>
            </a-form-item>
        </a-form>
    </div>
</div>
</template>
<script setup>
import { reactive, nextTick } from 'vue'
import ScriptInputField from '@/components/CodeEditor/ScriptInputField.vue'

const state = reactive({ isOpen: false }) // 表单对话框的状态，默认为关闭状态
let formData = reactive({ nextId: null, title: null, condition: null })
let _graph = null
let _currentEditEdge = null // 当前编辑的边

function toClose() {
    state.isOpen = false
}
function show(graph,currentEditEdge) { 
    state.isOpen = true
    _graph = graph
    _currentEditEdge = currentEditEdge
    nextTick(() => {
        formData.nextId = _currentEditEdge.target.cell
        const labels = _currentEditEdge.target.cellData.labels
        if(labels && labels.length > 0){
            formData.title = labels[0]
        }
        const data = _currentEditEdge.getData()
        if(data){
            formData.title = data.title
            formData.condition = data.condition
        }
    })
}

function onChange() {
    _currentEditEdge.setData({
        nextId: formData.nextId,
        title: formData.title,
        condition: formData.condition
    })
    if(formData.title){
        _currentEditEdge.setLabels([formData.title])
    }else{
        _currentEditEdge.setLabels([])
    }
    
}

defineExpose({
    toClose,
    show
})
</script>