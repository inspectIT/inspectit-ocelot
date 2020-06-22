import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { promotionActions, promotionSelectors } from '../../../redux/ducks/promotion';
import { dialogActions } from '../../../redux/ducks/dialog';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
const PromotionToolbar = () => {
  const dispatch = useDispatch();

  const promoteConfiguration = () => {
    dispatch(dialogActions.showPromotionApprovalDialog());
  };

  const refreshConfiguration = () => {
    dispatch(promotionActions.fetchPromotions());
  };

  const approvalCount = useSelector(promotionSelectors.getApprovalCount);
  const updating = useSelector((state) => state.promotion.pendingRequests) > 0;

  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  return (
    <>
      <style jsx>
        {`
          .searchbox {
            display: flex;
            height: 2rem;
            align-items: center;
          }
          .searchbox :global(.pi) {
            font-size: 1.75rem;
            color: #aaa;
            margin-right: 1rem;
          }
          .headline {
            font-weight: normal;
            margin-right: 1rem;
          }
          .buttons :global(.p-button) {
            margin-left: 0.25rem;
          }
        `}
      </style>

      <Toolbar style={{ border: '0', backgroundColor: '#eee', borderBottom: '1px solid #ddd' }}>
        <div className="p-toolbar-group-left">
          <div className="searchbox">
            <i className="pi pi-cloud-upload" />
            <h4 className="headline">Configuration Promotion</h4>
          </div>
        </div>
        <div className="p-toolbar-group-right buttons">
          <Button
            disabled={updating}
            tooltip="Reload Configurations"
            tooltipOptions={tooltipOptions}
            icon={'pi pi-refresh' + (updating ? ' pi-spin' : '')}
            onClick={refreshConfiguration}
          />
          <Button disabled={approvalCount === 0} icon="pi pi-cloud-upload" label="Promote Configurations" onClick={promoteConfiguration} />
        </div>
      </Toolbar>
    </>
  );
};

export default PromotionToolbar;
