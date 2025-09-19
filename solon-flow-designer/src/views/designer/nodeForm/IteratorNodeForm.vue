<template>
    <a-form ref="formRef" :model="formData" layout="vertical">
        <a-form-item label="节点ID" name="id">
            {{ formData.id }}
        </a-form-item>
        <a-form-item label="标题">
            <a-input v-model:value="formData.title" @change="onChange" />
        </a-form-item>
        <a-form-item label="元数据">
            <MetaInputField v-model:value="formData.meta" @change="onChange" />
        </a-form-item>
    </a-form>
</template>
<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import MetaInputField from "@/components/CodeEditor/MetaInputField.vue";

import { useNodeForm } from "@/views/designer/nodeForm/public/baseNodeForm.js";

// 显式定义事件列表
const emitEvents = ['change']
const emit = defineEmits(['change'])
// 同时传递 emit 函数和事件列表
const { formRef, formData, resetFields, onShow, onChange } = useNodeForm(emit, emitEvents)

formData.meta = "{'$for': '','$in': ''}"

defineExpose({
    resetFields,
    onShow
})
</script>