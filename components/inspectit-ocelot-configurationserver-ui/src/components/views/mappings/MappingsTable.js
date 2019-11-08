import React from 'react';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { TieredMenu } from 'primereact/tieredmenu';
import { OverlayPanel } from 'primereact/overlaypanel';
import DeleteDialog from './dialogs/DeleteDialog';
import ConfigurationDownload from './ConfigurationDownload';

/** Component including the menu button for each mapping */
const ButtonCell = ({ mapping, onEdit, onDelete, onDownload, appendRef }) => {
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
        onDelete(mapping.name)
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
  return (
    <div ref={el => thisCell.div = el}>
      <TieredMenu model={menuItems} popup={true} appendTo={appendRef} ref={el => thisCell.menu = el} />
      <Button icon="pi pi-bars" onClick={(e) => thisCell.menu.toggle(e)} />
    </div>
  )
}

/** Component to display sources & it's OverlayPanel */
const SourceCell = ({ sources = [], appendRef }) => {
  const thisCell = {};
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        sources.length <= 5 ?
          sources.map(source => (<p>{source}</p>)) :
          <div
            onMouseEnter={e => thisCell.op.show(e)}
            onMouseLeave={e => thisCell.op.hide(e)}
          >
            <p>{sources[0]}</p><p>{sources[1]}</p>
            <p>{sources[2]}</p><p>{sources[3]}</p>
            <p>{sources[4]}</p><p>...</p>
            <OverlayPanel appendTo={appendRef} ref={(el) => thisCell.op = el}>
              {sources.map(source => (<p>{source}</p>))}
            </OverlayPanel>
          </div>
      }
    </div>
  )
}

/** Component to display attributes & it's OverlayPanel */
const AttributesCell = ({ attributes = {}, appendRef }) => {
  const thisCell = {};
  const keys = Object.keys(attributes);
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {
        keys && keys.length <= 5 ?
          keys.map(key => (<p>{`${key}: ${attributes[key]}`}</p>)) :
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
            <OverlayPanel appendTo={appendRef} ref={(el) => thisCell.op = el}>
              {keys.map(key => (<p>{`${key}: ${attributes[key]}`}</p>))}
            </OverlayPanel>
          </div>
      }
    </div>
  )
}

class MappingsTable extends React.Component {
  configDownload = React.createRef();
  state = {};

  render() {
    const { filterValue, maxHeight, mappings, putMappings } = this.props;

    const mappingValues = mappings.map((mapping) => {
      //build a dummy string to allow filtering
      const attributesSearchString =
        Object.keys(mapping.attributes).sort().map((key) => {
          return key + ": " + mapping.attributes[key] + "\n"
        });
      return {
        ...mapping,
        attributesSearchString
      }
    })

    return (
      /** primereact/overlaypanel && primereact/tieredMenu must be appended to s.th. 'higher' than the cells components to avoid strange behavoir */
      < div ref={el => this.mappingsTable = el} >
        <DataTable
          value={mappingValues}
          reorderableRows={true} scrollable={true}
          scrollHeight={maxHeight ? maxHeight : '100%'}
          onRowReorder={(e) => { putMappings(e.value) }}
          globalFilter={filterValue}
        >
          <Column rowReorder={!filterValue} style={{ width: '3em' }} />
          <Column columnKey="name" field="name" header="Mapping Name" />
          <Column
            columnKey="sources"
            field="sources"
            body={(data) => (<SourceCell sources={data.sources} appendRef={this.mappingsTable} />)}
            header="Sources"
          />
          <Column
            columnKey="attributes"
            field="attributesSearchString"
            body={(data) => (<AttributesCell attributes={data.attributes} appendRef={this.mappingsTable} />)}
            header="Attributes" />
          <Column
            columnKey="buttons"
            body={(data) => (
              <ButtonCell
                mapping={data}
                onEdit={this.props.onEditMapping}
                onDelete={this.showDeleteMappingDialog}
                onDownload={this.configDownload.download}
                appendRef={this.mappingsTable}
              />)}
            style={{ width: '4em' }}
          />
        </DataTable>
        <DeleteDialog
          visible={this.state.isDeleteDialogShown}
          onHide={this.hideDeleteMappingDialog}
          mappingName={this.state.selectedMappingName}
        />
        {/** reference is used for calling onDownload within ButtonCell component */}
        <ConfigurationDownload onRef={ref => this.configDownload = ref} />
      </div >
    )
  }

  componentDidMount = () => {
    this.props.fetchMappings();
  };

  showDeleteMappingDialog = (selectedMappingName) => this.setState({ isDeleteDialogShown: true, selectedMappingName: selectedMappingName });
  hideDeleteMappingDialog = () => this.setState({ isDeleteDialogShown: false, selectedMappingName: null });
}

function mapStateToProps(state) {
  const { mappings } = state.mappings;
  return {
    mappings
  }
}

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
  putMappings: mappingsActions.putMappings,
}

export default connect(mapStateToProps, mapDispatchToProps)(MappingsTable)