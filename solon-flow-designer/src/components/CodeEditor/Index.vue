<template>
    <div ref="codemirror" class="codemirror"></div>
  </template>
  <script setup>
  import { EditorView, basicSetup } from 'codemirror'
  import { EditorState } from "@codemirror/state"
  import { keymap } from '@codemirror/view'
  import { indentWithTab } from '@codemirror/commands'
  import { javascript } from '@codemirror/lang-javascript'
  import { java } from '@codemirror/lang-java'
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
  
  const props = defineProps({
    value: {
      type: String,
      default: null,
    },
    contentHeight: {
      type: String,
      default: '200px',
    },
    lang: {
      type: String,
      default: 'javascript',
    },
  })
  const emit = defineEmits(['update:value', 'change'])
  
  const codemirror = ref()
  let view
  onMounted(() => {
    const onUpdate = EditorView.updateListener.of(v => {
      if (v.docChanged) {
        emit('update:value', v.state.doc.toString())
        emit('change', v.state.doc.toString())
      }
    })
  
    var langType = null
    if (props.lang === 'java') {
      langType = java()
    } else if (props.lang === 'javascript') {
      langType = javascript()
    }
  
    view = new EditorView({
      state: EditorState.create({
        doc: props.value,
        extensions: [
          basicSetup,
          keymap.of([indentWithTab]),
          onUpdate,
          langType,
        ],
      }),
      parent: codemirror.value,
    })
  })
  
  onUnmounted(() => {
    if (view) {
      view.destroy()
    }
  })
  
  watch(
    () => props.value,
    () => {
      const currentValue = view ? view.state.doc.toString() : ''
      if (view && props.value !== currentValue) {
        view.dispatch({
          changes: {
            from: 0,
            to: currentValue.length,
            insert: props.value || '',
          },
        })
      }
      // reloadEditor(props.value)
    }
  )
  
  function reloadEditor(d) {
    nextTick(() => {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: d },
      })
    })
  }
  
  function setDoc(doc) {
    reloadEditor(doc)
  }
  
  defineExpose({
    reloadEditor,
    setDoc,
  })
  </script>
  <style>
  /* 这个$props没有写错,不要改 */
  .cm-editor {
    height: v-bind('$props.contentHeight');
    font-size: 16px;
  }
  </style>
  