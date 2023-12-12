import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { mappingsActions } from '../../../redux/ducks/mappings';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { TieredMenu } from 'primereact/tieredmenu';
import { OverlayPanel } from 'primereact/overlaypanel';
import DeleteDialog from './dialogs/DeleteDialog';
import ConfigurationDownload from './ConfigurationDownload';
import { cloneDeep, find } from 'lodash';
import classnames from 'classnames';

/** Component including the menu button for each mapping */
const ButtonCell = ({ readOnly, mapping, onEdit, onDelete, onDownload, onDuplicate, appendRef }) => {
  const thisCell = {};

  const menuItems = [];

  if (!readOnly) {
    menuItems.push({
      label: 'Edit',
      icon: 'pi pi-fw pi-pencil',
      command: (e) => {
        thisCell.menu.toggle(e);
        onEdit(mapping);
      },
    });
  }

  menuItems.push({
    label: 'Download Configuration',
    icon: 'pi pi-fw pi-download',
    command: (e) => {
      thisCell.menu.toggle(e);
      onDownload(mapping.attributes);
    },
  });

  if (!readOnly) {
    menuItems.push({
      label: 'Duplicate',
      icon: 'pi pi-fw pi-clone',
      command: (e) => {
        thisCell.menu.toggle(e);
        onDuplicate(mapping);
      },
    });

    menuItems.push({
      separator: true,
    });

    menuItems.push({
      label: 'Delete',
      icon: 'pi pi-fw pi-trash',
      command: (e) => {
        thisCell.menu.toggle(e);
        onDelete(mapping.name);
      },
    });
  }

  return (
    <div ref={(el) => (thisCell.div = el)} className="this">
      <style jsx>{`
        :global(.mappings-table.p-tieredmenu) {
          width: 16rem;
        }
      `}</style>
      <TieredMenu className="mappings-table" model={menuItems} popup={true} appendTo={appendRef} ref={(el) => (thisCell.menu = el)} />
      <Button icon="pi pi-bars" onClick={(e) => thisCell.menu.toggle(e)} />
    </div>
  );
};

/** Component to display sources & it's OverlayPanel */
const SourceCell = ({ sources = [], appendRef }) => {
  const thisCell = {};
  return (
    <div>
      <style>{` p{ margin: 0.2em; } `}</style>
      {sources.length <= 5 ? (
        sources.map((source) => <p key={source}>{source}</p>)
      ) : (
        <div onMouseEnter={(e) => thisCell.op.show(e)} onMouseLeave={(e) => thisCell.op.hide(e)}>
          <p>{sources[0]}</p>
          <p>{sources[1]}</p>
          <p>{sources[2]}</p>
          <p>{sources[3]}</p>
          <p>{sources[4]}</p>
          <p>...</p>
          <OverlayPanel appendTo={appendRef} ref={(el) => (thisCell.op = el)}>
            {sources.map((source) => (
              <p key={source}>{source}</p>
            ))}
          </OverlayPanel>
        </div>
      )}
    </div>
  );
};

/** Component to display attributes & it's OverlayPanel */
const AttributesCell = ({ attributes = {}, appendRef }) => {
  const thisCell = {};
  const keys = Object.keys(attributes);
  return (
    <div>
      <style>{` p{ margin: 0.2em; text-align: left; } `}</style>
      {keys && keys.length <= 5 ? (
        keys.map((key) => <p key={key}>{`${key}: ${attributes[key]}`}</p>)
      ) : (
        <div onMouseEnter={(e) => thisCell.op.show(e)} onMouseLeave={(e) => thisCell.op.hide(e)}>
          <p>{`${keys[0]}: ${attributes[keys[0]]}`}</p>
          <p>{`${keys[1]}: ${attributes[keys[1]]}`}</p>
          <p>{`${keys[2]}: ${attributes[keys[2]]}`}</p>
          <p>{`${keys[3]}: ${attributes[keys[3]]}`}</p>
          <p>{`${keys[4]}: ${attributes[keys[4]]}`}</p>
          <p>...</p>
          <OverlayPanel appendTo={appendRef} ref={(el) => (thisCell.op = el)}>
            {keys.map((key) => (
              <p key={key}>{`${key}: ${attributes[key]}`}</p>
            ))}
          </OverlayPanel>
        </div>
      )}
    </div>
  );
};

const BranchCell = ({ branch }) => {
  const isLiveBranch = branch === 'LIVE';
  const branchName = isLiveBranch ? 'Live' : 'Workspace';
  const iconClass = classnames('pi', {
    'pi-circle-on': isLiveBranch,
    'pi-circle-off': !isLiveBranch,
    live: isLiveBranch,
    workspace: !isLiveBranch,
  });

  return (
    <>
      <style jsx>{`
        .this {
          display: flex;
          align-items: center;
        }
        .pi {
          margin-right: 0.5rem;
        }
        .pi.live {
          color: #ef5350;
        }
        .pi.workspace {
          color: #616161;
        }
      `}</style>

      <div className="this">
        <i className={iconClass}></i>
        <span>{branchName}</span>
      </div>
    </>
  );
};

class MappingsTable extends React.Component {
  configDownload = React.createRef();
  state = {};

  componentDidMount = () => {
    this.props.fetchMappings();
  };

  duplicateMapping = (mapping) => {
    const newMapping = {
      ...cloneDeep(mapping),
      name: this.getUniqueName(mapping.name),
      isNew: true,
    };

    this.props.onDuplicateMapping(newMapping);
  };

