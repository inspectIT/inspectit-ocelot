import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../../redux/ducks/mappings';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';


/**
 * Dialog for refreshing the shown mappings.
 */
class RefreshDialog extends React.Component {

    refreshButton = React.createRef();

    render() {
        return (
            <Dialog
                header={"Refresh Mappings"}
                modal={true}
                visible={this.props.visible}
                onHide={this.props.onHide}
                footer={(
                    <div>
                        <Button label="Refresh" ref={this.refreshButton} className="p-button-danger" onClick={this.handleClick} />
                        <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
                    </div>
                )}
            >
                Are you sure you want to refresh this page? Your changes will be lost. This cannot be undone!
            </Dialog>
        )
    }

    handleClick = () => {
      this.props.fetchMappings();
      this.props.onHide();
    }

    componentDidUpdate(prevProps) {
        if (!prevProps.visible && this.props.visible) {
            this.refreshButton.current.element.focus();
        }
    }
}

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
}

export default connect(null, mapDispatchToProps)(RefreshDialog);