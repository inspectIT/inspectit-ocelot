import { createSelector } from 'reselect'
import { map, find } from 'lodash';

const configurationSelector = state => state.configuration;

const _asTreeNode = (parent, node) => {
    const { type, name } = node;
    const key = parent + name;

    if (type === "directory") {
        return {
            key,
            label: name,
            icon: "pi pi-fw pi-inbox",
            children: map(node.children, child => _asTreeNode(key + "/", child))
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
        const { files } = configuration;

        const fileTree = map(files, file => _asTreeNode("/", file));
        return fileTree;
    }
);

export const getSelectedFile = createSelector(
    configurationSelector,
    configuration => {
        const { selection, files } = configuration;

        if (!selection) {
            return null;
        }

        const names = selection.split("/");

        let current = files;
        for (let idx in names) {
            const name = names[idx];
            const isLast = idx == names.length - 1;

            if (name === "") {
                continue;
            }

            current = find(current, { name });

            if (!isLast) {
                current = current.children;
            }
        }

        return current;
    }
);

export const isSelectionDirectory = createSelector(
    [getSelectedFile, configurationSelector],
    (selectedFile, configuration) => {
        if (selectedFile) {
            return selectedFile.type === "directory";
        } else {
            return false;
        }
    }
);