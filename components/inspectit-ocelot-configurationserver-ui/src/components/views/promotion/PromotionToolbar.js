import React from 'react';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar of the promotion view.
 */
const PromotionToolbar = ({ onRefresh, onPromote, loading, enabled, canPromote }) => {
  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  return (
    <div className="this">
      <style jsx>
        {`
          .this :global(.p-toolbar) {
            padding-top: 0.5rem;
            padding-bottom: 0.5rem;
          }
          .searchbox {
            display: flex;
            height: 2rem;
            align-items: center;
          }
          .searchbox :global(.pi) {
            font-size: 1.25rem;
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

      <Toolbar
        style={{ border: '0', backgroundColor: '#eee', borderBottom: '1px solid #ddd' }}
        left={
          <div className="p-toolbar-group-left">
            <div className="searchbox">
              <i className="pi pi-cloud-upload" />
              <h4 className="headline">File Promotion</h4>
            </div>
          </div>
        }
        right={
          <div className="p-toolbar-group-right buttons">
            <Button
              disabled={loading}
              tooltip="Reload Available Changes"
              tooltipOptions={tooltipOptions}
              icon={'pi pi-refresh' + (loading ? ' pi-spin' : '')}
              onClick={onRefresh}
            />
            {canPromote && <Button disabled={!enabled} icon="pi pi-cloud-upload" label="Promote Files" onClick={onPromote} />}
          </div>
        }
      />
    </div>
  );
};

export default PromotionToolbar;
