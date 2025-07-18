<template>
    <a-flex vertical gap="small">
        <div class="script-content" @click="openEditor">
            <!-- 脚本内容，固定高度为80px,超出了会自动隐藏，从上到下渐变模糊,点击后会弹出编辑框 -->
            <template v-if="state.scriptContent">
                {{ state.scriptContent }}
            </template>
            <template v-else>
                点击查看元数据
            </template>
        </div>
        <a-modal :width="780" :open="state.isOpenEditor" title="元数据编辑（YAML or JSON）" destroyOnClose
            @cancel="state.isOpenEditor = false" @ok="submitScriptContent">
            <CodeEditor v-model:value="state.scriptContentShadow" @change="onChange" lang="yaml"
                :contentHeight="contentHeight" />
        </a-modal>
    </a-flex>
</template>
<script setup>
import { onMounted, reactive, watch } from 'vue';
import CodeEditor from './Index.vue';
import yamlUtils from 'js-yaml'

const emit = defineEmits(['update:value', 'change'])

const props = defineProps({
    value: {
        type: [String,Object],
        default: null,
    },
    contentHeight: {
        type: String,
        default: '400px',
    },
    lang: {
        type: String,
        default: 'java',
    },
})


const state = reactive({
    scriptContent: "",
    scriptContentShadow: "",
    isOpenEditor: false
});

watch(() => props.value, (newValue) => {
    let v = newValue;
    if(typeof newValue === 'object'){
        v = yamlUtils.dump(newValue)
    }
    state.scriptContent = v;
    state.scriptContentShadow = v;
})
onMounted(() => {
    let v = props.value;
    if(typeof props.value === 'object'){
        v = yamlUtils.dump(props.value)
    }
    state.scriptContent = v;
    state.scriptContentShadow = v;
})
function openEditor() {
    state.isOpenEditor = true;
}

function onChange(value) {
    state.scriptContentShadow = value;
}

function submitScriptContent() {
    state.isOpenEditor = false;
    state.scriptContent = state.scriptContentShadow;
    if (!state.scriptContentShadow) {
      state.scriptContentShadow = '{}';
    }

    try {
      let metaObj = yamlUtils.load(state.scriptContent);

      emit('update:value', metaObj)
      emit('change', metaObj)
    } catch (e) {
      alert('格式错误，请检查')
    }
}
</script>
<style scoped>
.script-content {
    height: 80px;
    overflow: hidden;
    border: 1px solid #ddd;
    color: #a9a7a7;
    padding: 8px;
    cursor: pointer;
    border-radius: 6px;
    white-space: pre-wrap;
    /* 保留换行符和空格 */
    word-wrap: break-word;
    /* 长单词自动换行 */
    position: relative;
    /* 为遮罩层定位 */
}

.script-content::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 20px;
    /* 模糊区域的高度 */
    background: linear-gradient(to top, rgba(255, 255, 255, 1), rgba(255, 255, 255, 0));
    /* 从上到下渐变透明 */
    mask: linear-gradient(to top, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0));
    /* 遮罩效果 */
    pointer-events: none;
    /* 防止遮罩层拦截点击事件 */
}

.script-content:hover {
    border-color: #409eff;
    /* 鼠标悬停时改变边框颜色 */
}
</style>