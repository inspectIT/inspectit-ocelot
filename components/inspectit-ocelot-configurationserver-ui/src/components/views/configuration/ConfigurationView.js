import React from 'react';
import FileTree from './FileTree';
import { connect } from 'react-redux'

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

export default connect(mapStateToProps, null)(ConfigurationView);