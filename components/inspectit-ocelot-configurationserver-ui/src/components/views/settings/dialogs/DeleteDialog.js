import React from 'react';
import { connect } from 'react-redux';
import { settingsActions } from '../../../../redux/ducks/settings';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * Dialog for deleting the specific user.
 */
class DeleteDialog extends React.Component {
  deleteButton = React.createRef();

  render() {
    const { id, username } = this.props.user;

    return (
      <Dialog
        header={'Delete User'}
        focusOnShow={false}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <Button label="Delete" ref={this.deleteButton} className="p-button-danger" onClick={this.deleteUser} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        }
      >
        Are you sure you want to delete the user
        <b>
          {'"'}
          {username}
          {'"'}
        </b>
        with the ID:
        <b>
          {'"'}
          {id}
          {'"'}
        </b>
        ? This cannot be undone!
      </Dialog>
    );
  }

  deleteUser = () => {
    this.props.deleteUser(this.props.user.id);
    this.props.onHide();
  };

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      /**Timeout is needed for .focus() to be triggered correctly. */
      setTimeout(() => {
        this.deleteButton.current.element.focus();
      }, 0);
    }
  }
}

const mapDispatchToProps = {
  deleteUser: settingsActions.deleteUser,
};

export default connect(null, mapDispatchToProps)(DeleteDialog);
