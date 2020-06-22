import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { promotionActions } from '../../../redux/ducks/promotion';
import { dialogActions } from '../../../redux/ducks/dialog';
import { PROMOTION_APPROVAL_DIALOG } from '../dialogs';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

const PromotionApprovalDialog = () => {
  const dispatch = useDispatch();

  const show = useSelector((state) => state.dialog.show) === PROMOTION_APPROVAL_DIALOG;
  const approvedFiles = useSelector((state) => state.promotion.files).filter((file) => file.approved);

  const hideDialog = () => {
    if (show) {
      dispatch(dialogActions.hideDialogs());
    }
  };

  const promoteFiles = () => {
    dispatch(promotionActions.promoteConfiguration());
    hideDialog();
  };

  const footer = (
    <div>
      <Button label="Promote" onClick={promoteFiles} />
      <Button label="Cancel" className="p-button-secondary" onClick={hideDialog} />
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

      <Dialog
        header="Promote Configurations"
        visible={show}
        style={{ width: '50vw' }}
        modal={true}
        onHide={() => hideDialog()}
        footer={footer}
      >
        <div className="content">
          <span>The following files have been approved and will be promoted:</span>
          <ul className="list">
            {approvedFiles.map(({ file }) => (
              <li key={file}>{file}</li>
            ))}
          </ul>
        </div>
      </Dialog>
    </>
  );
};

export default PromotionApprovalDialog;