  getUniqueName = (nameBase) => {
    const { mappings } = this.props;

    let name;
    let mappingExists = true;
    let i = 1;
    while (mappingExists) {
      name = nameBase + ' (' + i++ + ')';
      mappingExists = find(mappings, { name: name });
    }

    return name;
  };

  showDeleteMappingDialog = (selectedMappingName) => {
    this.setState({ isDeleteDialogShown: true, selectedMappingName: selectedMappingName });
  };
  hideDeleteMappingDialog = () => this.setState({ isDeleteDialogShown: false, selectedMappingName: null });

  render() {
    const { readOnly, filterValue, maxHeight, mappings, putMappings, sidebar } = this.props;

    const mappingValues = mappings.map((mapping) => {
      //build a dummy string to allow filtering
      const attributesSearchString = Object.keys(mapping.attributes)
        .sort()
        .map((key) => {
          return key + ': ' + mapping.attributes[key] + '\n';
        });
      return {
        ...mapping,
        attributesSearchString,
      };
    });

    return (
      /** primereact/overlaypanel && primereact/tieredMenu must be appended to s.th. 'higher' than the cells components to avoid strange behavoir */
      <>
        <style global jsx>{`
          .p-datatable-scrollable .p-datatable-thead > tr > th,
          .p-datatable-scrollable .p-datatable-tbody > tr > td,
          .p-datatable-scrollable .p-datatable-tfoot > tr > td {
            flex: unset;
            display: flex;
            text-align: center;
          }
          .cell-text {
            display: inline-block;
            white-space: normal;
            overflow: visible;
            overflow-wrap: anywhere;
          }
          .table-and-sidebar {
            display: flex;
            flex: 1 1 auto;
            overflow: auto;
            position: inherit;
          }
          .table {
            flex: 1;
          }
          .versioning-sidebar {
            flex: 0;
          }
          .drag-icon-col {
            width: 10rem;
          }
          .mapping-name-col {
            width: -moz-available;
            width: -webkit-fill-available;
            width: fill-available;
          }
          .source-branch-col {
            width: 40rem;
          }
          .sources-col {
            width: -moz-available;
            width: -webkit-fill-available;
            width: fill-available;
          }
          .attributes-col {
            width: -moz-available;
            width: -webkit-fill-available;
            width: fill-available;
          }
          .buttons-col {
            width: 15rem;
          }
        `}</style>
        <div className="table-and-sidebar">
          <div className="table" ref={(el) => (this.mappingsTable = el)}>
            <DataTable
              value={mappingValues}
              reorderableRows={!readOnly}
              scrollable={true}
              scrollHeight={maxHeight ? maxHeight : '100%'}
              onRowReorder={(e) => {
                putMappings(e.value);
              }}
              globalFilter={filterValue}
            >
              {!readOnly && <Column className="drag-icon-col" rowReorder={!filterValue} />}
              <Column className="mapping-name-col cell-text" columnKey="name" field="name" header="Mapping Name" />
              <Column
                className="source-branch-col cell-text"
                columnKey="branch"
                field="branch"
                header="Source Branch"
                body={(data) => <BranchCell branch={data.sourceBranch} />}
              />
              <Column
                className="sources-col cell-text"
                columnKey="sources"
                field="sources"
                body={(data) => <SourceCell sources={data.sources} appendRef={this.mappingsTable} />}
                header="Sources"
              />
              <Column
                className="attributes-col cell-text"
                columnKey="attributes"
                field="attributesSearchString"
                body={(data) => <AttributesCell attributes={data.attributes} appendRef={this.mappingsTable} />}
                header="Attributes"
              />
              <Column
                className="buttons-col cell-text"
                columnKey="buttons"
                body={(data) => (
                  <ButtonCell
                    readOnly={readOnly}
                    mapping={data}
                    onEdit={this.props.onEditMapping}
                    onDelete={this.showDeleteMappingDialog}
                    onDownload={this.configDownload.download}
                    onDuplicate={this.duplicateMapping}
                    appendRef={this.mappingsTable}
                  />
                )}
              />
            </DataTable>
            <DeleteDialog
              visible={this.state.isDeleteDialogShown}
              onHide={this.hideDeleteMappingDialog}
              mappingName={this.state.selectedMappingName}
            />
            {/** reference is used for calling onDownload within ButtonCell component */}
            <ConfigurationDownload onRef={(ref) => (this.configDownload = ref)} />
          </div>
          <div className="versioning-sidebar">{sidebar}</div>
        </div>
      </>
    );
  }
}

MappingsTable.propTypes = {
  /** The value used for fritlering of the mappings */
  filterValue: PropTypes.string,
  /** Callback when the user wants to edit an agent mapping */
  onEditMapping: PropTypes.func.isRequired,
  /** Callback when the user wants to duplicate an agent mapping */
  onDuplicateMapping: PropTypes.func.isRequired,
  /** The max height of the data table */
  maxHeight: PropTypes.string,

  /** Redux Props */
  /** The mappings to show */
  mappings: PropTypes.arrayOf(PropTypes.object).isRequired,
  /** Function for fetching agent mappings */
  fetchMappings: PropTypes.func.isRequired,
  /** Function for adding or updating agent mappings */
  putMappings: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  const { mappings } = state.mappings;
  return {
    mappings,
  };
}

const mapDispatchToProps = {
  fetchMappings: mappingsActions.fetchMappings,
  putMappings: mappingsActions.putMappings,
};

export default connect(mapStateToProps, mapDispatchToProps)(MappingsTable);
