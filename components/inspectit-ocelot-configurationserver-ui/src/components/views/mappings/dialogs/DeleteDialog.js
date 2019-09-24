import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../../redux/ducks/mappings';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';


/**
 * Dialog for deleting the selected mapping.
 */
class DeleteDialog extends React.Component {

    deleteButton = React.createRef();

    render() {
        return (
            <Dialog
                header={"Delete mapping"}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={(
                    <div>
                        <Button label="Delete" ref={this.deleteButton} className="p-button-danger" onClick={this.handleClick} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                Are you sure you want to delete this mapping? Your changes will be lost after saving your mappings.
            </Dialog>
        )
    }

    handleClick = () => {
			this.props.onHide();
			this.props.deleteEditableMapping(this.props.mapping.name);
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.deleteButton.current.element.focus();
        }
    }
}

const mapDispatchToProps = {
  deleteEditableMapping: mappingsActions.deleteEditableMapping,
}

export default connect(null, mapDispatchToProps)(DeleteDialog);