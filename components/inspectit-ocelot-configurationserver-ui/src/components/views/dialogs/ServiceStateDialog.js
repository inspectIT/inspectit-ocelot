import React from 'react';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

export default function ServiceStateDialog({ visible, onHide, serviceStateMap }) {
  let keyNames = Object.keys(serviceStateMap);
  let values = Object.values(serviceStateMap);
  let optimizedServiceStateMap = [];

  for (let i = 0; i < keyNames.length; i++) {
    let tempObject = {};
    tempObject['name'] = keyNames[i];
    tempObject['state'] = values[i];

    optimizedServiceStateMap.push(tempObject);
  }

  const nameTemplate = (rowData) => {
    return (
      <div>
        <label>{rowData.name}</label>
      </div>
    );
  };

  const stateTemplate = (rowData) => {
    return (
      <div className="this">
        <style jsx>{`
          .this {
            position: relative;
          }

          .this :global(.btn-enabled) {
            background-color: lightgreen;
            border-color: lightgreen;
            color: black;
            width: 150px;
            font-weight: bold;
          }

          .this :global(.btn-disabled) {
            background-color: lightcoral;
            border-color: lightcoral;
            color: black;
            width: 150px;
            font-weight: bold;
          }
        `}</style>
        <label>
          {rowData.state ? (
            <Button className="btn-enabled" disabled={true} label="ENABLED" color="green" />
          ) : (
            <Button className="btn-disabled" disabled={true} label="DISABLED" color="red" />
          )}
        </label>
      </div>
    );
  };

  return (
    <Dialog
      style={{ width: '50vw', overflow: 'auto' }}
      header="Service States"
      modal={true}
      visible={visible}
      onHide={onHide}
      footer={
        <div>
          <Button label="Close" onClick={onHide} className="p-button-secondary" />
        </div>
      }
    >
      <DataTable value={optimizedServiceStateMap} rowHover reorderableColumns>
        <Column header="Service Name" field="name" body={nameTemplate} sortable />
        <Column header="State" field="state" body={stateTemplate} sortable style={{ width: '175px' }} />
      </DataTable>
    </Dialog>
  );
}
