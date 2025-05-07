const nodeTypeDef = {
    "start": {
        "type": "start",
        "name": "开始",
        "color": "#0820e2",
        "icon": "fa-solid fa-play",
    },
    "end": {
        "type": "end",
        "name": "结束",
        "color": "#5d1dd0",
        "icon": "fa-solid fa-stop",
    },
    "inclusive": {
        "type": "inclusive",
        "name": "包含网关",
        "color": "#f1711f",
        "icon": "fa-solid fa-genderless",
    },
    "exclusive": {
        "type": "exclusive",
        "name": "排他网关",
        "color": "#ae0e6a",
        "icon": "fa-solid fa-xmark",
    },
    "parallel": {
        "type": "parallel",
        "name": "并行网关",
        "color": "#7e48ee",
        "icon": "fa-solid fa-plus",
    },
    "activity": {
        "type": "activity",
        "name": "活动节点",
        "color": "#047cc2",
        "icon": "fa-solid fa-user",
    },
    "script": {
        "type": "script",
        "name": "脚本节点",
        "color": "#2cdcae",
        "icon": "fa-solid fa-file",
    },
    "subFlow": {
        "type": "subFlow",
        "name": "子流程",
        "color": "#f4a63a",
        "icon": "fa-solid fa-diagram-next",
    },
}

const groupMap = [
    {"name":"开关节点","value":"switch","nodes":["start","end"]},
    {"name":"网关节点","value":"gateway","nodes":["inclusive","exclusive","parallel"]},
    {"name":"活动节点","value":"activity","nodes":["activity","script","subFlow"]}
]

export {nodeTypeDef,groupMap}