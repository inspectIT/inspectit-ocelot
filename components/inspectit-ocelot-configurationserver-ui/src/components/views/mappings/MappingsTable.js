import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';
import { agentConfigActions } from '../../../redux/ducks/agent-config';
import { configurationActions } from '../../../redux/ducks/configuration';

import {DataTable} from 'primereact/datatable';
import {Column} from 'primereact/column';
import {Button} from 'primereact/button';
import {TieredMenu} from 'primereact/tieredmenu';
import {OverlayPanel} from 'primereact/overlaypanel';
import DeleteDialog from './dialogs/DeleteDialog';

const ButtonCell = ({mapping, onEdit, onDelete, onDownload, apenndRef}) => {
  const thisCell = {};
  const menuItems = [
    {
      label: 'Edit',
      icon: 'pi pi-fw pi-pencil',
      command: (e) => {
        thisCell.menu.toggle(e)
        onEdit(mapping)
      },
    },
    {
      label: 'Delete',
      icon: 'pi pi-fw pi-trash',
      command: (e) => {
        thisCell.menu.toggle(e)
        onDelete(mapping)
      },
    },
    {
      label: 'Configuration',
      icon: 'pi pi-fw pi-download',
      command: (e) => {
        thisCell.menu.toggle(e)
        onDownload(mapping.attributes)
      }
    }
  ]
  return(
    <div ref={el => thisCell.div = el}> 
    <TieredMenu model={menuItems} popup={true} appendTo={apenndRef} ref={el => thisCell.menu = el} />
    <Button icon="pi pi-bars" onClick={(event) => thisCell.menu.toggle(event)} style={{'margin-right': '5.25m'}}/>
  </div>
  )
}

const SourceCell = ({sources, apenndRef}) => {
  if(!sources) {
    return null;
  }
  const thisCell = {};

  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        sources.length <= 5 ? 
        sources.map(source => ( <p>{source}</p> )) :
        <div 
          onMouseEnter={e => thisCell.op.show(e)} 
          onMouseLeave={e => thisCell.op.hide(e)}
        >
          <p>{sources[0]}</p><p>{sources[1]}</p>
          <p>{sources[2]}</p><p>{sources[3]}</p>
          <p>{sources[4]}</p><p>...</p>
          <OverlayPanel appendTo={apenndRef} ref={(el) => thisCell.op = el}>
            {sources.map(source => ( <p>{source}</p> ))}
          </OverlayPanel>
        </div>
      }
    </div>
  )
}

const AttributesCell = ({attributes, apenndRef}) => {
if(!attributes) {
  return null;
}
const thisCell = {};

const keys = Object.keys(attributes);
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        keys && keys.length <= 5 ?
        keys.map(key => ( <p>{`${key}: ${attributes[key]}`}</p> )) :
        <div 
          onMouseEnter={e => thisCell.op.show(e)} 
          onMouseLeave={e => thisCell.op.hide(e)}
        >
          <p>{`${keys[0]}: ${attributes[keys[0]]}`}</p>
          <p>{`${keys[1]}: ${attributes[keys[1]]}`}</p>
          <p>{`${keys[2]}: ${attributes[keys[2]]}`}</p>
          <p>{`${keys[3]}: ${attributes[keys[3]]}`}</p>
          <p>{`${keys[4]}: ${attributes[keys[4]]}`}</p>
          <p>...</p>
          <OverlayPanel appendTo={apenndRef} ref={(el) => thisCell.op = el}>
            {keys.map(key => ( <p>{`${key}: ${attributes[key]}`}</p> ))}
          </OverlayPanel>
        </div>
      }
    </div>
  )
}

class MappingsTable extends React.Component{
  constructor(props){
    super(props);
    this.state = {};
  }

  render() {
    const filteredMappings = this.props.mappings.filter(mapping => !this.isMappingHidden(mapping));

    return(
      <div ref={el => this.mappingsTable = el}>
        <DataTable value={filteredMappings} reorderableRows={true} scrollable={true} scrollHeight={this.props.maxHeight ? this.props.maxHeight : '100%'} onRowReorder={(e) => {this.props.putMappings(e.value)}} globalFilter={this.state.globalFilter} >
          <Column rowReorder={!this.props.filterValue} style={{width: '3em'}} />
          <Column columnKey="name" field="name" header="Mapping Name"/>
          <Column columnKey="sources" field="sources" body={(data) => (<SourceCell sources={data.sources} apenndRef={this.mappingsTable}/>)} header="Sources" />
          <Column columnKey="attributes" field="attributes" body={(data) => (<AttributesCell attributes={data.attributes} apenndRef={this.mappingsTable}/>)} header="Attributes" />
          <Column columnKey="buttons" field="" body={(data) => (<ButtonCell mapping={data} onEdit={this.props.onEditMapping} onDelete={this.showDeleteMappingDialog} onDownload={this.props.downloadConfigFile} apenndRef={this.mappingsTable}/>)} style={{width: '4em'}} />
        </DataTable>
        <DeleteDialog 
          visible={this.state.isDeleteDialogShown} 
          onHide={this.hideDeleteMappingDialog} 
          mapping={this.state.selectedMapping} 
        />
      </div>
    )
  }

  componentDidMount = () => {
    this.props.fetchMappings();
  };

  showDeleteMappingDialog = (selectedMapping) => this.setState({isDeleteDialogShown: true, selectedMapping: selectedMapping});
  hideDeleteMappingDialog = () => this.setState({isDeleteDialogShown: false, selectedMapping: {}});

  /**
   * returns true when mapping is no longer included due to filterValue
   * 
   * @param {*} mapping 
   */
  isMappingHidden(mapping){
    let {filterValue} = this.props;
    filterValue = filterValue.toLowerCase();

    if(mapping.name && mapping.name.toLowerCase().includes(filterValue)){
      return false;
    } else {
      if(mapping.sources){
        for(let i = 0; i < mapping.sources.length; i++){
          if(mapping.sources[i].toLowerCase().includes(filterValue)){
            return false;
          }
        }
      }

      if(mapping.attributes){
        for (let key in mapping.attributes) {
          const attributeString = `${key}: ${mapping.attributes[key]}`;
          if(attributeString.toLowerCase().includes(filterValue)) {
            return false;
          }
        }
      }

    }
    return true;
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
  fetchFiles: configurationActions.fetchFiles,
}

export default connect(mapStateToProps, mapDispatchToProps)(MappingsTable)