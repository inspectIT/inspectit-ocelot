import React from 'react';
import { Tree } from 'primereact/tree';

import { isEqual, cloneDeep } from 'lodash';
import * as treeUtils from './treeUtils';

/**
 * Tree for EditSources Component
 * Config Files are shown as tree and can be selected
 */
class SourceTree extends React.Component {
  state = {
    selectedSources: {},
  }

  render() {
    return (
      <Tree
        value={this.props.tree}
        selectionMode='checkbox'
        selectionKeys={this.state.selectedSources}
        onSelectionChange={e => this.handleSelectionChange(e.value)}
        expandedKeys={this.props.expandedKeys}
      />
    )
  }

  componentDidUpdate = () => {
    // new tree needed whenever an unknown source is added by user
    const newTree = getUpdatedTree(this.props.sourcePaths, this.props.tree);
    if (!isEqual(newTree, this.props.tree)) {
      this.props.onTreeChange(newTree);
    }

    // new selection needed when user adds sources without clicking within tree checkboxes
    const newSelection = getUpdatedTreeSelection(this.props.sourcePaths, this.props.tree);
    if (!isEqual(newSelection, this.state.selectedSources)) {
      this.setState({ selectedSources: newSelection });
    }
  }

	/**
	 * callback for onSelectionChange of Primereact/Tree component
	 * will filter all completely checked nodes for the callback
	 * ~ completely checked is a node, if the node and/or all his childs are checked
	 */
  handleSelectionChange = (newSelection) => {
    let selectedSources = [];

    for (let source in newSelection) {
      if (newSelection[source].checked === true && newSelection[source].partialChecked === false) {
        selectedSources.push(source);
      }
    }

    this.props.onSelectionChange(selectedSources);
    this.setState({ selectedSources: newSelection });
  }
}

export default SourceTree

/**
 * for every path (source), which is not yet in the tree object,
 * a node for the path will be added to the result tree
 * 
 * @param {array of strings} sources 
 * @param {array of tree nodes} oldTree 
 */
const getUpdatedTree = (sources = [], oldTree = [rootNode]) => {
  let res = cloneDeep(oldTree);

  sources.forEach(path => {
    if (!treeUtils.findNode(res, path)) {
      /** new nodes will at least be added after root node */
      treeUtils.addNode(res[0], path);
    }
  })
  return res;
}

/**
 * creates and returns a selection object used by primereact/tree.
 * primereact/tree creates the selection object when selection changes through clicking - 
 * since we can change the selection through input as well - we need to create our own selection object
 * 
 * makes use of different functions to correctly check all sources 
 * + the children in case a source is a folder
 * + rechecks the parent node in case it turns out that all it's child nodes are checked.
 * 
 * primereact/tree handles selection by itself, as long as selection changes through clicks.
 * When changing the selection manually the selection object has to be created manually.
 * 
 * @param {array} sources - array of string/ source paths
 * @param {array} tree - array of tree nodes used in primereact/tree
 */
const getUpdatedTreeSelection = (sources = [], tree = [rootNode]) => {
  let res = {};
  sources.forEach(path => {
    // add a checkObj for each path/source
    res = Object.assign(res, _getCheckedPathObj(path));

    // it it's a folder, add a checkObj for all children as well
    const node = treeUtils.findNode(tree, path);
    if (node && node.children) {
      _checkPathOfChildren(res, node.children);
    }
  })
  /**
   * after all sources have been added, 
   * try to find parent nodes where all children have been checked and 
   * change the corresponding checkObj for this node (from unchecked to checked)
   */
  for (let nodePath in res) {
    if (res[nodePath].checked) {
      const parentNode = treeUtils.findParentNode(tree, nodePath);
      if (parentNode && parentNode.children) {
        _checkPathOfParent(res, parentNode, tree);
      }
    }
  }
  return res;
}

/** below here are (helper) functions for getUpdatedTreeSelection */

/**
 * takes a single string/path and returns a basic selection object (for this path).
 * The selection object is used by primereact tree component
 * 
 * @param {string} nodePath - equals a source path in the form /folder/file.yml
 */
const _getCheckedPathObj = (nodePath) => {
  let res = {};
  const paths = treeUtils.getParentKeys(nodePath);
  // parent-folders are considered to have multiple children, hence they can't be full checked now
  paths.forEach((path, idx) => {
    if (!paths[idx + 1]) {
      res[[path]] = { checked: true, partialChecked: false };
    } else {
      res[[path]] = { checked: false, partialChecked: true };
    }
  })
  return res;
}

/**
 * takes an object which shall be modified and an array of nodes,
 * which will be used for this
 * 
 * similar to _getCheckedPathObj as it modifies the given selection
 * for use by primereact tree component
 * 
 * @param {array} nodeArray - an array of node children
 * @param {object} selection - the result obj which will be modified
 */
const _checkPathOfChildren = (selection, nodeArray = []) => {
  nodeArray.forEach(node => {
    selection[[node.key]] = { checked: true, partialChecked: false };

    if (node.children) {
      _checkPathOfChildren(selection, node.children);
    }
  })
}


/**
 * checks whether or not all children of the given node
 * are full checked inside the selection object.
 * If yes: updates the selection to full check the node and redo for
 * the next parent node
 * 
 * @param {object} selection - the result obj which will be modified
 * @param {object} node - a tree node object
 * @param {array} tree - an array of tree node objects 
 */
const _checkPathOfParent = (selection, node, tree) => {
  if (!node.children) {
    return;
  }
  let allChildsChecked = true;
  node.children.forEach(child => {
    if (!selection[child.key] || !selection[child.key].checked) {
      allChildsChecked = false;
    }
  })

  if (allChildsChecked) {
    selection[node.key] = { checked: true, partialChecked: false };

    const parent = treeUtils.findParentNode(tree, node.key);

    if (parent && parent.children) {
      _checkPathOfParent(selection, parent, tree);
    }
  }

}