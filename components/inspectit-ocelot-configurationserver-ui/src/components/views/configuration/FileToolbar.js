import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'

import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import DeleteDialog from './dialogs/DeleteDialog'
import CreateDialog from './dialogs/CreateDialog'
import MoveDialog from './dialogs/MoveDialog'

/**
 * The toolbar used in the configuration view's file tree.
 */
class FileToolbar extends React.Component {

    state = {
        isDeleteFileDialogShown: false,
        isCreateFileDialogShown: false,
        isCreateDirectoryDialogShown : false,
        isMoveDialogShown : false
    }

    fetchFiles = () => this.props.fetchFiles()

    showDeleteFileDialog = () => this.setState({ isDeleteFileDialogShown: true })

    hideDeleteFileDialog = () => this.setState({ isDeleteFileDialogShown: false })

    showCreateFileDialog = () => this.setState({ isCreateFileDialogShown: true })

    hideCreateFileDialog = () => this.setState({ isCreateFileDialogShown: false })

    showCreateDirectoryDialog = () => this.setState({ isCreateDirectoryDialogShown: true })

    hideCreateDirectoryDialog = () => this.setState({ isCreateDirectoryDialogShown: false })

    showMoveDialog = () => this.setState({ isMoveDialogShown: true })

    hideMoveDialog = () => this.setState({ isMoveDialogShown: false })

    render() {
        const { loading, selection } = this.props;

        const tooltipOptions = {
            showDelay: 500,
            position: "top"
        }
        return (
            <div className="this">
                <style jsx>{`
                .this :global(.p-toolbar) {
                    border: 0;
                    border-radius: 0;
                    background-color: #eee;
                    border-bottom: 1px solid #ddd;
                }

                .this :global(.p-toolbar-group-left) :global(.p-button) {
                    margin-right: 0.25rem;
                }
                `}</style>
                <Toolbar>
                    <div className="p-toolbar-group-left">
                        <Button disabled={loading} tooltip="New file" icon="pi pi-file" tooltipOptions={tooltipOptions} onClick={this.showCreateFileDialog}/>
                        <Button disabled={loading} tooltip="New directory" icon="pi pi-folder-open" tooltipOptions={tooltipOptions}  onClick={this.showCreateDirectoryDialog}/>
                        <Button disabled={loading || !selection} tooltip="Move/Rename file or directory" icon="pi pi-pencil" tooltipOptions={tooltipOptions} onClick={this.showMoveDialog}/>
                        <Button disabled={loading || !selection} tooltip="Delete file or directory" icon="pi pi-trash" tooltipOptions={tooltipOptions} onClick={this.showDeleteFileDialog}/>
                    </div>
                    <div className="p-toolbar-group-right">
                        <Button disabled={loading} onClick={this.fetchFiles} tooltip="Reload" icon={"pi pi-refresh" + (loading ? " pi-spin" : "")} tooltipOptions={tooltipOptions} />
                    </div>
                </Toolbar>
                <DeleteDialog visible={this.state.isDeleteFileDialogShown} onHide={this.hideDeleteFileDialog} />
                <CreateDialog directoryMode={false} visible={this.state.isCreateFileDialogShown} onHide={this.hideCreateFileDialog} />
                <CreateDialog directoryMode={true} visible={this.state.isCreateDirectoryDialogShown} onHide={this.hideCreateDirectoryDialog} />
                <MoveDialog visible={this.state.isMoveDialogShown} onHide={this.hideMoveDialog} />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const { pendingRequests, selection } = state.configuration;
    return {
        loading: pendingRequests > 0,
        selection
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles
}

export default connect(mapStateToProps, mapDispatchToProps)(FileToolbar);