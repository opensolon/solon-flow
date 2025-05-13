const nodeTypeDef = {
    "start": {
        "type": "start",
        "title": "开始",
        "color": "#0820e2",
        "icon": "fa-solid fa-play",
    },
    "end": {
        "type": "end",
        "title": "结束",
        "color": "#5d1dd0",
        "icon": "fa-solid fa-stop",
    },
    "inclusive": {
        "type": "inclusive",
        "title": "包含网关",
        "color": "#f1711f",
        "icon": "fa-solid fa-genderless",
    },
    "exclusive": {
        "type": "exclusive",
        "title": "排他网关",
        "color": "#ae0e6a",
        "icon": "fa-solid fa-xmark",
    },
    "parallel": {
        "type": "parallel",
        "title": "并行网关",
        "color": "#7e48ee",
        "icon": "fa-solid fa-plus",
    },
    "activity": {
        "type": "activity",
        "title": "活动节点",
        "color": "#047cc2",
        "icon": "fa-solid fa-user",
    },
    "script": {
        "type": "script",
        "title": "脚本节点",
        "color": "#2cdcae",
        "icon": "fa-solid fa-file",
    },
    "subFlow": {
        "type": "subFlow",
        "title": "子流程",
        "color": "#f4a63a",
        "icon": "fa-solid fa-diagram-next",
    },
}

const groupMap = [
    {"title":"开关节点","value":"switch","nodes":["start","end"]},
    {"title":"网关节点","value":"gateway","nodes":["inclusive","exclusive","parallel"]},
    {"title":"活动节点","value":"activity","nodes":["activity","script","subFlow"]}
]

export {nodeTypeDef,groupMap}