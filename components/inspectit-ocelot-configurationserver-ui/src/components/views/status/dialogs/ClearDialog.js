import React from 'react';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../../redux/ducks/agent-status';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * Dialog for clearing all agent statuses.
 */
class ClearDialog extends React.Component {
  clearButton = React.createRef();

  render() {
    return (
      <Dialog
        header={'Clear History of all Agents' + this.props.type}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <Button label="Clear All" ref={this.clearButton} className="p-button-danger" onClick={this.clearAndHide} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        Are you sure that you want to clear the history of all agents?
      </Dialog>
    );
  }

  clearAndHide = () => {
    this.props.clearStatus(true);
    this.props.onHide();
  };

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      this.clearButton.current.element.focus();
    }
  }
}

const mapDispatchToProps = {
  clearStatus: agentStatusActions.clearStatus,
};

export default connect(null, mapDispatchToProps)(ClearDialog);
