import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import dateformat from 'dateformat';
import TimeAgo from 'react-timeago';
import { map } from 'lodash';
import classnames from 'classnames';
import classNames from 'classnames';
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
            color: #007ad9;
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
    logValue: '',
    showServiceStateDialog: false,
  };

  resolveServiceAvailability = (metaInformation) => {
    const { agentVersion } = metaInformation;
    const agentVersionTokens = agentVersion.split('.');
    let logAvailable = false;
    let agentCommandsEnabled = true;
    let serviceStatesAvailable = false;
    let supportArchiveAvailable = false;
    let serviceStates = '{}';

    // in case of snapshot version, assume we are up to date
    if (agentVersion == 'SNAPSHOT') {
      logAvailable = agentCommandsEnabled = serviceStatesAvailable = supportArchiveAvailable = true;
    } else if (agentVersionTokens.length === 2 || agentVersionTokens.length === 3) {
      const agentVersionNumber =
        agentVersionTokens[0] * 10000 + agentVersionTokens[1] * 100 + (agentVersionTokens.length === 3 ? agentVersionTokens[2] * 1 : 0);
      // logs are available at version 1.15+
      logAvailable = agentVersionNumber > 11500;
      // support archive is available at version 2.2.0+
      supportArchiveAvailable = agentVersionNumber >= 20200;
      // service states are available at version 2.2.0+
      serviceStatesAvailable = agentVersionNumber >= 20200;
    }

    if (serviceStatesAvailable) {
      try {
        serviceStates = JSON.parse(metaInformation.serviceStates);
        logAvailable = serviceStates.LogPreloader;
        agentCommandsEnabled = serviceStates.AgentCommandService;
        supportArchiveAvailable = agentCommandsEnabled;
      } catch (e) {
        //ignore
      }
    }
    return {
      logAvailable: logAvailable,
      agentCommandsEnabled: agentCommandsEnabled,
      serviceStatesAvailable: serviceStatesAvailable,
      supportArchiveAvailable: supportArchiveAvailable,
      serviceStates: serviceStates,
    };
  };

  nameTemplate = (rowData) => {
    const { onShowDownloadDialog, onShowServiceStateDialog } = this.props;
    const {
      metaInformation,
      attributes,
      attributes: { service },
    } = rowData;

    // Set default values
    let logAvailable = false;
    let agentCommandsEnabled = false;
    let serviceStatesAvailable = false;
    let serviceStates = '{}';
    let name = '-';
    let agentIdElement = <span></span>
    let agentId = '';

    if (metaInformation && Object.entries(metaInformation).length > 0) {
      ({ logAvailable, agentCommandsEnabled, serviceStatesAvailable, serviceStates } = this.resolveServiceAvailability(metaInformation));

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
          .this :global(.log-button) {
            width: 1.2rem;
            height: 1.2rem;
            position: absolute;
            right: 1.5rem;
            top: 0;
            background: #ddd;
            border-color: #ddd;
          }
          .this :global(.service-state-button) {
            width: 1.2rem;
            height: 1.2rem;
            position: absolute;
            right: 3rem;
            top: 0;
            background: #ddd;
            border-color: #ddd;
          }
          .this :global(.badge) {
            width: 1.2rem;
            height: 1.2rem;
            position: absolute;
            right: 4.5rem;
            top: 0;
            background: #007ad9;
            border-radius: 25%;
            display: inline-flex;
            justify-content: center;
            color: white;
          }
          .this :global(.might-overflow) {
            max-width: 17.8rem;
            display: inline-block;
            white-space: normal;
            overflow: visible;
            overflow-wrap: break-word;
            text-overflow: unset;
          }
        `}</style>
        <span className="might-overflow">
          {name} {agentIdElement}
        </span>
        {rowData.count > 1 ? (
          <span className="badge">
            <b>{rowData.count}</b>
          </span>
        ) : null}
        <Button
          className="service-state-button"
          icon="pi pi-sliders-h"
          onClick={() => onShowServiceStateDialog(serviceStates)}
          tooltip={serviceStatesAvailable ? 'Service States' : 'Service States are available for agent versions 2.2.0 and above'}
          tooltipOptions={{ showDelay: 500 }}
          disabled={!serviceStatesAvailable}
        />
        <Button
          className="config-info-button"
          icon="pi pi-cog"
          onClick={() => onShowDownloadDialog(agentId, attributes, 'config')}
          tooltip="Show Configuration"
          tooltipOptions={{ showDelay: 500 }}
        />
        <Button
          className="log-button"
          icon="pi pi-align-justify"
          onClick={() => onShowDownloadDialog(agentId, attributes, 'log')}
          tooltip={
            logAvailable && agentCommandsEnabled
              ? 'Show Logs'
              : "<b>Logs not available!</b>\nMake sure to enable 'log-preloading' and 'agent-commands' in the config, and configure the URL for the agent commands.\nThis feature is only available for agent versions 1.15.0 and higher"
          }
          tooltipOptions={{ showDelay: 500 }}
          disabled={!logAvailable || !agentCommandsEnabled}
        />
      </div>
    );
  };

  setServiceStateDialogShown = (showDialog) => {
    this.setState({
      showServiceStateDialog: showDialog,
    });
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

  agentHealthTemplate = (rowData) => {
    const { onShowHealthStateDialog, onShowDownloadDialog } = this.props;
    const { healthState, metaInformation } = rowData;
    const health = healthState?.health ?? null;
    const agentId = metaInformation?.agentId ?? '';
    const agentVersion = metaInformation?.agentVersion ?? '';

    // Set default values
    let agentCommandsEnabled = false;
    let supportArchiveAvailable = false;
    if (metaInformation && Object.entries(metaInformation).length > 0) {
      ({ agentCommandsEnabled, supportArchiveAvailable } = this.resolveServiceAvailability(metaInformation));
    }

    let healthInfo;
    let iconClass;
    let iconColor;
    switch (health) {
      case 'OK':
        healthInfo = 'OK';
        iconClass = 'pi-check-circle';
        iconColor = '#0abd04';
        break;
      case 'ERROR':
        healthInfo = 'Error';
        iconClass = 'pi-times-circle';
        iconColor = 'red';
        break;
      case 'WARNING':
        healthInfo = 'Warning';
        iconClass = 'pi-minus-circle';
        iconColor = '#e8c413';
        break;
      default:
        healthInfo = 'Unknown';
        iconClass = 'pi-question-circle';
        iconColor = 'gray';
        break;
    }

    return (
      <>
        <style jsx>{`
          .state {
            align-items: center;
            display: flex;
            cursor: pointer;
            position: relative;
          }

          .state :global(.archive-button) {
            width: 1.2rem;
            height: 1.2rem;
            position: absolute;
            right: 0;
            top: 0;
            background: #ddd;
            border-color: #ddd;
          }
        `}</style>
        {health ? (
          <div className="state">
            <div className="health-state" onClick={() => onShowHealthStateDialog(agentId, healthState)}>
              <i className={classNames('pi pi-fw', iconClass)} style={{ color: iconColor }}></i>
              <span>{healthInfo}</span>
            </div>
            <Button
              className="archive-button"
              icon="pi pi-cloud-download"
              onClick={() => onShowDownloadDialog(agentId, agentVersion, 'archive')}
              tooltip={
                agentCommandsEnabled && supportArchiveAvailable
                  ? 'Download Support Archive'
                  : "<b>Support archive not available!</b>\nMake sure to enable 'agent-commands' in the config and configure the URL for the agent commands. \n This feature is only available for agent versions 1.15.0 and above."
              }
              tooltipOptions={{ showDelay: 500 }}
              disabled={!agentCommandsEnabled}
            />
          </div>
        ) : (
          '-'
        )}
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
          <Column header="Agent State" field="health" body={this.agentHealthTemplate} sortable style={{ width: '150px' }} />
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
