import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { promotionActions } from '../../../redux/ducks/promotion';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

const PromotionConflictDialog = () => {
  const dispatch = useDispatch();

  const showConflictDialog = useSelector((state) => state.promotion.showConflictDialog);

  const hideConflictDialog = () => {
    dispatch(promotionActions.hideConflictDialog());
  };

  const hideAndRefresh = () => {
    dispatch(promotionActions.hideConflictDialog());
    dispatch(promotionActions.fetchPromotions());
  };

  const footer = (
    <div>
      <Button label="Continue" onClick={hideConflictDialog} />
      <Button label="Continue and Refresh" className="p-button-secondary" onClick={hideAndRefresh} />
    </div>
  );

  return (
    <Dialog
      header="Concurrent Modification of Configurations"
      visible={showConflictDialog}
      style={{ width: '50vw' }}
      modal={true}
      onHide={() => hideConflictDialog()}
      footer={footer}
    >
      <p>One or more configurations have been promoted in the meantime.</p>
      <p>Your current state is out of sync, thus, cannot be promoted in order to negative side effects.</p>
      <p>Please refresh your promotion files and try again.</p>
    </Dialog>
  );
};

export default PromotionConflictDialog;
