import React from 'react';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

class SourceTable extends React.Component {
  render() {
    return (
      <div className="this">
        <style jsx>
          {`
            .this :global(.p-datatable table thead.p-datatable-thead) {
              display: none;
            }
            .this {
              border-top: 1px solid #ddd;
            }
          `}
        </style>
        <DataTable
          value={this.props.sourcePaths}
          reorderableRows
          onRowReorder={(e) => {
            this.props.onRowReoder(e.value);
          }}
          resizableColumns
          scrollable
          scrollHeight={this.props.maxHeight ? this.props.maxHeight : '100%'}
        >
          <Column rowReorder style={{ width: '3em' }} />
          <Column body={(rowData) => rowData} />
        </DataTable>
      </div>
    );
  }
}

export default SourceTable;
