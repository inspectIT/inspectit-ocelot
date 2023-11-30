import React from 'react';
import { connect } from 'react-redux';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { mappingsActions } from '../../../../redux/ducks/mappings';

/**
 * Dialog for changing the agent mappings source branch
 */
class ChangeSourceBranchDialog extends React.Component {
  changeSourceBranch = (selectedSourceBranch) => {
    if (selectedSourceBranch) {
      this.props.putSourceBranch(selectedSourceBranch);
    }
    this.props.onHide();
  };
  render() {
    return (
      <Dialog
        header={'Change source branch for agent mappings configuration'}
        modal={true}
        focusOnShow={false}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <Button label="Change" className="p-button-danger" onClick={this.changeAndHide} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        Are you sure that you want to change the source branch for
        <br></br> the agent mappings configuration to <b>{this.props.selectedSourceBranch}?</b>
      </Dialog>
    );
  }

  changeAndHide = () => {
    const selectedBranch = this.props.selectedSourceBranch;
    this.changeSourceBranch(selectedBranch);
    this.props.onHide();
  };
}

const mapDispatchToProps = {
  putSourceBranch: mappingsActions.putMappingsSourceBranch,
};

export default connect(null, mapDispatchToProps)(ChangeSourceBranchDialog);
