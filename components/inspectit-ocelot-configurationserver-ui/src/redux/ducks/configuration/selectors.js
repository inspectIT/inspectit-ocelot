import { createSelector } from 'reselect'
import {map} from 'lodash';

const configurationSelector = state => state.configuration;

const asTreeNode = (parent, node) => {
    const {type, name} = node;
    const key = parent +  name;

    if (type === "directory") {        
        return {
            key,
            label: name,
            icon: "pi pi-fw pi-inbox",
            children: map(node.children, child => asTreeNode(key + "/", child))
        };
    } else {
        return {
            key,
            label: name,
            icon: "pi pi-fw pi-file",
        };
    }
};

export const fileTree = createSelector(
    configurationSelector,
    configuration => {
        const {files} = configuration;

        const fileTree= map(files, file => asTreeNode("/", file));
        return fileTree;
    }
);
