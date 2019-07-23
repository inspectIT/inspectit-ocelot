import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../redux/ducks/configuration'

import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import DeleteDialog from './dialogs/DeleteDialog'
import CreateDialog from './dialogs/CreateDialog'

/**
 * The toolbar used in the configuration view's file tree.
 */
class FileToolbar extends React.Component {

    state = {
        deleteFileDialogShown: false,
        createFileDialogShown: false,
        createDirectoryDialogShown : false,
    }

    fetchFiles = () => {
        this.props.fetchFiles();
    }

    showDeleteFileDialog = () => this.setState({ deleteFileDialogShown: true })
    hideDeleteFileDialog = () => this.setState({ deleteFileDialogShown: false })
    showCreateFileDialog = () => this.setState({ createFileDialogShown: true })
    hideCreateFileDialog = () => this.setState({ createFileDialogShown: false })
    showCreateDirectoryDialog = () => this.setState({ createDirectoryDialogShown: true })
    hideCreateDirectoryDialog = () => this.setState({ createDirectoryDialogShown: false })

    render() {
        const { loading, selection } = this.props;
        const selectedName = selection ? selection.split("/").slice(-1)[0] : ""
        const isLoading = loading > 0;

        const tooltipOptions = {
            showDelay: 500,
            position: "top"
        }
        return (
            <div className="this">
                <style jsx>{`
                .this :global(.p-toolbar) {
                    background: 0;
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
                        <Button disabled={isLoading} tooltip="New file" icon="pi pi-file" tooltipOptions={tooltipOptions} onClick={this.showCreateFileDialog}/>
                        <Button disabled={isLoading} tooltip="New directory" icon="pi pi-folder-open" tooltipOptions={tooltipOptions}  onClick={this.showCreateDirectoryDialog}/>
                        <Button disabled={isLoading || !selection} tooltip="Delete file or directory" icon="pi pi-trash" tooltipOptions={tooltipOptions} onClick={this.showDeleteFileDialog}/>
                    </div>
                    <div className="p-toolbar-group-right">
                        <Button disabled={isLoading} onClick={this.fetchFiles} tooltip="Reload" icon={"pi pi-refresh" + (isLoading ? " pi-spin" : "")} tooltipOptions={tooltipOptions} />
                    </div>
                </Toolbar>
                <DeleteDialog visible={this.state.deleteFileDialogShown} onHide={this.hideDeleteFileDialog} />
                <CreateDialog directoryMode={false} visible={this.state.createFileDialogShown} onHide={this.hideCreateFileDialog} />
                <CreateDialog directoryMode={false} visible={this.state.createFileDialogShown} onHide={this.hideCreateFileDialog} />
                <CreateDialog directoryMode={true} visible={this.state.createDirectoryDialogShown} onHide={this.hideCreateDirectoryDialog} />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const { loading, selection } = state.configuration;
    return {
        loading,
        selection
    }
}

const mapDispatchToProps = {
    fetchFiles: configurationActions.fetchFiles
}

export default connect(mapStateToProps, mapDispatchToProps)(FileToolbar);