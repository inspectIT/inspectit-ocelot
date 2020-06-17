import React from 'react';
import { connect, useSelector } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { Toolbar } from 'primereact/toolbar';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';

/** Toolbar for mappingsView for changing mappings filter, downloading config files, reloading & adding mappings */
const PromotionToolbar = () => {
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
          <Button icon="pi pi-refresh" onClick={console.log} style={{ marginRight: '.25em' }} />
        </div>
      </Toolbar>
    </>
  );
};

export default PromotionToolbar;
