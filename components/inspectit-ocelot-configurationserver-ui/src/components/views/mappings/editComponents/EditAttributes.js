import React from 'react'
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

class EditAttributes extends React.Component{

  constructor(props){
    super(props)
    this.state = {
      attKey: null,
      attValue: null
    }
  }

  /** editor functions will be called instead of body functions during edit mode of DataTable*/
  keyEditor = (columnProps) => {
    const {attKey} = this.state
 
    return ( 
      <InputText 
        value={attKey === null ? Object.keys(columnProps.rowData)[0] : attKey}
        onChange={e => this.setState({attKey: e.target.value})}
        onBlur={e => this.onBlur_KeyField(e.target.value, columnProps)}
        onKeyDown={e => {
          if(e.key === 'Enter' || e.key === 'Tab'){
            this.onBlur_KeyField(e.target.value, columnProps)}
            if(e.key === 'Tab'){
              this.setState({attValue: ''})
            }
        }}
      /> 
    )
  }

  valueEditor = (columnProps) => {
    const {attValue} = this.state

    return ( 
      <InputText 
        value={attValue === null ? columnProps.rowData[Object.keys(columnProps.rowData)[0]] : attValue}
        onChange={e => this.setState({attValue: e.target.value})} 
        onBlur={e => this.onBlur_ValueField(e.target.value, columnProps.rowData)}
        onKeyDown={e =>{
          if(e.key === 'Enter' || e.key === 'Tab'){
            this.onBlur_ValueField(e.target.value, columnProps.rowData)
          } 
        }}
      /> 
    )
  }

  onBlur_KeyField = (newKey, columnProps) => {
    const allAttKeys = Object.keys(this.props.attributes);

    if(newKey !== '' && !allAttKeys.includes(newKey)){
      const oldKey = Object.keys(columnProps.rowData)[0];
      this.props.onChangeAttributeKey(oldKey, newKey);
    } 
    else if (allAttKeys[columnProps.rowIndex] !== newKey) {
      this.props.showWarningMessage('Attribute has not been Changed', !newKey ? 'The attribute key cannot be empty' : 'This mapping already has an attribute with the given key');
    }
    this.setState({attKey: null});
  }

  onBlur_ValueField = (newValue, rowData) => {
    const key = Object.keys(rowData)[0];

    if(newValue !== '') {
      this.props.onChValueOrAddAttribute(key, newValue);
    } else {
      this.props.showWarningMessage('Attribute has not been Changed', 'The attribute value cannot be empty');
    }

    this.setState({attValue: null});
  }

  render() {
    let attributesArray = objectToArray(this.props.attributes);
    attributesArray.push({})

    return(
      <DataTable 
        value={attributesArray} 
        scrollable={true} 
        scrollHeight={`100%`} 
      >
        <Column 
          columnKey='key' 
          field='key' 
          header='Attribute Key'
          body={rowData => Object.keys(rowData)[0] ? Object.keys(rowData)[0] : <p style={{color: 'grey'}}>add new key</p>} 
          editor={this.keyEditor} 
          />
        <Column 
          columnKey='value' 
          field='value' 
          header='Attribute Value'
          body={rowData => rowData[Object.keys(rowData)[0]] ?rowData[Object.keys(rowData)[0]] : <p style={{color: 'grey'}}>add new value</p>} 
          editor={this.valueEditor} 
          />
        <Column 
          columnKey='buttonRow'
          body={rowData => <Button icon='pi pi-trash' onClick={() => this.props.onDeleteAttribute(rowData)}/>}
          style={{width: '4em'}}
        />
      </DataTable>
    )
  }
}

const objectToArray= (obj) => {
  if(!obj){return []}
  let res = [];
  Object.keys(obj).forEach((key) => {
    res.push({ [key]: obj[key] });
  })
  return res;
}

const mapDispatchToProps = {
  showWarningMessage: notificationActions.showWarningMessage,
}

export default connect(null, mapDispatchToProps)(EditAttributes)