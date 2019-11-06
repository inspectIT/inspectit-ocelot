/**
 * Util functions for EditSource && primereact/tree
 */
import { find, isEqual } from 'lodash';

/**
 * rootNode for tree. All other files should be added as children
 */
export const rootNode = {
  key: '/',
  label: '/',
  icon: 'pi pi-fw pi-folder',
  children: []
}

/**
 * splits up the given node path/source to create an array of
 * all included sub paths
 * 
 * e.g. if given /folder1/folder2/file.yml
 * ['/folder1', '/folder1/folder2', '/folder1/folder2/file.yml'] will be returned
 * 
 * @param {string} nodePath - equals a source path in the form /folder/file.yml
 */
export const getParentKeys = (nodePath) => {
  if (nodePath === '/') {
    return ['/'];
  }

  let res = [nodePath];
  let parentKey = _getParentKey(nodePath);
  while (parentKey) {
    res.unshift(parentKey);
    parentKey = _getParentKey(parentKey);
  }

  return res;
}

/**
 * truncates the given node key to receive the key of the parent node
 * returns the parent key or null in case no parent exists
 * 
 * @param {string} nodeKey - the node key/ source path of the child node
 */
const _getParentKey = (key) => {
  const parentKey = key.slice(0, key.lastIndexOf('/'));
  return !isEqual(parentKey, key) ? parentKey : null
}

/**
 * returns true when the given first string/source(sub) 
 * is a subfile of the second string/source
 * 
 * @param {string} sub - string to check if it is a subfiile
 * @param {string} parent - supposed parent file of sub
 */
export const isSubfile = (sub, parent) => {
  if (sub.startsWith(parent === '/' ? parent : parent + '/')) {
    return true;
  }
  return false;
}

/**
 * Below here are function for finding and adding nodes within tree object of EditSources Component
 * 
 * a tree node always contains key/label, icon is optional and children lists nested (sub)nodes
 * the functions below work for nodes where the parent key is part of the node key (/folder/file.yml is nested within /folder)
 */

/**
 * searches for an object with the specified key within an array of objects
 * 
 * @param {array} rootNodes - array of objects which include at least key: string and children: array in case of nesting;
 * @param {string} nodeKey - the string/ node key which will be looked for
 */
export const findNode = (rootNodes, nodeKey) => {
  if (!rootNodes || !nodeKey) {
    return null;
  }

  let res = find(rootNodes, { key: nodeKey });
  if (res) {
    return res;
  }

  for (let i = 0; !res && i < rootNodes.length; i++) {
    if (nodeKey.startsWith(rootNodes[i].key)) {
      let childNodes = rootNodes[i].children;
      if (childNodes) {
        return findNode(childNodes, nodeKey);
      }
    }
  }
}

/**
 * triggers findNode with the 'parent key' of the given node key
 * (findNode will return null in case of not receiving a node key)
 *
 * @param {array} rootNodes - array of objects which include at least key: string and children: array in case of nesting;
 * @param {string} nodeKey - the string/ node key which will be looked for
 */
export const findParentNode = (rootNodes, nodeKey) => {
  return findNode(rootNodes, _getParentKey(nodeKey));
}

/**
 * this function tries to 'walk' down the treepath - based on path input, starting from root
 * and will add a new node whenever it cannot find the next node
 * 
 * @param {object} rootNode - root node, after which the new node shall be added
 * @param {*} path - the node key path from the current tree level onwards
 */
export const addNode = (rootNode, path) => {
  // splitting path, since only the next key for the current tree level is needed -> next key for the next call of addNode etc.
  let keys = path.split('/').filter(str => str !== '');
  let children = rootNode.children;

  if (_stopRecursion(keys, children)) {
    return
  }

  // the node key to find for the current tree level -> needs to be added in case it does not yet exist
  const newNodeKey = `${rootNode.key !== '/' ? rootNode.key : ''}/${keys[0]}`;
  let foundNode;

  children.forEach(node => {
    if (node.key === newNodeKey) {
      foundNode = true;
      _continueRecursion(node, keys);
    }
  })

  if (!foundNode) {
    let newNode = _createNewNode(newNodeKey, keys[0]);
    children.push(newNode);
    _continueRecursion(newNode, keys);
  }
}

/**
 * if the given node can't have children or the path is empty
 * recusion at this point can be stopped
 * 
 * @param {obj} node - tree object
 * @param {string} path - the nodekey which should be added
 */
const _stopRecursion = (keys, children) => {
  if (keys.length <= 0) {
    return true;
  }

  if (!children) { // a node without children is a file
    return true;
  }
}

/**
 * removes the first entry of keys, since the corresponding 'parentNode' has been found
 * and continues to try adding nodes
 * 
 * @param {obj} node - tree object
 * @param {array} keys - array of keys -- each entry equals a layer withing the tree
 */
const _continueRecursion = (node, keys) => {
  keys.shift();
  addNode(node, keys.join('/'));
}

/**
 * creates and returns a new node object
 * 
 * @param {string} key - key of the new node
 * @param {string} label - label of the new node
 */
const _createNewNode = (key, label) => {
  return {
    key: key,
    label: label,
    icon: 'pi pi-fw pi-question-circle',
    children: []
  }
}


