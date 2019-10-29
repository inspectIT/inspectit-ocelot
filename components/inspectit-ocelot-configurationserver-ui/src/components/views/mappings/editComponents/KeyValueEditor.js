import React from 'react'
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

import { cloneDeep } from 'lodash';

class KeyValueEditor extends React.Component{

  constructor(props){
    super(props)
    this.state = {
      editValue: null,
    }
  }

  editor = (columnProps) => {
    const { editValue } = this.state;
    return ( 
      <InputText 
        value={editValue === null ? columnProps.rowData[columnProps.field] : editValue}
        onChange={e => this.setState({editValue: e.target.value})}
        onBlur={e => this.handleBlur(e.target.value, columnProps)}
        onKeyDown={e => this.handleKeyDown(e.key, e.target.value, columnProps)}
      /> 
    )
  }

  handleKeyDown = (key, targetValue, columnProps) => {
    if(key === 'Enter' || key === 'Tab'){
      this.handleBlur(targetValue, columnProps)
    }
    if(key === 'Tab'){
      this.setState({editValue: ''})
    }
  }

  handleBlur = (newValue, columnProps) => {
    const dataCopy = cloneDeep(columnProps.value)
    dataCopy[columnProps.rowIndex][columnProps.field] = newValue

    this.props.onChange(dataCopy.filter(obj => obj.key || obj.value))
    this.setState({editValue: null});
  }

  handleDelete = (rowData) => {
    this.props.onChange(this.props.keyValueArray.filter(obj => obj.key !== rowData.key || obj.value !== rowData.value))
  }

  render() {
    const dataArray = cloneDeep(this.props.keyValueArray) || [];
    dataArray.push({});

    return(
      <DataTable 
        value={dataArray} 
        scrollable={true} 
        scrollHeight={this.props.maxHeight ? this.props.maxHeight : '100%'}
      >
        <Column 
          columnKey='key' 
          field='key' 
          header='Key'
          headerStyle={{'font-weight': 'normal'}}
          body={rowData => rowData.key || <p style={{color: 'grey'}}>click here to add new key</p>} 
          editor={this.editor} 
          />
        <Column 
          columnKey='value' 
          field='value' 
          header='Value'
          headerStyle={{'font-weight': 'normal'}}
          body={rowData => rowData.value || <p style={{color: 'grey'}}>click here to add new value</p>} 
          editor={this.editor} 
          />
        <Column 
          columnKey='buttonRow'
          body={rowData => (rowData.key || rowData.value) != null ? <Button icon='pi pi-trash' onClick={() => this.handleDelete(rowData)}/> : ''}
          style={{width: '4em'}}
        />
      </DataTable>
    )
  }
}


export default KeyValueEditor;