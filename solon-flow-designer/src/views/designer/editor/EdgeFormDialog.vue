<template>
<div class="formDialog" :style="{ 'display': state.isOpen ? 'block' : 'none' }" >
    <div class="header">
        <div class="title">连接：{{ formData.title || "" }}</div>
        <div class="closeBtn" @click="toClose">
            <font-awesome-icon icon="fa-solid fa-xmark" />
        </div>
    </div>
    <div class="content">
        <a-form ref="formRef" :model="formData" layout="vertical">
            <a-form-item label="下一个节点ID" name="nextId">
                {{ formData.nextId }}
            </a-form-item>
            <a-form-item label="标题">
                <a-input v-model:value="formData.title" @change="onChange"/>
            </a-form-item>
            <a-form-item label="连接条件">
                <ConditionInputField v-model:value="formData.condition" @change="onChange" />
            </a-form-item>
            <a-form-item label="元数据">
                <MetaInputField v-model:value="formData.meta" @change="onChange">
                </MetaInputField>
            </a-form-item>
        </a-form>
    </div>
</div>
</template>
<script setup>
import { ref,reactive, nextTick } from 'vue'
import ConditionInputField from '@/components/CodeEditor/ConditionInputField.vue'
import MetaInputField from '@/components/CodeEditor/MetaInputField.vue'

const state = reactive({ isOpen: false }) // 表单对话框的状态，默认为关闭状态
const formRef = ref(null)
let formData = reactive({ nextId: null, title: null, condition: null })
let _graph = null
let _currentEditEdge = null // 当前编辑的边

function toClose() {
    state.isOpen = false
    formRef.value.resetFields()
}
function show(graph,currentEditEdge) { 
    state.isOpen = true
    _graph = graph
    _currentEditEdge = currentEditEdge
    nextTick(() => {
        formData.id = _currentEditEdge.id
        formData.nextId = _currentEditEdge.target.cell
        const data = _currentEditEdge.getData()
        if(data){
            formData.condition = data.condition
            formData.title = data.title
        }else{
            formData.condition = null
            formData.title = null
        }
    })
}

function onChange() {
    _currentEditEdge.setData({
        id:formData.id,
        nextId: formData.nextId,
        title: formData.title,
        condition: formData.condition,
        meta: formData.meta
    },{
        overwrite:true
      })
    if(formData.title){
        _currentEditEdge.setLabels([formData.title])
    }else if(formData.condition){
        _currentEditEdge.setLabels([formData.condition])
    }else{
      _currentEditEdge.setLabels([])
    }
    
}

defineExpose({
    toClose,
    show
})
</script>