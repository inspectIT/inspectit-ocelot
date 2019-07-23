import { createSelector } from 'reselect'
import { map, find } from 'lodash';
import "./queries";
import { getFile, isDirectory } from './queries';

const configurationSelector = state => state.configuration;

/**
 * Recoursivly building a tree representation based on the given file objects.
 * 
 * @param {*} parentKey the key (absolute path) of the node's parent
 * @param {*} node the current node (file)
 */
const _asTreeNode = (parentKey, node) => {
    const { type, name } = node;
    const key = parentKey + name;

    if (type === "directory") {
        return {
            key,
            label: name,
            icon: "pi pi-fw pi-folder",
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
        if(selection) {
            return getFile(files,selection);
        } else {
            return null;
        }
    }
);

/**
 * Returns true if the selected file is a directory. The function returns false in case a file is selected or no selection exists.
 */
export const isSelectionDirectory = createSelector(
    [getSelectedFile],
    (selectedFile) => isDirectory(selectedFile)
);