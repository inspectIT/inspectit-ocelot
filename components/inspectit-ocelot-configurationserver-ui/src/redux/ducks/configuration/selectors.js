import { createSelector } from 'reselect'
import { map, find } from 'lodash';

const configurationSelector = state => state.configuration;

/**
 * Recoursivly building a tree representation based on the given file objects.
 * @param {*} parent 
 * @param {*} node 
 */
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

/**
 * Returns the loaded configuration files and directories in a tree structure used by the FileTree component.
 */
export const getFileTree = createSelector(
    configurationSelector,
    configuration => {
        const { files } = configuration;

        const fileTree = map(files, file => _asTreeNode("/", file));
        return fileTree;
    }
);

/**
 * Returns the selected file object.
 */
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

/**
 * Returns true if the selected file is a directory. The function returns false in case a file is selected or no selection exists.
 */
export const isSelectionDirectory = createSelector(
    [getSelectedFile],
    (selectedFile) => {
        if (selectedFile) {
            return selectedFile.type === "directory";
        } else {
            return false;
        }
    }
);