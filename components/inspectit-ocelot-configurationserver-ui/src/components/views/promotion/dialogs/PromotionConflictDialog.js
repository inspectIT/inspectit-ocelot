import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

/**
 * Dialog for showing promotion conflicts. A conflict can occure if the
 * live branch has been modified and the user tries to promote new files.
 */
const PromotionConflictDialog = ({ visible, onHide, onRefresh }) => {
  const refresh = () => {
    onRefresh();
    onHide();
  };

  const footer = (
    <div>
      <Button label="Continue" onClick={onHide} />
      <Button label="Continue and Refresh" className="p-button-secondary" onClick={refresh} />
    </div>
  );

  return (
    <Dialog
      header="Concurrent Modification of Configurations"
      visible={visible}
      style={{ width: '50vw' }}
      modal={true}
      onHide={onHide}
      footer={footer}
    >
      <p>One or more configurations have been promoted in the meantime.</p>
      <p>Your current state is out of sync, thus, cannot be promoted in order to prevent negative side effects.</p>
      <p>Please refresh your promotion files and try again.</p>
    </Dialog>
  );
};

export default PromotionConflictDialog;
