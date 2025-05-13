<template>
    <a-space style="width: 100%" direction="vertical">
                    <a-row :gutter="5" v-for="(metaItem,index) in state.metas" :key="metaItem.key">
                        <a-col :span="10">
                            <a-input v-model:value="metaItem.key" @change="onChange"/>
                        </a-col>
                        <a-col :span="10">
                            <a-input v-model:value="metaItem.value" @change="onChange"/>
                        </a-col>
                        <a-col :span="2">
                            <a-button type="link" danger @click="removeMeta(index)"><CloseOutlined /></a-button>
                        </a-col>
                    </a-row>
                    <a-row :gutter="5">
                        <a-col :span="10">
                            <a-input v-model:value="state.tempMeta.key" />
                        </a-col>
                        <a-col :span="10">
                            <a-input v-model:value="state.tempMeta.value"/>
                        </a-col>
                        <a-col :span="2">
                            <a-button type="link" @click="toAddMeta"><CheckOutlined /></a-button>
                        </a-col>
                    </a-row>
                </a-space>
</template>
<script setup>

import { ref,reactive,watch } from 'vue'
import { CheckOutlined,CloseOutlined } from '@ant-design/icons-vue';

const props = defineProps({
    value: {
        type: Object,
        default: () => {}
    }
})

const emit = defineEmits(['change','update:value'])

watch(() => props.value, (newValue, oldValue) => {
    console.log('meta newValue',newValue)
    if(!newValue){
        state.metas = []
    }else{
        let data = []
        for (const key in newValue) {
            data.push({ key: key, value: newValue[key] })
        }
        state.metas = data;
    }
})

const state = reactive({
    metas: [],
    tempMeta: { key: null, value: null }
})


function toAddMeta() {
    if (!state.tempMeta.key || !state.tempMeta.value) { return }
    state.metas.push({ key: state.tempMeta.key, value: state.tempMeta.value })
    state.tempMeta.key = null
    state.tempMeta.value = null
    onChange()
}

function removeMeta(index) {
    state.metas.splice(index, 1)
    onChange()
}

function onChange() {
    // 将state.metas转换成Object
    let metas = {}
    state.metas.forEach(metaItem => {
        metas[metaItem.key] = metaItem.value
    })
    emit('update:value', metas)
    emit('change', metas)
}

defineExpose({
})

</script>