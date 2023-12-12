import yaml from 'js-yaml';
import React from 'react';
import { connect } from 'react-redux';
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration';
import { notificationActions } from '../../../redux/ducks/notification';
import EditorView from '../../editor/EditorView';
import CreateDialog from './dialogs/CreateDialog';
import DeleteDialog from './dialogs/DeleteDialog';
import UploadDialog from './dialogs/UploadDialog';
import MoveDialog from './dialogs/MoveDialog';
import FileToolbar from './FileToolbar';
import FileTree from './FileTree';
import { enableOcelotAutocompletion } from './OcelotAutocompleter';
import SearchDialog from './dialogs/SearchDialog';
import ConvertDialog from '../../common/dialogs/ConvertDialog';
import ConfigurationSidebar from './ConfigurationSidebar';
import DownloadDialogue from '../dialogs/DownloadDialogue';

/** Data */
import { CONFIGURATION_TYPES, DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';

/**
 * The header component of the editor view.
 */
const EditorHeader = ({ icon, path, name, isContentModified, readOnly }) => (
  <>
    <style jsx>{`
      .header {
        font-size: 1rem;
        display: flex;
        align-items: center;
        height: 2rem;
      }
      .header :global(.pi) {
        font-size: 1.25rem;
        color: #aaa;
        margin-right: 1rem;
      }
      .path {
        color: #999;
      }
      .dirtyStateMarker {
        margin-left: 0.25rem;
        color: #999;
      }
    `}</style>
    <div className="header">
      <i className={'pi ' + icon}></i>
      <div className="path">{path}</div>
      <div className="name">{name}</div>
      {isContentModified && <div className="dirtyStateMarker">*</div>}
      {readOnly && <div className="dirtyStateMarker">(read only)</div>}
    </div>
  </>
);

/**
 * The configuration view component used for managing the agent configurations.
 */
class ConfigurationView extends React.Component {
  state = {
    isDeleteFileDialogShown: false,
    isUploadDialogShown: false,
    isCreateDialogShown: false,
    isDirectoryMode: false,
    configurationType: CONFIGURATION_TYPES.YAML,
    isMoveDialogShown: false,
    filePath: null,
    isSearchDialogShown: false,
    isConfigurationDialogShown: false,
    isConvertDialogShown: false,
  };

  parsePath = (filePath, defaultConfigFilePath) => {
    if (filePath) {
      return this.splitIntoPathAndName(filePath);
    } else if (defaultConfigFilePath) {
      const path = defaultConfigFilePath.replace(DEFAULT_CONFIG_TREE_KEY, '/Ocelot Defaults');
      return this.splitIntoPathAndName(path);
    } else {
      return {};
    }
  };

  splitIntoPathAndName = (path) => {
    const lastIndex = path.lastIndexOf('/') + 1;
    return {
      path: path.slice(0, lastIndex),
      name: path.slice(lastIndex),
    };
  };

  onSave = () => {
    const { selection, fileContent, writeFile } = this.props;
    writeFile(selection, fileContent, false);
  };

  convertEditor = () => {
    const { selection, fileContent, writeFile } = this.props;

    try {
      // load-and-dump is done to remove comments etc.
      const configuration = yaml.load(fileContent);
      const updatedYamlConfiguration = yaml.dump(configuration);
      writeFile(selection, updatedYamlConfiguration, false);
    } catch (error) {
      console.error('Configuration could not been updated.', error);
    }
  };

  onChange = (value) => {
    if (!this.props.loading && this.props.isLatestVersion) {
      this.props.selectedFileContentsChanged(value);
    }
  };

  onRefresh = () => {
    this.props.selectedFileContentsChanged(null);
  };

  showDeleteFileDialog = (filePath) => this.setState({ isDeleteFileDialogShown: true, filePath });

  hideDeleteFileDialog = () => this.setState({ isDeleteFileDialogShown: false, filePath: null });

  showUploadDialog = () => this.setState({ isUploadDialogShown: true });

  hideUploadDialog = () => this.setState({ isUploadDialogShown: false });

  showCreateFileDialog = (filePath, configurationType) =>
    this.setState({
      isCreateDialogShown: true,
      isDirectoryMode: false,
      configurationType: configurationType,
      filePath,
    });

  hideCreateDialog = () => this.setState({ isCreateDialogShown: false, isDirectoryMode: false, filePath: null });

  showCreateDirectoryDialog = (filePath) =>
    this.setState({
      isCreateDialogShown: true,
      isDirectoryMode: true,
      configurationType: CONFIGURATION_TYPES.YAML,
      filePath,
    });

  showMoveDialog = (filePath) => this.setState({ isMoveDialogShown: true, filePath });

  hideMoveDialog = () => this.setState({ isMoveDialogShown: false, filePath: null });

  showSearchDialog = () => this.setState({ isSearchDialogShown: true });

  hideSearchDialog = () => this.setState({ isSearchDialogShown: false });

  showConfigurationDialog = () => this.setState({ isConfigurationDialogShown: true });

  hideConfigurationDialog = () => this.setState({ isConfigurationDialogShown: false });

  showConvertDialog = () => this.setState({ isConvertDialogShown: true });

  hideConvertDialog = () => this.setState({ isConvertDialogShown: false });

  /**
   * Opens the specified file in the specified version. The version is only changed if it differs from the current one.
   * If no version is specified, the latest version will be selected.
   */
  openFile = (filename, versionId = null) => {
    if (this.props.selectedVersion !== versionId) {
      this.props.selectVersion(versionId, false);
    }
    this.props.selectFile(filename);
  };

  toggleShowHiddenFiles = () => {
    this.props.toggleShowHiddenFiles();
  };

  render() {
    const {
      selection,
      isDirectory,
      loading,
      isContentModified,
      fileContent,
      yamlError,
      selectedDefaultConfigFile,
      schema,
      showVisualConfigurationView,
      toggleVisualConfigurationView,
      isLatestVersion,
      canWrite,
    } = this.props;
    const showEditor = (selection || selectedDefaultConfigFile) && !isDirectory;
    const { path, name } = this.parsePath(selection, selectedDefaultConfigFile);
    const icon = 'pi-' + (isDirectory ? 'folder' : 'file');
    const showHeader = !!name;

    const readOnly = !canWrite || !!selectedDefaultConfigFile || !isLatestVersion;

    const fileContentWithoutFirstLine = fileContent ? fileContent.split('\n').slice(1).join('\n') : '';
    return (
      <div className="this">
        <style jsx>{`
          .this {
            height: 100%;
            display: flex;
          }
          .this :global(.p-tree) {
            height: 100%;
            border: 0;
            border-radius: 0;
            display: flex;
            flex-direction: column;
            background: 0;
          }
          .treeContainer {
            height: 100%;
            display: flex;
            flex-direction: column;
            border-right: 1px solid #ddd;
          }
          .details {
            color: #ccc;
            font-size: 0.75rem;
            text-align: center;
            padding: 0.25rem 0;
          }
        `}</style>
        <div className="treeContainer">
          <FileToolbar
            readOnly={readOnly}
            showDeleteFileDialog={this.showDeleteFileDialog}
            showUploadDialog={this.showUploadDialog}
            showCreateFileDialog={this.showCreateFileDialog}
            showCreateDirectoryDialog={this.showCreateDirectoryDialog}
            showMoveDialog={this.showMoveDialog}
            selectedVersionChange={this.selectedVersionChange}
            showSearchDialog={this.showSearchDialog}
            toggleShowHiddenFiles={this.toggleShowHiddenFiles}
          />
          <FileTree
            className="fileTree"
            showDeleteFileDialog={this.showDeleteFileDialog}
            showUploadDialog={this.showUploadDialog}
            showCreateFileDialog={this.showCreateFileDialog}
            showCreateDirectoryDialog={this.showCreateDirectoryDialog}
            showMoveDialog={this.showMoveDialog}
            readOnly={readOnly}
          />
          <div className="details">Last refresh: {this.props.updateDate ? new Date(this.props.updateDate).toLocaleString() : '-'}</div>
        </div>
        <EditorView
          showEditor={showEditor}
          value={fileContent}
          schema={schema}
          hint={'Select a file to start editing.'}
          onSave={this.onSave}
          showConfigurationDialog={this.showConfigurationDialog}
          showConvertWarning={this.showConvertDialog}
          enableButtons={showEditor && !loading}
          onCreate={enableOcelotAutocompletion}
          onChange={this.onChange}
          onRefresh={this.onRefresh}
          canSave={isContentModified && !yamlError}
          isErrorNotification={true}
          notificationIcon="pi-exclamation-triangle"
          notificationText={yamlError}
          loading={loading}
          readOnly={readOnly}
          showVisualConfigurationView={showVisualConfigurationView}
          onToggleVisualConfigurationView={toggleVisualConfigurationView}
          sidebar={<ConfigurationSidebar />}
        >
          {showHeader ? (
            <EditorHeader icon={icon} path={path} name={name} isContentModified={isContentModified} readOnly={readOnly} />
          ) : null}
        </EditorView>

        <DeleteDialog visible={this.state.isDeleteFileDialogShown} onHide={this.hideDeleteFileDialog} filePath={this.state.filePath} />
        <UploadDialog visible={this.state.isUploadDialogShown} onHide={this.hideUploadDialog} />
        <CreateDialog
          directoryMode={this.state.isDirectoryMode}
          visible={this.state.isCreateDialogShown}
          onHide={this.hideCreateDialog}
          filePath={this.state.filePath}
          configurationType={this.state.configurationType}
        />
        <MoveDialog visible={this.state.isMoveDialogShown} onHide={this.hideMoveDialog} filePath={this.state.filePath} />

        <SearchDialog visible={this.state.isSearchDialogShown} onHide={this.hideSearchDialog} openFile={this.openFile} />

        <DownloadDialogue
          visible={this.state.isConfigurationDialogShown}
          onHide={this.hideConfigurationDialog}
          loading={this.props.loading}
          contentValue={fileContentWithoutFirstLine}
          contentType={'config'}
          contextName={`${path}${name}`}
        />

        <ConvertDialog
          visible={this.state.isConvertDialogShown}
          onHide={this.hideConvertDialog}
          name={`${path}${name}`}
          text="Warning"
          onSuccess={this.convertEditor}
        />
      </div>
    );
  }
}

const getYamlError = (content) => {
  try {
    yaml.load(content);
    return null;
  } catch (error) {
    if (error.message) {
      return 'YAML Syntax Error: ' + error.message;
    } else {
      return 'YAML cannot be parsed.';
    }
  }
};

function mapStateToProps(state) {
  const {
    updateDate,
    selection,
    selectedFileContent,
    pendingRequests,
    selectedDefaultConfigFile,
    schema,
    showVisualConfigurationView,
    selectedVersion,
    versions,
  } = state.configuration;
  const isLatestVersion = selectedVersion === null || selectedVersion === versions[0].id;
  const unsavedFileContent = selection ? configurationSelectors.getSelectedFileUnsavedContents(state) : null;
  const isContentModified = unsavedFileContent !== null && isLatestVersion;
  const fileContent = isContentModified ? unsavedFileContent : selectedFileContent;

  return {
    updateDate,
    selection,
    isDirectory: configurationSelectors.isSelectionDirectory(state) || !!(selectedDefaultConfigFile && !fileContent),
    isContentModified,
    fileContent,
    yamlError: getYamlError(fileContent),
    loading: pendingRequests > 0,
    selectedDefaultConfigFile,
    schema,
    showVisualConfigurationView,
    canWrite: state.authentication.permissions.write,
    isLatestVersion,
    selectedVersion,
  };
}

const mapDispatchToProps = {
  showWarning: notificationActions.showWarningMessage,
  writeFile: configurationActions.writeFile,
  selectedFileContentsChanged: configurationActions.selectedFileContentsChanged,
  selectVersion: configurationActions.selectConfigurationVersion,
  toggleVisualConfigurationView: configurationActions.toggleVisualConfigurationView,
  selectFile: configurationActions.selectFile,
  fetchVersions: configurationActions.fetchConfigurationVersions,
  toggleShowHiddenFiles: configurationActions.toggleShowHiddenFiles,
};

export default connect(mapStateToProps, mapDispatchToProps)(ConfigurationView);
