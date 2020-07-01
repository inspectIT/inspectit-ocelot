import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

/**
 * Dialog for showing the currently approved files before promoting them.
 */
const PromotionApprovalDialog = ({ visible, onHide, onPromote, approvedFiles = [] }) => {
  const footer = (
    <div>
      <Button label="Promote" onClick={onPromote} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
    </div>
  );

  return (
    <>
      <style jsx>
        {`
          .content {
          }
          .list li {
            font-family: monospace;
          }
        `}
      </style>

      <Dialog header="Promote Configurations" visible={visible} style={{ width: '50vw' }} modal={true} onHide={onHide} footer={footer}>
        <div className="content">
          <span>The following files have been approved and will be promoted:</span>
          <ul className="list">
            {approvedFiles.map((file) => (
              <li key={file}>{file}</li>
            ))}
          </ul>
        </div>
      </Dialog>
    </>
  );
};

export default PromotionApprovalDialog;
