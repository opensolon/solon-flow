<template>
    <a-form ref="formRef" :model="formData" layout="vertical">
              <a-form-item label="节点ID" name="id">
                  {{ formData.id }}
              </a-form-item>
              <a-form-item label="标题">
                  <a-input v-model:value="formData.title" @change="onChange"/>
              </a-form-item>
              <a-form-item label="任务">
                  <TaskInputField v-model:value="formData.task" @change="onChange" />
              </a-form-item>
              <a-form-item label="任务条件">
                  <ConditionInputField v-model:value="formData.when" @change="onChange" />
              </a-form-item>
              <a-form-item label="元数据">
                  <MetaInputField v-model:value="formData.meta" @change="onChange" />
              </a-form-item>
          </a-form>
  </template>
  <script setup>
  import ConditionInputField from '@/components/CodeEditor/ConditionInputField.vue'
  import TaskInputField from '@/components/CodeEditor/TaskInputField.vue'
  import MetaInputField from '@/components/CodeEditor/MetaInputField.vue';
  import {useNodeForm} from "@/views/designer/nodeForm/public/baseNodeForm.js";

  // 显式定义事件列表
  const emitEvents = ['change']
  const emit = defineEmits(['change'])
  // 同时传递 emit 函数和事件列表
  const {formRef, formData, resetFields, onShow, onChange} = useNodeForm(emit, emitEvents)

  
  defineExpose({
      resetFields,
      onShow
  })
  </script>