import React from 'react';
import { Tree } from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration';
import { linkPrefix } from '../../../lib/configuration';
import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';
import { filter } from 'lodash';
import PropTypes from 'prop-types';

/**
 * The file tree used in the configuration view.
 */
class FileTree extends React.Component {
  state = {
    contextMenuModel: [],
    expandedKeys: {},
  };
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
    const { selection, selectedDefaultConfigFile } = this.props;
    // check if a new file has been selected
    if (
      (selection || selectedDefaultConfigFile) &&
      (selectedDefaultConfigFile !== prevProps.selectedDefaultConfigFile || selection !== prevProps.selection)
    ) {
      // if true, expand needed nodes in FileTree, in case the file was opened using search
      let filePath = '';
      if (selection) {
        filePath = selection;
      } else {
        filePath = selectedDefaultConfigFile.replace(DEFAULT_CONFIG_TREE_KEY, '/Ocelot Defaults');
      }
      const splitFilePath = filePath.split('/');
      let currentNode = '';
      let expandedKeys = { ...this.state.expandedKeys };
      for (let i = 1; i < splitFilePath.length; i++) {
        currentNode += '/' + splitFilePath[i];
        expandedKeys[currentNode] = true;
      }
      this.setState({ expandedKeys: expandedKeys });
    }
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
    if (event.originalEvent) {
      event.originalEvent.persist();
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

          .this :global(.cm-hidden-file-tree-label) {
            color: #aaa;
          }

          .this :global(.ocelot-tree-head-orange) {
            background: url('${linkPrefix}/static/images/inspectit-ocelot-head_orange.svg') center no-repeat;
            background-size: 1rem 1rem;
          }

          .this :global(.ocelot-tree-head-white) {
            background: url('${linkPrefix}/static/images/inspectit-ocelot-head_white.svg') center no-repeat;
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
            expandedKeys={this.state.expandedKeys}
            onToggle={(e) => this.setState({ expandedKeys: e.value })}
          />
        </div>
      </div>
    );
  }

  getContextMenuModel = (filePath) => {
    const {
      showCreateDirectoryDialog,
      showCreateFileDialog,
      showMoveDialog,
      showDeleteFileDialog,
      exportSelection,
      toggleShowHiddenFiles,
      showHiddenFiles,
    } = this.props;

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
        label: showHiddenFiles ? 'Hide Files' : 'Show Hidden Files',
        icon: showHiddenFiles ? 'pi pi-eye-slash' : 'pi pi-eye',
        command: () => toggleShowHiddenFiles(),
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
  const { pendingRequests, selection, defaultConfig, selectedDefaultConfigFile, selectedVersion, showHiddenFiles } = state.configuration;
  return {
    files: configurationSelectors.getFileTree(state),
    loading: pendingRequests > 0,
    selection,
    showHiddenFiles,
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
  toggleShowHiddenFiles: configurationActions.toggleShowHiddenFiles,
  move: configurationActions.move,
};

FileTree.propTypes = {
  className: PropTypes.string,
  /**  the default configuration key/value ~ path/content pairs in a tree structure. */
  defaultTree: PropTypes.array,
  /** The loaded configuration files and directories in a tree structure. */
  files: PropTypes.array,
  /** The path of the currently selected file if it is not from the default config. */
  selection: PropTypes.string,
  /** The default configuration of the Ocelot agents. Will be retrieved as key/value pairs each representing path/content of a file. */
  defaultConfig: PropTypes.object,
  /** The path of the currently selected file if it is from the default config. */
  selectedDefaultConfigFile: PropTypes.string,
  /** Whether the current selection in the FileTree is readOnly. */
  readOnly: PropTypes.bool,
  /** The selected version of configuration files. */
  selectedVersion: PropTypes.string,
  /** Callback which triggers showing the deleteFileDialog. */
  showDeleteFileDialog: PropTypes.func,
  /** Callback which triggers showing the showCreateDirectoryDialog. */
  showCreateDirectoryDialog: PropTypes.func,
  /** Callback which triggers showing the showCreateFileDialog. */
  showCreateFileDialog: PropTypes.func,
  /** Callback which triggers showing the showMoveDialog. */
  showMoveDialog: PropTypes.func,
  /** Redux dispatch action for exporting and downloading a file. */
  exportSelection: PropTypes.func,
  /** Redux dispatch action for fetching all configuration files and directories. */
  fetchFiles: PropTypes.func,
  /** Redux dispatch action for fetching the default config. */
  fetchDefaultConfig: PropTypes.func,
  /** Redux dispatch action for propagating selection changes in the file tree. */
  selectFile: PropTypes.func,
  /** Redux dispatch action for moving a file. */
  move: PropTypes.func,
};

export default connect(mapStateToProps, mapDispatchToProps)(FileTree);
