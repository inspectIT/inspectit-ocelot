import { createSelector } from 'reselect';
import { map, find } from 'lodash';
import { getFile, isDirectory } from './utils';
import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';

const configurationSelector = (state) => state.configuration;

/**
 * Recoursivly building a tree representation based on the given file objects.
 *
 * @param {*} parentKey the key (absolute path) of the node's parent
 * @param {*} node the current node (file)
 */
const _asTreeNode = (parentKey, node, unsavedFileContents, selectedVersion) => {
  const { type, name } = node;
  const key = parentKey + name;

  if (type === 'directory') {
    const hasUnsavedChanges = Object.keys(unsavedFileContents).find((path) => path.startsWith(key + '/'));
    const labelValue = name + (hasUnsavedChanges && selectedVersion === 0 ? ' *' : '');
    return {
      key,
      label: labelValue,
      icon: 'pi pi-fw pi-folder',
      children: map(node.children, (child) => _asTreeNode(key + '/', child, unsavedFileContents, selectedVersion)),
    };
  } else {
    const labelValue = name + ((key in unsavedFileContents) && selectedVersion === 0 ? ' *' : '');
    return {
      key,
      label: labelValue,
      icon: 'pi pi-fw pi-file',
    };
  }
};

/**
 * Returns the loaded configuration files and directories in a tree structure used by the FileTree component.
 */
export const getFileTree = createSelector(configurationSelector, (configuration) => {
  const { files, unsavedFileContents, selectedVersion } = configuration;

  const fileTree = map(files, (file) => _asTreeNode('/', file, unsavedFileContents, selectedVersion));
  return fileTree;
});

/**
 * Returns the selected file object.
 */
export const getSelectedFile = createSelector(configurationSelector, (configuration) => {
  const { selection, files } = configuration;
  if (selection) {
    return getFile(files, selection);
  } else {
    return null;
  }
});

/**
 * Returns true if the selected file is a directory. The function returns false in case a file is selected or no selection exists.
 */
export const isSelectionDirectory = createSelector([getSelectedFile], (selectedFile) => isDirectory(selectedFile));

/**
 * Returns contents of the selected file which have not been saved yet.
 * Null if there are no unsaved changes.
 */
export const getSelectedFileUnsavedContents = createSelector(configurationSelector, (configuration) => {
  const { selection, unsavedFileContents } = configuration;
  return selection in unsavedFileContents ? unsavedFileContents[selection] : null;
});

/**
 * Returns true if there are any unsaved configuration changes.
 */
export const hasUnsavedChanges = createSelector(configurationSelector, (configuration) => {
  const { unsavedFileContents } = configuration;
  return Object.keys(unsavedFileContents).length > 0;
});

/**
 * Compares two path strings for sorting.
 * Sorting ensures that directories appear before files and that they are sorted lexicographically.
 */
const comparePaths = (firstPath, secondPath) => {
  const firstNames = firstPath.split('/');
  const secondNames = secondPath.split('/');
  let depth = 0;
  while (depth < firstNames.length && depth < secondNames.length) {
    let firstIsFile = firstNames.length === depth + 1;
    let secondIsFile = secondNames.length === depth + 1;
    if (firstIsFile && !secondIsFile) {
      return 1;
    }
    if (!firstIsFile && secondIsFile) {
      return -1;
    }
    const nameComparison = firstNames[depth].localeCompare(secondNames[depth]);
    if (nameComparison !== 0) {
      return nameComparison;
    }
    depth++;
  }
  return 0; //both are equal
};

/**
 * Returns the default configuration key/value ~ path/content pairs in a tree structure used by the FileTree component.
 */
export const getDefaultConfigTree = createSelector(configurationSelector, (configuration) => {
  const { defaultConfig, selectedDefaultConfigFile } = configuration;

  const paths = Object.keys(defaultConfig);
  const res = [];

  if (paths.length !== 0) {
    res.push(_getDefaultRoot(selectedDefaultConfigFile));

    paths.sort(comparePaths);
    for (const path of paths) {
      _addNode(res[0], path);
    }
  }

  return res;
});

/**
 * Takes the given path to create a new node for every unknown subpath starting from the given root.
 *
 * @param {object} rootNode - New Nodes will be added to the children property of this node.
 * @param {*} path - The File path to be added.
 */
const _addNode = (rootNode, path) => {
  const names = path.split('/').filter((str) => str !== '');

  let parent = rootNode;

  for (let i = 0; i < names.length; i++) {
    const childKey = `${parent.key}/${names[i]}`;
    const matchingChild = find(parent.children, { key: childKey });

    if (matchingChild) {
      parent = matchingChild;
    } else {
      const newChild = {
        key: childKey,
        label: names[i],
        icon: `pi pi-fw ${names[i + 1] ? 'pi-folder' : 'pi-file'}`,
        children: [],
        draggable: false,
      };

      parent.children.push(newChild);
      parent = newChild;
    }
  }
};

/**
 * Returns a root tree object for the default configuration tree.
 *
 * @param {String} selection the current user selection
 */
const _getDefaultRoot = (selection) => {
  return {
    key: DEFAULT_CONFIG_TREE_KEY,
    label: 'Ocelot Defaults',
    icon: `cm-tree-icon ocelot-tree-head-${selection === DEFAULT_CONFIG_TREE_KEY ? 'white' : 'orange'}`,
    children: [],
    className: 'cm-tree-label',
    draggable: false,
  };
};
