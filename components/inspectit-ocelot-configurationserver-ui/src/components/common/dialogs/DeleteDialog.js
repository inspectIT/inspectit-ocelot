import React, { useEffect } from 'react';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';

/**
 * A generic Dialog for deleting the given element.
 */
const DeleteDialog = ({name, visible, onHide, text, onSuccess}) => {
  const deleteButton = React.createRef();
  useEffect(() => {
    if(deleteButton && deleteButton.current){
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
          <Button label="Delete" ref={deleteButton} className="p-button-danger" onClick={() => {onSuccess(name); onHide();}} />
          <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
        </div>
      }
    >
      Are you sure you want to delete <b>&quot;{name}&quot;</b> ? This cannot be undone!
    </Dialog>
  );
};


export default DeleteDialog;
