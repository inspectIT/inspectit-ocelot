import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';

/**
 * Dialog for showing the currently approved files before promoting them.
 */
const PromotionApprovalDialog = ({ visible, onHide, onPromote, isLoading, approvedFiles = [] }) => {
  const footer = (
    <div>
      <Button label="Promote" onClick={onPromote} disabled={isLoading} />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} disabled={isLoading} />
    </div>
  );

  return (
    <>
      <style jsx>
        {`
          .list li {
            font-family: monospace;
          }

          .content :global(.p-progressbar) {
            height: 0.5rem;
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

          {isLoading && <ProgressBar mode="indeterminate" />}
        </div>
      </Dialog>
    </>
  );
};

export default PromotionApprovalDialog;
