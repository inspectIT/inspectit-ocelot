import yaml from 'js-yaml';
import React from 'react';
import { connect } from 'react-redux';
import { DEFAULT_CONFIG_TREE_KEY } from '../../../data/constants';
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration';
import { notificationActions } from '../../../redux/ducks/notification';
import EditorView from '../../editor/EditorView';
import CreateDialog from './dialogs/CreateDialog';
import DeleteDialog from './dialogs/DeleteDialog';
import MoveDialog from './dialogs/MoveDialog';
import FileToolbar from './FileToolbar';
import FileTree from './FileTree';
import { enableOcelotAutocompletion } from './OcelotAutocompleter';



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
            font-size: 1.75rem;
            color: #aaa;
            margin-right: 1rem;
        }
        .path {
            color: #999;
        }
        .dirtyStateMarker {
            margin-left: .25rem;
            color: #999;
        }
        `}</style>
        <div className="header">
            <i className={"pi " + icon}></i>
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
        isCreateFileDialogShown: false,
        isCreateDirectoryDialogShown: false,
        isMoveDialogShown: false,
        filePath: null
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
    }

    splitIntoPathAndName = (path) => {
        const lastIndex = path.lastIndexOf("/") + 1;
        return {
            path: path.slice(0, lastIndex),
            name: path.slice(lastIndex)
        };
    }

    onSave = () => {
        const { selection, fileContent } = this.props;
        this.props.writeFile(selection, fileContent, false);
    }

    onChange = (value) => {
        if (!this.props.loading) {
            this.props.selectedFileContentsChanged(value);
        }
    }

    onRefresh = () => {
        this.props.selectedFileContentsChanged(null);
    }

    showDeleteFileDialog = (filePath) => this.setState({ isDeleteFileDialogShown: true, filePath });

    hideDeleteFileDialog = () => this.setState({ isDeleteFileDialogShown: false, filePath: null });

    showCreateFileDialog = (filePath) => this.setState({ isCreateFileDialogShown: true, filePath });

    hideCreateFileDialog = () => this.setState({ isCreateFileDialogShown: false, filePath: null });

    showCreateDirectoryDialog = (filePath) => this.setState({ isCreateDirectoryDialogShown: true, filePath });

    hideCreateDirectoryDialog = () => this.setState({ isCreateDirectoryDialogShown: false, filePath: null });

    showMoveDialog = (filePath) => this.setState({ isMoveDialogShown: true, filePath });

    hideMoveDialog = () => this.setState({ isMoveDialogShown: false, filePath: null });

    render() {
        const { selection, isDirectory, loading, isContentModified, fileContent, yamlError, selectedDefaultConfigFile, schema, showVisualConfigurationView, toggleVisualConfigurationView } = this.props;
        const showEditor = (selection || selectedDefaultConfigFile) && !isDirectory;

        const { path, name } = this.parsePath(selection, selectedDefaultConfigFile);
        const icon = "pi-" + (isDirectory ? "folder" : "file");
        const showHeader = !!name;

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
                    padding: .25rem 0;
                }
                `}</style>
                <div className="treeContainer">
                    <FileToolbar
                        showDeleteFileDialog={this.showDeleteFileDialog}
                        showCreateFileDialog={this.showCreateFileDialog}
                        showCreateDirectoryDialog={this.showCreateDirectoryDialog}
                        showMoveDialog={this.showMoveDialog}
                    />
                    <FileTree
                        className="fileTree"
                        showDeleteFileDialog={this.showDeleteFileDialog}
                        showCreateFileDialog={this.showCreateFileDialog}
                        showCreateDirectoryDialog={this.showCreateDirectoryDialog}
                        showMoveDialog={this.showMoveDialog}
                    />
                    <div className="details">Last refresh: {this.props.updateDate ? new Date(this.props.updateDate).toLocaleString() : "-"}</div>
                </div>
                <EditorView
                    showEditor={showEditor}
                    value={fileContent}
                    schema={schema}
                    hint={"Select a file to start editing."}
                    onSave={this.onSave}
                    enableButtons={showEditor && !loading}
                    onCreate={enableOcelotAutocompletion}
                    onChange={this.onChange}
                    onRefresh={this.onRefresh}
                    canSave={isContentModified && !yamlError}
                    isErrorNotification={true}
                    notificationIcon="pi-exclamation-triangle"
                    notificationText={yamlError}
                    loading={loading}
                    readOnly={!!selectedDefaultConfigFile}
                    showVisualConfigurationView={showVisualConfigurationView}
                    onToggleVisualConfigurationView={toggleVisualConfigurationView}
                >
                    {showHeader ? <EditorHeader icon={icon} path={path} name={name} isContentModified={isContentModified} readOnly={!!selectedDefaultConfigFile} /> : null}
                </EditorView>
                <DeleteDialog visible={this.state.isDeleteFileDialogShown} onHide={this.hideDeleteFileDialog} filePath={this.state.filePath} />
                <CreateDialog directoryMode={false} visible={this.state.isCreateFileDialogShown} onHide={this.hideCreateFileDialog} filePath={this.state.filePath} />
                <CreateDialog directoryMode={true} visible={this.state.isCreateDirectoryDialogShown} onHide={this.hideCreateDirectoryDialog} filePath={this.state.filePath} />
                <MoveDialog visible={this.state.isMoveDialogShown} onHide={this.hideMoveDialog} filePath={this.state.filePath} />
            </div>
        );
    }
}

const getYamlError = (content) => {
    try {
        yaml.safeLoad(content);
        return null;
    } catch (error) {
        if (error.message) {
            return "YAML Syntax Error: " + error.message;
        } else {
            return "YAML cannot be parsed.";
        }
    }
}

function mapStateToProps(state) {
    const { updateDate, selection, selectedFileContent, pendingRequests, selectedDefaultConfigFile, schema, showVisualConfigurationView } = state.configuration;
    const unsavedFileContent = selection ? configurationSelectors.getSelectedFileUnsavedContents(state) : null;
    const fileContent = unsavedFileContent != null ? unsavedFileContent : selectedFileContent;

    return {
        updateDate,
        selection,
        isDirectory: configurationSelectors.isSelectionDirectory(state) || !!(selectedDefaultConfigFile && !fileContent),
        isContentModified: unsavedFileContent != null,
        fileContent,
        yamlError: getYamlError(fileContent),
        loading: pendingRequests > 0,
        selectedDefaultConfigFile,
        schema,
        showVisualConfigurationView
    }
}

const mapDispatchToProps = {
    showWarning: notificationActions.showWarningMessage,
    writeFile: configurationActions.writeFile,
    selectedFileContentsChanged: configurationActions.selectedFileContentsChanged,
    toggleVisualConfigurationView: configurationActions.toggleVisualConfigurationView
}

export default connect(mapStateToProps, mapDispatchToProps)(ConfigurationView);