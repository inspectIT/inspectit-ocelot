import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { agentConfigActions } from '../../../redux/ducks/agent-config';

import {DataTable} from 'primereact/datatable';
import {Column} from 'primereact/column';
import {Button} from 'primereact/button';

import {isEqual} from 'lodash';
import { notificationActions } from '../../../redux/ducks/notification';

const ButtonCell = ({mapping, onEdit, onDelete, onDownload, saved, showInfo}) => {
  return(
    <div>
      <Button 
        icon='pi pi-pencil' 
        onClick={() => {onEdit(mapping)}}
      />
      <Button 
        icon='pi pi-trash' 
        onClick={() => {onDelete(mapping)}}
        style={{marginTop: "0.25em"}}
      />
      <Button 
        icon='pi pi-download' 
        onClick={() => {
          if(!saved()){
            showInfo('Unsaved changes', 'The downloaded configuration file is based on unsaved changes and might not be as expected')
          }
          onDownload(mapping.attributes, mapping.name)}
        } 
        tooltip="Click here to download the configuration file for this mapping" 
        style={{marginTop: "0.25em"}}
      />
    </div>
  )
}

const SourceCell = ({sources}) => {
  if(!sources) {return}
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        sources.length <= 5 ? 
        sources.map(source => ( <p>{source}</p> )) :
        <div>
          <p>{sources[0]}</p><p>{sources[1]}</p>
          <p>{sources[2]}</p><p>{sources[3]}</p>
          <p>{sources[4]}</p><p>...</p>
        </div>
      }
    </div>
  )
}

const AttributesCell = ({attributes}) => {
if(!attributes) {return}

const keys = Object.keys(attributes);
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        keys && keys.length <= 5 ?
        keys.map(key => ( <p>{`${key}: ${attributes[key]}`}</p> )) :
        <div>
          <p>{`${keys[0]}: ${attributes[keys[0]]}`}</p>
          <p>{`${keys[1]}: ${attributes[keys[1]]}`}</p>
          <p>{`${keys[2]}: ${attributes[keys[2]]}`}</p>
          <p>{`${keys[3]}: ${attributes[keys[3]]}`}</p>
          <p>{`${keys[4]}: ${attributes[keys[4]]}`}</p>
          <p>...</p>
        </div>
      }
    </div>
  )
}

class MappingsTable extends React.Component{

  render() {
    const filteredMappings = this.filterMappings(this.props.editableMappings);
    return(
      <DataTable value={filteredMappings} reorderableRows={true} onRowReorder={(e) => {this.props.onUpdateEditMappings(e.value)}} autoLayout={true} >
        <Column rowReorder={true} style={{width: '3em'}} />
        <Column columnKey="name" field="name" header="Name"/>
        <Column columnKey="sources" field="sources" body={(data) => (<SourceCell sources={data.sources}/>)} header="Source" />
        <Column columnKey="attributes" field="attributes" body={(data) => (<AttributesCell attributes={data.attributes} />)} header="Attributes" />
        <Column columnKey="buttons" field="" body={(data) => (<ButtonCell mapping={data} onEdit={this.props.onEditMapping} onDelete={this.props.onDeleteMapping} onDownload={this.props.downloadConfigFile} saved={this.areMappingsChanged} showInfo={this.props.showInfoMessage} />)} header="" style={{width: '3em'}} />
      </DataTable>
    )
  }

  componentDidMount = () => this.props.fetchMappings();

  componentDidUpdate = () => this.props.onMappingsChanged(this.areMappingsChanged());

  areMappingsChanged = () => {
    return isEqual(this.props.editableMappings, this.props.mappings)
  }

  filterMappings = (mappings) => {
    const {filterValue} = this.props;
    let res = [];

    mappings.forEach(element => {
      if (element.head) {
        res.push(element);
     } else if (element.name && element.name.includes(filterValue)) {
        res.push(element);
      } else if (element.sources) {
        let pushed = false;
        for(let i = 0; i < element.sources.length; i++){
          if(element.sources[i].includes(filterValue)) {
            res.push(element);
            pushed = true;
            break;
          }
        }
        if(!pushed){
          let keys = Object.keys(element.attributes);
          for (let i = 0; i < keys.length; i++) {
            const attribute = `${keys[i]}: ${element.attributes[keys[i]]}`;
            if(attribute.includes(filterValue)) {
              res.push(element);
              pushed = true;
              break;
            }
          }
        }
      }
    })
    return res;
  }
}

function mapStateToProps(state) {
  const {editableMappings, mappings} = state.mappings;
  return {
    editableMappings,
    mappings
  }
}

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
  onUpdateEditMappings: mappingsActions.replaceEditableMappings,
  downloadConfigFile: agentConfigActions.fetchConfigurationFile,
  showInfoMessage: notificationActions.showInfoMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(MappingsTable)