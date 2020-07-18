import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * A generic Dialog for deleting the given element.
 */
const DeleteDialog = ({ name, visible, onHide, text, onSuccess }) => {
  const deleteButton = React.createRef();
  useEffect(() => {
    if (deleteButton && deleteButton.current) {
      deleteButton.current.element.focus();
    }
  });
  return (
    <Dialog
      header={text}
      modal={true}
      visible={visible}
      onHide={onHide}
      footer={
        <div>
          <Button
            label="Delete"
            ref={deleteButton}
            className="p-button-danger"
            onClick={() => {
              onSuccess(name);
              onHide();
            }}
          />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      Are you sure you want to delete <b>&quot;{name}&quot;</b> ? This cannot be undone!
    </Dialog>
  );
};

DeleteDialog.propTypes = {
  /** The name of the element to delete */
  name: PropTypes.string,
  /** The text to show in the dialog */
  text: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on deletion success */
  onSuccess: PropTypes.func,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

DeleteDialog.defaultProps = {
  text: 'Delete element',
  visible: true,
  onSuccess: () => {},
  onHide: () => {},
};

export default DeleteDialog;
