import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors, configurationUtils } from '../../../../redux/ducks/configuration'

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';


/**
 * Dialog for deleting the currently selected file or folder.
 */
class DeleteDialog extends React.Component {

    state = {};

    deleteButton = React.createRef();

    render() {
        const { selectionName, isDir } = this.state;

        return (
            <Dialog
                header={"Delete " + isDir}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={(
                    <div>
                        <Button label="Delete" ref={this.deleteButton} className="p-button-danger" onClick={this.deleteSelectedFile} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                Are you sure you want to delete <b>"{selectionName}"</b> ? This cannot be undone!
            </Dialog>
        )
    }

    deleteSelectedFile = () => {
        this.props.deleteSelection(true);
        this.props.onHide();
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.deleteButton.current.element.focus();

            /** Pick selection between redux state selection and incoming property selection. */
            const { selection, stateSelection, type } = this.props;

            let selectionName;
            let isDir;
            if (stateSelection) {
                selectionName = stateSelection.split("/").slice(-1)[0];
                const file = configurationUtils.getFile(this.props.files, stateSelection);
                isDir = configurationUtils.isDirectory(file) ? "Directory" : "File";
            } else {
                selectionName = selection ? selection.split("/").slice(-1)[0] : "";
                isDir = type;
            }

            this.setState({ selectionName, isDir });
        }
    }

}

function mapStateToProps(state) {
    const { selection, files } = state.configuration;
    return {
        type: configurationSelectors.isSelectionDirectory(state) ? "Directory" : "File",
        selectionName: selection ? selection.split("/").slice(-1)[0] : "",
        selection,
        files
    }
}

const mapDispatchToProps = {
    deleteSelection: configurationActions.deleteSelection
}

export default connect(mapStateToProps, mapDispatchToProps)(DeleteDialog);