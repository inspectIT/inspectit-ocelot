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
            margin-left: 30%;
            font-weight: bold;
          }

          .this :global(.btn-disabled) {
            background-color: lightcoral;
            border-color: lightcoral;
            color: black;
            width: 150px;
            margin-left: 30%;
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
          <Button label="Go Back" onClick={onHide} className="p-button-secondary" />
          <Button label="Accept" onClick={onHide} className="p-button-primary" />
        </div>
      }
    >
      <DataTable value={optimizedServiceStateMap} rowHover reorderableColumns autoLayout={true}>
        <Column header="Service Name" body={nameTemplate} />
        <Column header="State" body={stateTemplate} />
      </DataTable>
    </Dialog>
  );
}
