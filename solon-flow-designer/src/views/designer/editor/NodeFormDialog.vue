<template>
    <div class="formDialog" :style="{ 'display': state.isOpen ? 'block' : 'none' }" >
        <div class="header">
            <div class="title">节点：{{ formData.title || "" }}</div>
            <div class="closeBtn" @click="toClose">
                <font-awesome-icon icon="fa-solid fa-xmark" />
            </div>
        </div>
        <div class="content">
        <a-form ref="formRef" :model="formData" layout="vertical">
            <a-form-item label="节点ID" name="id">
                {{ formData.id }}
            </a-form-item>
            <a-form-item label="标题">
                <a-input v-model:value="formData.title" @change="onChange"/>
            </a-form-item>
            <a-form-item label="任务">
                <ScriptInputField v-model:value="formData.task" @change="onChange">
                </ScriptInputField>
            </a-form-item>
            <a-form-item label="任务条件">
                <ScriptInputField v-model:value="formData.when" @change="onChange">
                </ScriptInputField>
            </a-form-item>
            <a-form-item label="元信息">
                <MetaDataField v-model:value="formData.meta" @change="onChange"></MetaDataField>
            </a-form-item>
        </a-form>
    </div>
    </div>
</template>
<script setup>
import { ref,reactive, nextTick,watch } from 'vue'
import ScriptInputField from '@/components/CodeEditor/ScriptInputField.vue'
import MetaDataField from './MetaDataField.vue';

const state = reactive({ 
    isOpen: false, // 表单对话框的状态，默认为关闭状态
}) 
const formRef = ref(null)
const metaDataFieldRef = ref(null)
let formData = reactive({ id: null, title: null, task: null, when: null,meta:null})
let _graph = null
let _currentEditNode = null // 当前编辑的节点

function toClose() {
    state.isOpen = false
    formRef.value.resetFields()
}
function show(graph,currentEditNode) { 
    state.isOpen = true
    _graph = graph
    _currentEditNode = currentEditNode
    nextTick(() => {
        formData.id = _currentEditNode.id
        const data = _currentEditNode.getData()
        if(data){
            formData.title = data.title
            formData.task = data.task
            formData.when = data.when
            formData.meta = data.meta
        }
    })
}

function onChange() {
    _currentEditNode.setData({ title: formData.title, task: formData.task, when: formData.when,meta:formData.meta })
    _currentEditNode.emit("node:data:changed",{})
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