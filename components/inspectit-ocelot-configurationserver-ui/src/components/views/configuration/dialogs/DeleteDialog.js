import React from 'react'
import { connect } from 'react-redux'
import { configurationActions, configurationSelectors } from '../../../../redux/ducks/configuration'
import {uniqueId} from 'lodash'

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';


/**
 * Dialog for deleting the currently selected file or folder.
 */
class DeleteDialog extends React.Component {

    state = {
        deleteButtonId: uniqueId("deletebtn")
    }

    render () {
        const {selection } = this.props;
        const selectedName = selection ? selection.split("/").slice(-1)[0] : ""

        return (
            <Dialog
                    header={"Delete "+ this.props.type}
                    modal={true}
                    visible={this.props.visible}
                    onHide={this.props.onHide}
                    footer={(
                        <div>
                            <Button label="Delete" id={this.state.deleteButtonId} className="p-button-danger" onClick={this.deleteSelectedFile}/>
                            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide}/>
                        </div>
                    )}
                >
                    {"Are you sure you want to delete "}<b>{"\"" + selectedName + "\""}</b>{" ? This cannot be undone!"}
                </Dialog>
        )
    }

    deleteSelectedFile = () => {
        this.props.deleteSelection(true);
        this.props.onHide();
    }

    componentDidUpdate(prevProps) {
        if(!prevProps.visible && this.props.visible) {
            document.getElementById(this.state.deleteButtonId).focus();
        }
    }
    
}

function mapStateToProps(state) {
    const { selection } = state.configuration;
    return {
        type: configurationSelectors.isSelectionDirectory(state) ? "Directory" : "File",
        selection
    }
}

const mapDispatchToProps = {
    deleteSelection: configurationActions.deleteSelection
}

export default connect(mapStateToProps, mapDispatchToProps)(DeleteDialog);