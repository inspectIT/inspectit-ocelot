import React from 'react';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import classNames from 'classnames';

import { cloneDeep, isEqual, assign } from 'lodash';

class KeyValueEditor extends React.Component {
  editor = (columnProps) => {
    return (
      <InputText
        defaultValue={columnProps.rowData[columnProps.field]}
        onChange={(e) => this.valueChanged(columnProps.rowIndex, columnProps.field, e.target.value)}
      />
    );
  };

  valueChanged = (row, field, newValue) => {
    const dataCopy = cloneDeep(this.props.keyValueArray || []);
    dataCopy.push({});
    dataCopy[row][field] = newValue;

    const newData = dataCopy.filter((obj) => obj.key || obj.value);
    if (!isEqual(this.props.keyValueArray, newData)) {
      this.props.onChange(newData);
    }
  };

  handleDelete = (rowIndex) => {
    this.props.onChange(this.props.keyValueArray.filter((obj, idx) => idx !== rowIndex));
  };

  render() {
    let dataArray = cloneDeep(this.props.keyValueArray) || [];
    dataArray.push({});
    dataArray.map((data, index) => assign(data, { index }));

    const valueColumnPattern = (rowData) => {
      const className = classNames({
        error: rowData.hasErrors,
        regex: true,
      });
      return (
        (
          <div className={className}>
            <style jsx>
              {`
                .error {
                  color: #f44336;
                }
                .error-sign {
                  float: right;
                  font-size: 1.25rem;
                }
                .regex {
                  font-family: monospace;
                }
              `}
            </style>
            {rowData.value}
            {rowData.hasErrors && <i className="error-sign pi pi-exclamation-triangle" title={rowData.errorMessage}></i>}
          </div>
        ) || (
          <div>
            <p style={{ color: 'grey' }}>click here to add new value</p>
          </div>
        )
      );
    };

    return (
      <DataTable value={dataArray} scrollable={true} scrollHeight={this.props.maxHeight ? this.props.maxHeight : '100%'}>
        <Column
          columnKey="key"
          field="key"
          header="Key"
          headerStyle={{ fontWeight: 'normal' }}
          body={(rowData) => rowData.key || <p style={{ color: 'grey' }}>click here to add new key</p>}
          editor={this.editor}
        />
        <Column
          columnKey="value"
          field="value"
          header="Value"
          headerStyle={{ fontWeight: 'normal' }}
          body={(rowData) => valueColumnPattern(rowData)}
          editor={this.editor}
        />
        <Column
          columnKey="buttonRow"
          body={(rowData) =>
            rowData.key || rowData.value ? <Button icon="pi pi-trash" onClick={() => this.handleDelete(rowData.index)} /> : ''
          }
          style={{ width: '4em' }}
        />
      </DataTable>
    );
  }
}

export default KeyValueEditor;
