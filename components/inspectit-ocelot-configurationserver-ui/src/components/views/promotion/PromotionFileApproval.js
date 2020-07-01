import React from 'react';
import { ToggleButton } from 'primereact/togglebutton';

/**
 * The footer bar used by users to approve the currently selected promotion file.
 */
const PromotionFileApproval = ({approved, onApproveFile}) => {
  return (
    <>
      <style jsx>
        {`
          .this {
            border-top: 1px solid #ddd;
            display: flex;
            flex-direction: row-reverse;
            padding: 0.5rem 1rem;
          }
        `}
      </style>

      <div className="this">
        <ToggleButton
          onLabel="Approved"
          offLabel="Not Approved"
          onIcon="pi pi-check"
          offIcon="pi pi-times"
          checked={approved}
          onChange={onApproveFile}
        />
      </div>
    </>
  );
};

export default PromotionFileApproval;
