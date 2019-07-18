import React from 'react';
import { connect } from 'react-redux'
import { configurationActions } from '../../../redux/ducks/configuration'

import FileTree from './FileTree';
import FileToolbar from './FileToolbar';

/**
 * The configuration view component used for managing the agent configurations.
 */
class ConfigurationView extends React.Component {

    render() {
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
                <div>{this.props.selection}</div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { updateDate, selection } = state.configuration;
    return {
        updateDate,
        selection
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles
}

export default connect(mapStateToProps, mapDispatchToProps)(ConfigurationView);