import React from 'react'
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

const buttonStyle = {'margin-left': '0.25em' }

class EditAttributes extends React.Component{

  keyInput = React.createRef();
  valueInput = React.createRef();

  constructor(props){
    super(props)
    this.state = {
      newAttKey: '',
      newAttValue: '',
      selectedAttributes: [],
      changedAttKey: null,
      changedAttValue: null
    }
  }

  /** editor functions will be called instead of body functions during edit mode of DataTable*/
  nameEditor = (columnProps) => {
    if(this.state.selectedAttributes.length !== 0) {return Object.keys(columnProps.rowData)[0]}
    return ( 
      <InputText 
        value={this.state.changedAttKey == null ? Object.keys(columnProps.rowData)[0]: this.state.changedAttKey} 
        onChange={e => this.onAttrKeyChanged(e, columnProps.rowIndex)} 
        onBlur={e => this.onAttrKeyFieldBlur(e.target.value, columnProps)}
        onKeyDown={e => {if(e.key === 'Enter' || e.key === 'Tab'){this.onAttrKeyFieldBlur(e.target.value, columnProps)} }}
      /> 
    )
  }

  valueEditor = (columnProps) => {
    if(this.state.selectedAttributes.length !== 0) {return columnProps.rowData[Object.keys(columnProps.rowData)[0]]}
    return ( 
      <InputText 
        value={this.state.changedAttValue == null ? columnProps.rowData[Object.keys(columnProps.rowData)[0]]: this.state.changedAttValue} 
        onChange={e => this.setState({changedAttValue: e.target.value})} 
        onBlur={e => this.onAttValueFieldBlur(e.target.value, columnProps.rowData)}
        onKeyDown={e =>{if(e.key === 'Enter' || e.key === 'Tab'){this.onAttValueFieldBlur(e.target.value, columnProps.rowData)} }}
      /> 
    )
  }

  onAttrKeyChanged = (e, rowIndex) => {
    const {value, style} = e.target;
    const allAttKeys = Object.keys(this.props.attributes);
    this.setState({changedAttKey: value});

    if(allAttKeys.includes(value) && allAttKeys[rowIndex] !== value){
      style.color = "red";
    } else {
      style.color = "black";
    }
  }

  onAttrKeyFieldBlur = (newKey, columnProps) => {
    const allAttKeys = Object.keys(this.props.attributes);

    if(newKey !== '' && !allAttKeys.includes(newKey)){
      const oldKey = Object.keys(columnProps.rowData)[0];
      this.props.onChangeAttributeKey(oldKey, newKey);
    } 
    else if (allAttKeys[columnProps.rowIndex] !== newKey) {
      this.props.showWarningMessage('Attribute has not been changed', !newKey ? 'The attribute key cannot be empty' : 'This mapping already has an attribute with the given key');
    }

    this.setState({changedAttKey: null});
  }

  onAttValueFieldBlur = (newValue, rowData) => {
    const key = Object.keys(rowData)[0];

    if(newValue !== '') {
      this.props.onChValueOrAddAttribute(key, newValue);
    } else {
      this.props.showWarningMessage('Attribute has not been changed', 'The attribute value cannot be empty');
    }

    this.setState({changedAttValue: null});
  }

  onAddNewAttribute = () => {
    const {newAttKey, newAttValue} = this.state;

    if(!this.props.attributes || !Object.keys(this.props.attributes).includes(newAttKey)){
      this.props.onChValueOrAddAttribute(newAttKey, newAttValue);
      this.setState({newAttKey: '', newAttValue: ''});
    } else {
      this.props.showWarningMessage('Attribute has not been added', 'This mapping already has an attribute with the given key');
    }
  }

  render() {
    const {newAttKey, newAttValue, selectedAttributes} = this.state;
    let attributesArray = objectToArray(this.props.attributes);
    return(
      <div className='this'>
        <style jsx>{`
          .this :global(div .p-datatable.p-component.p-datatable-scrollable){
            border: none;
            margin: 0;
            background: red;
          }
          .this :global(.p-datatable table thead){
            display: none;
          }
          .head{
            display: flex;
            align-items: center;
            border-bottom: 2px solid #ddd;
            height: 3rem;
            padding-left: 0.5em;
          }
        `}</style>
        <div className='head'>
        <InputText
          ref={this.keyInput} 
          value={newAttKey} 
          placeholder='Attribute Key'
          onChange={e => this.setState({newAttKey: e.target.value})} 
          onKeyPress={e => {if(e.key === 'Enter'){this.valueInput.current.element.focus()}}}
          style={{marginRight: "0.2em"}}
        />
          {`:`}
          <InputText 
            ref={this.valueInput}
            value={newAttValue}
            placeholder='Attribute Value'
            onChange={e => this.setState({newAttValue: e.target.value})}
            onKeyPress={e => {if(e.key === 'Enter'){this.onAddNewAttribute(); this.keyInput.current.element.focus()}}}
            style={{marginLeft: "0.2em"}}
          />
          <Button 
            icon='pi pi-plus' 
            disabled={!newAttKey || !newAttValue} 
            onClick={this.onAddNewAttribute} 
            style={buttonStyle}
          />
          <Button 
            icon='pi pi-trash'
            disabled={selectedAttributes.length === 0}
            onClick={() => { this.props.onDeleteAttribute(selectedAttributes); this.setState({selectedAttributes: []}) }}
            style={buttonStyle}
          />
        </div>
        <DataTable 
          value={attributesArray} 
          scrollable={true} 
          scrollHeight={`${this.props.height}px`} 
          editable={false}
          selection={selectedAttributes} 
          onSelectionChange={e => this.setState({selectedAttributes: e.value})} 
        >
          <Column selectionMode="multiple" style={{width:'3em'}}/>
          <Column 
            columnKey='name' 
            field='name' 
            body={rowData => Object.keys(rowData)[0]} 
            editor={this.nameEditor} 
            />
          <Column 
            columnKey='value' 
            field='value' 
            body={rowData => rowData[Object.keys(rowData)[0]]} 
            editor={this.valueEditor} 
            />
        </DataTable>
      </div>
    )
  }
}

const objectToArray= (obj) => {
  if(!obj){return}
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