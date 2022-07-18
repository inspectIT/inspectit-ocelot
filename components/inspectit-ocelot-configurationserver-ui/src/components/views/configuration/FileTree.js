import React from 'react';
import { Tree } from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration';
import { linkPrefix } from '../../../lib/configuration';
import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';
import { filter } from 'lodash';

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {
  state = {
    contextMenuModel: [],
  };

  searchedFileTest = '';
  contextMenuRef = React.createRef();

  /**
   * Fetch the files initially.
   */
  componentDidMount = () => {
    const { defaultConfig } = this.props;
    this.props.fetchFiles();

    if (Object.entries(defaultConfig).length === 0) {
      this.props.fetchDefaultConfig();
    }
  };

  /**
   * Invoked immediately after updating occurs.
   * This method is not called for the initial render.
   */
  componentDidUpdate(prevProps) {
    if (prevProps.targetFile !== '' && prevProps.targetFile !== this.searchedFileTest) {
      // Overwrite searched file from props
      this.searchedFileTest = prevProps.targetFile.toString();
      const targetNodeKey = '/' + this.searchedFileTest.substring(0, this.searchedFileTest.indexOf('/'));
      const iterations = this.searchedFileTest.split('/').length - 1;
      this.expandLabelThroughDOMElements(targetNodeKey, iterations, 1);
    }
  }

  /**
   * Extends closed nodes when selecting target key.
   * @param targetNodeKey the node path given via props from ConfigurationView.js
   * @param iterations signals if and how many recursive calls should occur
   * @param split helps extracting the node key for recursive calls
   */
  expandLabelThroughDOMElements(targetNodeKey, iterations, split) {
    let togglers = Array.from(document.getElementsByClassName('p-tree-toggler p-unselectable-text p-link'));
    for (const toggler of togglers) {
      if (toggler && toggler.innerHTML !== undefined) {
        let reactHandlerKey = Object.keys(toggler).filter((item) => {
          return item.indexOf('__reactEventHandlers') >= 0;
        });
        let reactHandler = toggler[reactHandlerKey[0]];
        if (targetNodeKey === reactHandler.children._owner.key && !this.noteExtended(targetNodeKey)) {
          toggler.click();
        }
      }
    }
    // Adding small timeout to make DOM register change in order to call of method recursively
    setTimeout(() => {
      if (iterations > 1) {
        const paths = this.searchedFileTest.split('/');
        let newString = '/' + paths[0];
        for (let i = 1; i <= split; i++) {
          newString = newString + '/' + paths[i];
        }
        iterations--;
        split++;
        this.expandLabelThroughDOMElements(newString, iterations, split);
      }
    }, '1');
  }

  /**
   * Checks if note is already open.
   * @param key the key of the node that needs to be extended
   */
  noteExtended(key) {
    let treeNodes = document.getElementsByClassName('p-treenode-content p-treenode-selectable');
    for (const node of treeNodes) {
      let reactHandlerKey = Object.keys(node).filter((item) => {
        return item.indexOf('__reactEventHandlers') >= 0;
      });
      let reactHandler = node[reactHandlerKey[0]];
      if (key === reactHandler.children[0]._owner.key) {
        if (reactHandler['aria-expanded']) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Handle tree selection changes.
   */
  onSelectionChange = (event) => {
    const { selection, selectedDefaultConfigFile } = this.props;
    const newSelection = event.value;
    if (newSelection) {
      if (newSelection !== selection && newSelection !== selectedDefaultConfigFile) {
        this.props.selectFile(newSelection);
      }
    } else {
      if (selection || selectedDefaultConfigFile) {
        this.props.selectFile(null);
      }
    }
  };

  /**
   * Handle ContextMenu selection.
   * Switch between a contextmenu for filenodes and a general menu.
   */
  showContextMenu = (event) => {
    const newSelection = event.value || '';

    if (newSelection && newSelection.startsWith(DEFAULT_CONFIG_TREE_KEY)) {
      // Show no contextmenu when clicked on an ocelot default configuration node.
      event.originalEvent.stopPropagation();
      return;
    }

    this.setState({ contextMenuModel: this.getContextMenuModel(newSelection) });
    this.contextMenuRef.current.show(event.originalEvent || event);
  };

  /**
   * Handle drag and drop movement.
   */
  onDragDrop = (event) => {
    const newTree = event.value.filter((node) => node.key !== DEFAULT_CONFIG_TREE_KEY);
    const paths = this.comparePaths('', newTree);

    if (paths) {
      const { source, target } = paths;
      this.props.move(source, target, true);
    }
  };

  /**
   * Handle delete key press
   */
  onKeyDown = (event) => {
    if (event.key === 'Delete' && this.props.selection) {
      this.props.showDeleteFileDialog(this.props.selection);
    }
  };

  /**
   * Attempt to find a file in the 'wrong' place by comparing a node's key with it's expected key.
   * Returns the old (source) and expected (target) key when a node is found.
   */
  comparePaths = (parentKey, nodes) => {
    const getFileName = (fileNode) => fileNode.key.substring(fileNode.key.lastIndexOf('/') + 1);
    let foundFile = filter(nodes, (file) => file.key !== `${parentKey}/${getFileName(file)}`);
    if (foundFile.length === 1) {
      return {
        source: foundFile[0].key,
        target: `${parentKey}/${getFileName(foundFile[0])}`,
      };
    }

    for (const child of nodes) {
      if (child.children) {
        const res = this.comparePaths(child.key, child.children);
        if (res) {
          return res;
        }
      }
    }

    return null;
  };

  render() {
    const { className, defaultTree, selection, selectedDefaultConfigFile, readOnly, files, selectedVersion } = this.props;

    return (
      <div className="this" onContextMenu={readOnly ? undefined : this.showContextMenu} onKeyDown={readOnly ? undefined : this.onKeyDown}>
        <style jsx>{`
                  .this {
                    overflow: auto;
                    flex-grow: 1;
                    display: flex;
                    flex-direction: column;
                  }

                  .this :global(.cm-tree-icon) {
                    width: 1.3rem;
                    height: 1.3rem;
                  }

                  .this :global(.cm-tree-label) {
                    color: #aaa;
                  }

                  .this :global(.ocelot-tree-head-orange) {
                    background: url("${linkPrefix}/static/images/inspectit-ocelot-head_orange.svg") center no-repeat;
                    background-size: 1rem 1rem;
                  }

                  .this :global(.ocelot-tree-head-white) {
                    background: url("${linkPrefix}/static/images/inspectit-ocelot-head_white.svg") center no-repeat;
                    background-size: 1rem 1rem;
                  }

                  .tree-container {
                    overflow: auto;
                  }

                  .version-banner {
                    background-color: #ffcc80;
                    height: 2.45rem;
                    border-bottom: 1px solid #dddddd;
                  }
                `}</style>
        {selectedVersion && <div className="version-banner" />}
        <div className="tree-container">
          <ContextMenu model={this.state.contextMenuModel} ref={this.contextMenuRef} />
          <Tree
            id={'fileTree'}
            className={className}
            filter={true}
            filterBy="label"
            value={defaultTree.concat(files)}
            selectionMode="single"
            selectionKeys={selection || selectedDefaultConfigFile}
            onSelectionChange={this.onSelectionChange}
            onContextMenuSelectionChange={readOnly ? undefined : this.showContextMenu}
            dragdropScope={readOnly ? undefined : 'config-file-tree'}
            onDragDrop={readOnly ? undefined : this.onDragDrop}
          />
        </div>
      </div>
    );
  }

  getContextMenuModel = (filePath) => {
    const { showCreateDirectoryDialog, showCreateFileDialog, showMoveDialog, showDeleteFileDialog, exportSelection } = this.props;

    return [
      {
        label: 'Add Folder',
        icon: 'pi pi-folder',
        command: () => showCreateDirectoryDialog(filePath),
      },
      {
        label: 'Add File',
        icon: 'pi pi-file',
        command: () => showCreateFileDialog(filePath),
      },
      {
        label: 'Download',
        icon: 'pi pi-download',
        command: () => exportSelection(true, filePath),
      },
      {
        label: 'Rename',
        icon: 'pi pi-pencil',
        disabled: !filePath,
        command: () => showMoveDialog(filePath),
      },
      {
        label: 'Delete',
        icon: 'pi pi-trash',
        disabled: !filePath,
        command: () => showDeleteFileDialog(filePath),
      },
    ];
  };
}

function mapStateToProps(state) {
  const { pendingRequests, selection, defaultConfig, selectedDefaultConfigFile, selectedVersion } = state.configuration;
  return {
    files: configurationSelectors.getFileTree(state),
    loading: pendingRequests > 0,
    selection,
    defaultConfig: defaultConfig,
    defaultTree: configurationSelectors.getDefaultConfigTree(state),
    selectedDefaultConfigFile,
    selectedVersion,
  };
}

const mapDispatchToProps = {
  fetchDefaultConfig: configurationActions.fetchDefaultConfig,
  fetchFiles: configurationActions.fetchFiles,
  selectFile: configurationActions.selectFile,
  exportSelection: configurationActions.exportSelection,
  move: configurationActions.move,
};

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);
