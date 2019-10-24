import React from 'react'
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

import { isNull } from 'lodash';

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
        onBlur={e => this.handleBlur(e.target.value, null, columnProps)}
        onKeyDown={e => {
          if(e.key === 'Enter' || e.key === 'Tab'){
            this.handleBlur(e.target.value, null, columnProps)}
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
        onBlur={e => this.handleBlur(null, e.target.value, columnProps)}
        onKeyDown={e =>{
          if(e.key === 'Enter' || e.key === 'Tab'){
            this.handleBlur(null, e.target.value, columnProps)
          } 
        }}
      /> 
    )
  }

  handleBlur = (attKey, attValue, columnProps) => {
    const allAttKeys = Object.keys(this.props.attributes);
    const oldKey = Object.keys(columnProps.rowData)[0];

    if(
      ( !isNull(attKey) && (allAttKeys.includes(attKey) && allAttKeys[columnProps.rowIndex] !== attKey) )
      || ( !isNull(attKey) && columnProps.rowData[attKey] || !isNull(attValue) && attValue === columnProps.rowData[oldKey] )
      || ( oldKey == null && !attKey && !attValue )
      || ( isNull(attKey) && oldKey == null && allAttKeys.includes('') )
    ) {
      // attribute has not been changed in a way there should be a callback
    } else {
      this.props.onAttributeChange(oldKey, attKey, attValue)
    }

    this.setState({attKey: null, attValue: null});
  }

  render() {
    let attributesArray = objectToArray(this.props.attributes);
    attributesArray.push({})

    return(
      <DataTable 
        value={attributesArray} 
        scrollable={true} 
        scrollHeight={this.props.maxHeight ? this.props.maxHeight : '100%'}
      >
        <Column 
          columnKey='key' 
          field='key' 
          header='Attribute Key'
          headerStyle={{'font-weight': 'normal'}}
          body={rowData => Object.keys(rowData)[0] ? Object.keys(rowData)[0] : <p style={{color: 'grey'}}>click here to add new key</p>} 
          editor={this.keyEditor} 
          />
        <Column 
          columnKey='value' 
          field='value' 
          header='Attribute Value'
          headerStyle={{'font-weight': 'normal'}}
          body={rowData => rowData[Object.keys(rowData)[0]] ? rowData[Object.keys(rowData)[0]] : <p style={{color: 'grey'}}>click here to add new value</p>} 
          editor={this.valueEditor} 
          />
        <Column 
          columnKey='buttonRow'
          body={rowData => Object.keys(rowData)[0] != null ? <Button icon='pi pi-trash' onClick={() => this.props.onDeleteAttribute(rowData)}/> : ''}
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