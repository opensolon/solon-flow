<template>
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
              <a-form-item label="元信息(JSON)">
                  <JsonInputField v-model:value="formData.meta" @change="onChange"></JsonInputField>
              </a-form-item>
          </a-form>
  </template>
  <script setup>
  import { ref,reactive, nextTick,watch } from 'vue'
  import ScriptInputField from '@/components/CodeEditor/ScriptInputField.vue'
  import JsonInputField from '@/components/CodeEditor/JsonInputField.vue';
  import MetaDataField from '../editor/MetaDataField.vue';
  
  const emit = defineEmits(['change'])
  const formRef = ref(null)
  let formData = reactive({ id: null, title: null, task: null, when: null,meta:null})
  
  let _currentEditNode = null // 当前编辑的节点
  
  function resetFields() {
      formRef.value.resetFields()
  }
  
  function onShow(currentEditNode){
      _currentEditNode = currentEditNode
      formData.id = _currentEditNode.id
      const data = _currentEditNode.getData()
      if(data){
          formData.id = data.id
          formData.title = data.title || ''
          formData.task = data.task || ''
          formData.when = data.when || ''
          formData.meta = data.meta || {}
          onChange()
      }
  }
  
  function onChange() {
      _currentEditNode.setData({ title: formData.title, task: formData.task, when: formData.when,meta:formData.meta })
      _currentEditNode.emit("node:data:changed",{})
      emit('change',formData)
  }
  
  defineExpose({
      resetFields,
      onShow
  })
  </script>