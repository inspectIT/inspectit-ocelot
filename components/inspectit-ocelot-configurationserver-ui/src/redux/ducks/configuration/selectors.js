import { createSelector } from 'reselect'
import { map, find } from 'lodash';
import { getFile, isDirectory } from './utils';

const configurationSelector = state => state.configuration;

/**
 * Recoursivly building a tree representation based on the given file objects.
 * 
 * @param {*} parentKey the key (absolute path) of the node's parent
 * @param {*} node the current node (file)
 */
const _asTreeNode = (parentKey, node, unsavedFileContents) => {
    const { type, name } = node;
    const key = parentKey + name;

    if (type === "directory") {
        const hasUnsavedChanges = Object.keys(unsavedFileContents).find((path) => path.startsWith(key + "/"));
        const labelValue = name + (hasUnsavedChanges ? " *" : "");
        return {
            key,
            label: labelValue,
            icon: "pi pi-fw pi-folder",
            children: map(node.children, child => _asTreeNode(key + "/", child, unsavedFileContents))
        };
    } else {
        const labelValue = name + ((key in unsavedFileContents) ? " *" : "");
        return {
            key,
            label: labelValue,
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
        const { files, unsavedFileContents } = configuration;

        const fileTree = map(files, file => _asTreeNode("/", file, unsavedFileContents));
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
        if (selection) {
            return getFile(files, selection);
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

/**
 * Returns contents of the selected file which have not been saved yet.
 * Null if there are no unsaved changes.
 */
export const getSelectedFileUnsavedContents = createSelector(
    configurationSelector,
    configuration => {
        const { selection, unsavedFileContents } = configuration;
        return selection in unsavedFileContents ? unsavedFileContents[selection] : null;
    }
);

/**
 * Returns true if there are any unsaved configuration changes.
 */
export const hasUnsavedChanges = createSelector(
    configurationSelector,
    configuration => {
        const { unsavedFileContents } = configuration;
        return Object.keys(unsavedFileContents).length > 0;
    }
);