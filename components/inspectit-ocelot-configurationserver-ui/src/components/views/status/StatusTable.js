import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import dateformat from 'dateformat';
import TimeAgo from 'react-timeago';
import { map } from 'lodash';
import classnames from 'classnames';
import { linkPrefix } from '../../../lib/configuration';

const timeFormatter = (time, unit, suffix) => {
  if (unit === 'second') {
    const tens = Math.max(10, Math.ceil(time / 10) * 10);
    return '< ' + tens + ' seconds ' + suffix;
  } else {
    return time + ' ' + unit + (time > 1 ? 's ' : ' ') + suffix;
  }
};

/**
 * Component for rendering the agent mapping cell of the data table.
 */
class AgentMappingCell extends React.Component {
  state = {
    showAttributes: false,
  };

  toggleShowAttributes = () => {
    this.setState({
      showAttributes: !this.state.showAttributes,
    });
  };

  render() {
    const {
      data: { mappingName, attributes },
    } = this.props;
    const { showAttributes } = this.state;
    let name;
    let classname;
    if (mappingName) {
      name = mappingName;
    } else {
      name = 'no mapping';
      classname = 'no-mapping';
    }

    return (
      <div className="this">
        <style jsx>{`
          .mapping {
            display: flex;
            align-items: stretch;
          }
          .mapping-name {
            flex: 1;
            margin-right: 0.5rem;
          }
          .no-mapping {
            color: gray;
            font-style: italic;
          }
          .show-attributes {
            float: right;
            cursor: pointer;
          }
          .attributes {
            margin-top: 0.5rem;
            border-left: 0.25rem solid #ddd;
            padding-left: 0.5rem;
          }
        `}</style>
        <div className="mapping">
          <div className={'mapping-name ' + classname}>{name}</div>
          <a onClick={() => this.toggleShowAttributes()} className="show-attributes">
            {showAttributes ? 'Hide' : 'Show'} Attributes
          </a>
        </div>
        {showAttributes && (
          <div className="attributes">
            {map(attributes, (value, key) => (
              <div key={key}>
                <b>{key}:</b> {value}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }
}

/**
 * The table listing all agent statuses
 */
class StatusTable extends React.Component {
  state = {
    configurationValue: '',
  };

  nameTemplate = (rowData) => {
    const { onShowConfiguration } = this.props;
    const {
      metaInformation,
      attributes,
      attributes: { service },
    } = rowData;

    let name = '-';
    let agentIdElement;
    let agentId = null;
    if (metaInformation) {
      if (service) {
        name = service;
      }
      agentId = metaInformation.agentId;
      agentIdElement = <span style={{ color: 'gray' }}>({agentId})</span>;
    }
    return (
      <div className="this">
        <style jsx>{`
          .this {
            position: relative;
          }
          .this :global(.config-info-button) {
            width: 1.2rem;
            height: 1.2rem;
            position: absolute;
            right: 0;
            top: 0;
            background: #ddd;
            border-color: #ddd;
          }
        `}</style>
        {name} {agentIdElement}
        <Button
          className="config-info-button"
          icon="pi pi-cog"
          onClick={() => onShowConfiguration(agentId, attributes)}
          tooltip="Show Configuration"
          tooltipOptions={{ showDelay: 500 }}
        />
      </div>
    );
  };

  iconTemplate = (rowData) => {
    const { metaInformation } = rowData;

    let imgColor;
    let tooltip;
    if (metaInformation) {
      imgColor = 'orange';
      tooltip = 'inspectIT Ocelot Agent';
    } else {
      imgColor = 'gray';
      tooltip = 'Generic Client';
    }

    const imgSrc = linkPrefix + '/static/images/inspectit-ocelot-head_' + imgColor + '.svg';
    return (
      <>
        <style jsx>{`
          .icon {
            width: 1.5rem;
            vertical-align: bottom;
            margin-right: 0.5rem;
          }
        `}</style>
        <img className="icon" title={tooltip} src={imgSrc} />
      </>
    );
  };

  agentVersionTemplate = (rowData) => {
    const { metaInformation } = rowData;

    return <span>{metaInformation ? metaInformation.agentVersion : '-'}</span>;
  };

  javaVersionTemplate = (rowData) => {
    const { metaInformation } = rowData;

    return <span>{metaInformation ? metaInformation.javaVersion : '-'}</span>;
  };

  jvmRestartTemplate = (rowData) => {
    const { metaInformation } = rowData;

    let date = '-';
    if (metaInformation) {
      const startTime = new Date(Number(metaInformation.startTime));
      date = dateformat(startTime, 'dd/mm/yy HH:MM:ss');
    }
    return <span>{date}</span>;
  };

  agentMappingTemplate = (rowData) => {
    return <AgentMappingCell data={rowData} />;
  };

  sourceBranchTemplate = (rowData) => {
    let branch = 'Unknown Branch';
    if (rowData.sourceBranch) {
      branch = rowData.sourceBranch;
    }
    const isLiveBranch = branch === 'live';
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
          <span>{branch}</span>
        </div>
      </>
    );
  };

  lastFetchTemplate = ({ lastConfigFetch }) => {
    return <TimeAgo date={lastConfigFetch} formatter={timeFormatter} />;
  };

  getAgentName = ({ metaInformation, attributes }) => {
    if (metaInformation) {
      return attributes.service + ' (' + metaInformation.agentId + ')';
    } else {
      return null;
    }
  };

  getMappingFilter = ({ mappingName, attributes }) => {
    const filterArray = map(attributes, (value, key) => key + ': ' + value);
    filterArray.push(mappingName);
    return filterArray;
  };

  showConfigurationDialog = (attribute) => {
    this.setState({
      attributes: attribute,
    });
    this.props.setAgentConfigurationShown(true);
  };

  render() {
    const { data: agents } = this.props;
    const agentValues = map(agents, (agent) => {
      return {
        ...agent,
        name: this.getAgentName(agent),
      };
    });

    return (
      <div className="this">
        <style jsx>{`
          .this :global(.p-datatable) {
            min-width: 1430px;
          }

          .this :global(.p-datatable) :global(th) {
            border: 0 none;
            text-align: left;
          }

          .this :global(.p-datatable) :global(.p-filter-column) {
            border-top: 1px solid #c8c8c8;
          }

          .this :global(.p-datatable-tbody) :global(td) {
            border: 0 none;
            cursor: auto;
            vertical-align: top;
          }
        `}</style>
        <DataTable value={agentValues} rowHover reorderableColumns>
          <Column body={this.iconTemplate} style={{ width: '34px' }} />
          <Column header="Name" field="name" body={this.nameTemplate} sortable style={{ width: '400px' }} />
          <Column
            header="Agent Version"
            field="metaInformation.agentVersion"
            body={this.agentVersionTemplate}
            sortable
            style={{ width: '150px' }}
          />
          <Column
            header="Java Version"
            field="metaInformation.javaVersion"
            body={this.javaVersionTemplate}
            sortable
            style={{ width: '150px' }}
          />
          <Column
            header="Last JVM Restart"
            field="metaInformation.startTime"
            body={this.jvmRestartTemplate}
            sortable
            style={{ width: '175px' }}
          />
          <Column header="Source Branch" field="sourceBranch" body={this.sourceBranchTemplate} style={{ width: '150px' }} sortable />
          <Column header="Agent Mapping" field="mappingFilter" body={this.agentMappingTemplate} sortable />
          <Column header="Last Fetch" field="lastConfigFetch" body={this.lastFetchTemplate} sortable style={{ width: '200px' }} />
        </DataTable>
      </div>
    );
  }
}

export default StatusTable;
