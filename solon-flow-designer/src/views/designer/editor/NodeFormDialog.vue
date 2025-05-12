<template>
    <div class="formDialog" :style="{ 'display': state.isOpen ? 'block' : 'none' }" >
        <div class="header">
            <div class="title">1</div>
            <div class="closeBtn" @click="toClose">
                <font-awesome-icon icon="fa-solid fa-xmark" />
            </div>
        </div>
    </div>
</template>
<script setup>
import { reactive, nextTick } from 'vue'
const state = reactive({ isOpen: false }) // 表单对话框的状态，默认为关闭状态

function toClose() {
    state.isOpen = false
}
function show() { 
    state.isOpen = true
    nextTick(() => {
        console.log('show')
    })
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