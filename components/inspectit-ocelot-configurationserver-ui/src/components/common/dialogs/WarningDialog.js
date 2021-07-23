import React from 'react';
import PropTypes from 'prop-types';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

/**
 * Generic dialog for showing warnings.
 */
const WarningDialog = ({ visible, onHide, title = 'Warning', children }) => {
  const footer = (
    <div>
      <Button label="Continue" onClick={onHide} />
    </div>
  );

  return (
    <Dialog header={title} visible={visible} style={{ width: '50vw' }} modal={true} onHide={onHide} footer={footer}>
      <div style={{ display: 'flex', flexDirection: 'row' }}>
        <i style={{ color: '#ffd54f', margin: '1rem 1rem 1rem 0rem', fontSize: '2.5rem' }} className="pi pi-exclamation-triangle"></i>
        <div style={{ flexGrow: 1 }}>{children}</div>
      </div>
    </Dialog>
  );
};

WarningDialog.propTypes = {
  /** The title to show in the dialog */
  title: PropTypes.string,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

export default WarningDialog;
