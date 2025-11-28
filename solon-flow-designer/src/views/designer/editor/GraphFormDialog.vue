<template>
    <div class="formDialog" :style="{ 'display': state.isOpen? 'block' : 'none' }" >
        <div class="header">
            <div class="title">流程图：{{ formData.title || "" }}</div>
            <div class="closeBtn" @click="toClose">
                <font-awesome-icon icon="fa-solid fa-xmark" />
            </div>
        </div>
        <div class="content">
            <a-form ref="formRef" :model="formData" layout="vertical">
                <a-form-item label="图ID">
                    {{ formData.id }}
                </a-form-item>
                <a-form-item label="标题">
                    <a-input v-model:value="formData.title" @change="onChange"/>
                </a-form-item>
                <a-form-item label="驱动">
                    <a-input v-model:value="formData.driver" @change="onChange"/>
                </a-form-item>
                <a-form-item label="元数据">
                    <MetaInputField v-model:value="formData.meta" @change="onChange" />
                </a-form-item>
            </a-form>
        </div>
    </div>
</template>
<script setup>
import { ref,reactive, nextTick,toRaw } from 'vue'
import MetaInputField from '@/components/CodeEditor/MetaInputField.vue';

const emit = defineEmits(['change'])
const state = reactive({ isOpen: false }) // 表单对话框的状态，默认为关闭状态
const formRef = ref(null)
let formData = reactive({ id: null, title: null, driver: null, meta:{}})
let _graph = null
let _currentEditGraph = null // 当前编辑的流图

function toClose() {
    state.isOpen = false
    formRef.value.resetFields()
}
function show(graph, currentEditGraph) {
    if(!currentEditGraph.title){
      currentEditGraph.title = "";
    }

    state.isOpen = true
    _graph = graph
    _currentEditGraph = currentEditGraph
    nextTick(() => {
        formData = Object.assign(formData,currentEditGraph)
    })
}
function onChange() {
    _currentEditGraph = Object.assign(_currentEditGraph,{ ...toRaw(formData) })
    emit('change',_currentEditGraph)
}

defineExpose({ show, toClose })
</script>