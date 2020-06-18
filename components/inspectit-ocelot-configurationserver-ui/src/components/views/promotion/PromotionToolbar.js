import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { promotionActions, promotionSelectors } from '../../../redux/ducks/promotion';
import { Toolbar } from 'primereact/toolbar';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
const PromotionToolbar = () => {
  const dispatch = useDispatch();

  const promoteConfiguration = () => {
    dispatch(promotionActions.promoteConfiguration());
  };

  const approvalCount = useSelector(promotionSelectors.getApprovalCount);

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
      `}
      </style>

      <Toolbar style={{ border: '0', backgroundColor: '#eee', borderBottom: '1px solid #ddd' }}>
        <div className="p-toolbar-group-left">
          <div className="searchbox">
            <i className="pi pi-unlock" />
            <h4 style={{ fontWeight: 'normal', marginRight: '1rem' }}>Configuration Promotion</h4>
          </div>
        </div>
        <div className="p-toolbar-group-right">
        <Button icon="pi pi-refresh" onClick={console.log} style={{ marginLeft: '.25em' }} />
        <Button disabled={approvalCount === 0} icon="pi pi-unlock" label="Promote Approved Configurations" onClick={promoteConfiguration} style={{ marginLeft: '.25em' }} />
        </div>
      </Toolbar>
    </>
  );
};

export default PromotionToolbar;
