import React from 'react';
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'
import { notificationActions } from '../../../redux/ducks/notification';

import FileTree from './FileTree';
import FileToolbar from './FileToolbar';
import EditorView from '../../editor/EditorView';
import yaml from 'js-yaml';

/**
 * The header component of the editor view.
 */
const EditorHeader = ({ icon, path, name, isContentModified }) => (
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
        </div>
    </>
);

/**
 * The configuration view component used for managing the agent configurations.
 */
class ConfigurationView extends React.Component {

    parsePath = (fullPath) => {
        if (fullPath) {
            const lastIndex = fullPath.lastIndexOf("/") + 1;
            return {
                path: fullPath.slice(0, lastIndex),
                name: fullPath.slice(lastIndex)
            }
        } else {
            return {};
        }
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

    render() {
        const { selection, isDirectory, loading, isContentModified, fileContent, yamlError } = this.props;
        const showEditor = selection && !isDirectory;

        const { path, name } = this.parsePath(selection);
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
                    <FileToolbar />
                    <FileTree className="fileTree" />
                    <div className="details">Last update: {this.props.updateDate ? new Date(this.props.updateDate).toLocaleString() : "-"}</div>
                </div>
                <EditorView
                    showEditor={showEditor}
                    value={fileContent}
                    hint={"Select a file to start editing."}
                    onSave={this.onSave}
                    enableButtons={showEditor && !loading}
                    onChange={this.onChange}
                    onRefresh={this.onRefresh}
                    canSave={isContentModified && !yamlError}
                    isErrorNotification={true}
                    notificationIcon="pi-exclamation-triangle"
                    notificationText={yamlError}
                    loading={loading}>
                    {showHeader ? <EditorHeader icon={icon} path={path} name={name} isContentModified={isContentModified} /> : null}
                </EditorView>
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
    const { updateDate, selection, selectedFileContent, pendingRequests } = state.configuration;
    const unsavedFileContent = selection ? configurationSelectors.getSelectedFileUnsavedContents(state) : null;
    const fileContent = unsavedFileContent != null ? unsavedFileContent : selectedFileContent;

    return {
        updateDate,
        selection,
        isDirectory: configurationSelectors.isSelectionDirectory(state),
        isContentModified: unsavedFileContent != null,
        fileContent,
        yamlError: getYamlError(fileContent),
        loading: pendingRequests > 0
    }
}

const mapDispatchToProps = {
    showWarning: notificationActions.showWarningMessage,
    writeFile: configurationActions.writeFile,
    selectedFileContentsChanged: configurationActions.selectedFileContentsChanged
}

export default connect(mapStateToProps, mapDispatchToProps)(ConfigurationView);