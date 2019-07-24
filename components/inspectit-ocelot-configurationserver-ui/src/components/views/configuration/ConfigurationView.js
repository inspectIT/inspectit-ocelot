import React from 'react';
import { connect } from 'react-redux'
import { configurationSelectors } from '../../../redux/ducks/configuration'

import FileTree from './FileTree';
import FileToolbar from './FileToolbar';
import EditorView from '../../editor/EditorView';

/**
 * The header component of the editor view.
 */
const EditorHeader = ({ icon, path, name }) => (
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
        `}</style>
        <div className="header">
            <i className={"pi " + icon}></i>
            <div className="path">{path}</div>
            <div className="name">{name}</div>
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
        console.log("save");
    }

    render() {
        const { selection, isDirectory } = this.props;
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
                <EditorView showEditor={showEditor} content={this.props.selection} hint={"Select a file to start editing."} onSave={this.onSave}>
                    {showHeader && <EditorHeader icon={icon} path={path} name={name} />}
                </EditorView>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { updateDate, selection } = state.configuration;
    return {
        updateDate,
        selection,
        isDirectory: configurationSelectors.isSelectionDirectory(state)
    }
}

export default connect(mapStateToProps, null)(ConfigurationView);