import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { agentConfigActions } from '../../../redux/ducks/agent-config';

import {DataTable} from 'primereact/datatable';
import {Column} from 'primereact/column';
import {Button} from 'primereact/button';
import {TieredMenu} from 'primereact/tieredmenu';
import {OverlayPanel} from 'primereact/overlaypanel';

import { notificationActions } from '../../../redux/ducks/notification';

const ButtonCell = ({mapping, onEdit, onDelete, onDownload, saved, showInfo}) => {
  const this_cell = {}
  const domElement_body = document.getElementsByTagName("BODY")[0]; 
  const menuItems = [
    {
      label: 'Edit',
      icon: 'pi pi-fw pi-pencil',
      command: (e) => {
        this_cell.menu.toggle(e)
        onEdit(mapping)
      },
    },
    {
      label: 'Delete',
      icon: 'pi pi-fw pi-trash',
      command: (e) => {
        this_cell.menu.toggle(e)
        onDelete(mapping)
      },
    },
    {
      label: 'Configuration',
      icon: 'pi pi-fw pi-download',
      command: (e) => {
        this_cell.menu.toggle(e)
        if(!saved()){
          showInfo('Unsaved changes', 'The downloaded configuration file is based on unsaved changes and might not be as expected')
        }
        onDownload(mapping.attributes)
      }
    }
  ]
  return(
    <div>
    <TieredMenu model={menuItems} popup={true} appendTo={domElement_body} ref={el => this_cell.menu = el} />
    <Button icon="pi pi-bars" onClick={(event) => this_cell.menu.toggle(event)}/>
  </div>
  )
}

const SourceCell = ({sources}) => {
  if(!sources) {return null}

  const this_cell = {}
  const domElement_body = document.getElementsByTagName("BODY")[0]; 

  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        sources.length <= 5 ? 
        sources.map(source => ( <p>{source}</p> )) :
        <div 
          onMouseEnter={e => this_cell.op.show(e)} 
          onMouseLeave={e => this_cell.op.hide(e)}
        >
          <p>{sources[0]}</p><p>{sources[1]}</p>
          <p>{sources[2]}</p><p>{sources[3]}</p>
          <p>{sources[4]}</p><p>...</p>
          <OverlayPanel appendTo={domElement_body} ref={(el) => this_cell.op = el}>
            {sources.map(source => ( <p>{source}</p> ))}
          </OverlayPanel>
        </div>
      }
    </div>
  )
}

const AttributesCell = ({attributes}) => {
if(!attributes) {return null}

const this_cell = {}
const domElement_body = document.getElementsByTagName("BODY")[0]; 

const keys = Object.keys(attributes);
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        keys && keys.length <= 5 ?
        keys.map(key => ( <p>{`${key}: ${attributes[key]}`}</p> )) :
        <div 
          onMouseEnter={e => this_cell.op.show(e)} 
          onMouseLeave={e => this_cell.op.hide(e)}
        >
          <p>{`${keys[0]}: ${attributes[keys[0]]}`}</p>
          <p>{`${keys[1]}: ${attributes[keys[1]]}`}</p>
          <p>{`${keys[2]}: ${attributes[keys[2]]}`}</p>
          <p>{`${keys[3]}: ${attributes[keys[3]]}`}</p>
          <p>{`${keys[4]}: ${attributes[keys[4]]}`}</p>
          <p>...</p>
          <OverlayPanel appendTo={domElement_body} ref={(el) => this_cell.op = el}>
            {keys.map(key => ( <p>{`${key}: ${attributes[key]}`}</p> ))}
          </OverlayPanel>
        </div>
      }
    </div>
  )
}

class MappingsTable extends React.Component{

  render() {
    const filteredMappings = this.filterMappings(this.props.mappings);
    return(
      <DataTable value={filteredMappings} reorderableRows={true} onRowReorder={(e) => {this.props.putMappings(e.value)}} autoLayout={true} >
        <Column rowReorder={true} style={{width: '3em'}} />
        <Column columnKey="name" field="name" header="Name"/>
        <Column columnKey="sources" field="sources" body={(data) => (<SourceCell sources={data.sources}/>)} header="Source" />
        <Column columnKey="attributes" field="attributes" body={(data) => (<AttributesCell attributes={data.attributes} />)} header="Attributes" />
        <Column columnKey="buttons" field="" body={(data) => (<ButtonCell mapping={data} onEdit={this.props.onEditMapping} onDelete={this.props.onDeleteMapping} onDownload={this.props.downloadConfigFile} saved={this.areMappingsChanged} showInfo={this.props.showInfoMessage} />)} header="" style={{width: '3em'}} />
      </DataTable>
    )
  }

  componentDidMount = () => this.props.fetchMappings();

  filterMappings = (mappings) => {
    let {filterValue} = this.props;
    filterValue = filterValue.toLowerCase()
    let res = [];

    mappings.forEach(element => {
      if (element.head) {
        res.push(element);
     } else if (element.name && element.name.toLowerCase().includes(filterValue)) {
        res.push(element);
      } else if (element.sources) {
        let pushed = false;
        for(let i = 0; i < element.sources.length; i++){
          if(element.sources[i].toLowerCase().includes(filterValue)) {
            res.push(element);
            pushed = true;
            break;
          }
        }
        if(!pushed){
          let keys = Object.keys(element.attributes);
          for (let i = 0; i < keys.length; i++) {
            const attribute = `${keys[i]}: ${element.attributes[keys[i]]}`;
            if(attribute.toLowerCase().includes(filterValue)) {
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
  const {mappings} = state.mappings;
  return {
    mappings
  }
}

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
  putMappings: mappingsActions.putMappings,
  downloadConfigFile: agentConfigActions.fetchConfigurationFile,
  showInfoMessage: notificationActions.showInfoMessage
}

export default connect(mapStateToProps, mapDispatchToProps)(MappingsTable)