import React from 'react';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import StatusTable from './StatusTable';
import StatusToolbar from './StatusToolbar';
import StatusFooterToolbar from './StatusFooterToolbar';
import axios from '../../../lib/axios-api';
import { isEqual, map } from 'lodash';
import DownloadDialogue from '../dialogs/DownloadDialogue';
import ServiceStateDialog from '../dialogs/ServiceStateDialog';
import { downloadArchiveFromJson } from '../../../functions/export-selection.function';
import AgentHealthStateDialogue from './dialogs/AgentHealthStateDialogue';

/**
 * The view presenting a list of connected agents, their mapping and when they last connected to the server.
 * The view is automatically refreshed and can also be refreshed manually using a refresh button.
 */
class StatusView extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      filter: '',
      useRegexFilter: false,
      useServiceMerge: true,
      error: false,
      agentsToShow: props.agents,
      showDownloadDialog: false,
      showHealthStateDialog: false,
      isServiceStateDialogShown: false,
      serviceStates: '{}',
      attributes: '',
      contentValue: '',
      contentType: '',
      errorConfig: false,
      isLoading: false,
      isDownloadDialogFooterHidden: false,
    };
  }

  componentDidUpdate(prevProps) {
    if (!isEqual(prevProps.agents, this.props.agents)) {
      this.filterAgents();
    }
  }

  onFilterModeChange = ({ checked }) => {
    this.setState({ useRegexFilter: checked }, this.filterAgents);
  };

  onServiceMergeChange = ({ checked }) => {
    this.setState({ useServiceMerge: checked }, this.filterAgents);
  };

  updateFilter = (filter) => {
    this.setState({ filter }, this.filterAgents);
  };

  filterAgents = () => {
    const { agents } = this.props;
    const { filter, useRegexFilter } = this.state;

    if (filter === '') {
      this.setState(
        {
          error: false,
          agentsToShow: agents,
        },
        this.mergeAgents
      );
    } else {
      try {
        let filterValue;
        if (useRegexFilter) {
          filterValue = filter;
        } else {
          filterValue = filter.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        }

        const regex = RegExp(filterValue, 'i');

        const agentsToShow = agents.filter((agent) => {
          const agentFilter = this.getAgentFilter(agent);
          return this.checkRegex(agentFilter, regex);
        });
        this.setState(
          {
            error: false,
            agentsToShow,
          },
          this.mergeAgents
        );
      } catch (error) {
        this.setState(
          {
            error: true,
            agentsToShow: agents,
          },
          this.mergeAgents
        );
      }
    }
  };

  getServiceName = ({ metaInformation, attributes }) => {
    if (metaInformation) {
      return attributes.service;
    } else {
      return null;
    }
  };

  getStartTime = ({ metaInformation }) => {
    if (metaInformation) {
      return metaInformation.startTime;
    } else {
      return null;
    }
  };

  mergeAgents = () => {
    const { agentsToShow, useServiceMerge } = this.state;
    if (useServiceMerge) {
      const mergedMap = agentsToShow.reduce((result, agent) => {
        const name = this.getServiceName(agent);
        if (result[name]) {
          if (this.getStartTime(result[name]) < this.getStartTime(agent)) {
            result[name] = {
              ...agent,
              count: result[name].count + 1,
            };
          } else {
            result[name].count += 1;
          }
        } else {
          result[name] = {
            ...agent,
            count: 1,
          };
        }
        return result;
      }, {});
      this.setState({
        error: false,
        agentsToShow: Object.values(mergedMap),
      });
    }
  };

  getAgentFilter = (agent) => {
    return {
      ...agent,
      name: this.getAgentName(agent),
      mappingFilter: this.getMappingFilter(agent),
    };
  };

  getAgentName = ({ metaInformation, attributes }) => {
    if (metaInformation) {
      return attributes.service + '(' + metaInformation.agentId + ')';
    } else {
      return null;
    }
  };

  getMappingFilter = ({ mappingName, attributes }) => {
    const filterArray = map(attributes, (value, key) => key + ':' + value);
    filterArray.push(mappingName);
    return filterArray;
  };

  checkRegex = (agent, regex) => {
    if (agent.name) {
      if (regex.test(agent.name)) {
        return true;
      }
    }
    for (let i = 0; i < agent.mappingFilter.length; i++) {
      if (regex.test(agent.mappingFilter[i])) {
        return true;
      }
    }
    if (agent.metaInformation != null) {
      if (regex.test(agent.metaInformation.agentVersion)) {
        return true;
      }
      if (regex.test(agent.metaInformation.javaVersion)) {
        return true;
      }
      if (regex.test(agent.metaInformation.agentId)) {
        return true;
      }
    }
    return false;
  };

  render() {
    const { agents } = this.props;
    const {
      filter,
      agentsToShow,
      useRegexFilter,
      useServiceMerge,
      error,
      readOnly,
      showDownloadDialog,
      showHealthStateDialog,
      contentValue,
      contentType,
      contentLoadingFailed,
      isLoading,
      agentId,
      isDownloadDialogFooterHidden: isDownloadDialogFooterHidden,
    } = this.state;

    return (
      <>
        <style jsx>{`
          .this {
            display: flex;
            flex-direction: column;
            height: 100%;
          }
          .data-table {
            width: 100%;
            overflow-x: auto;
            flex: 1;
          }
        `}</style>
        <div className="this">
          <div>
            <StatusToolbar
              filter={filter}
              onFilterChange={this.updateFilter}
              onModeChange={this.onFilterModeChange}
              onServiceMergeChange={this.onServiceMergeChange}
              useRegexFilter={useRegexFilter}
              useServiceMerge={useServiceMerge}
              error={error}
              disableClear={readOnly}
            />
          </div>
          <div className="data-table">
            <StatusTable
              data={agentsToShow}
              filter={filter}
              onShowDownloadDialog={this.showDownloadDialog}
              onShowServiceStateDialog={this.showServiceStateDialog}
              onShowHealthStateDialog={this.showHealthStateDialog}
            />
          </div>
          <div>
            <StatusFooterToolbar fullData={agents} filteredData={agentsToShow} />
          </div>
          <DownloadDialogue
            visible={showDownloadDialog}
            onHide={() => this.setShowDownloadDialog(false)}
            error={contentLoadingFailed}
            loading={isLoading}
            contentValue={contentValue}
            contentType={contentType}
            contextName={'Agent ' + agentId}
            isDownloadDialogFooterHidden={isDownloadDialogFooterHidden}
            onCancel={() => {
              this.setShowDownloadDialog(false);
            }}
          />
          <AgentHealthStateDialogue
            visible={showHealthStateDialog}
            onHide={() => this.setShowHealthStateDialog(false)}
            contentValue={this.state.attributes}
            contextName={'Agent ' + agentId}
          />
          <ServiceStateDialog
            visible={this.state.isServiceStateDialogShown}
            onHide={() => this.setServiceStateDialogShown(false)}
            serviceStateMap={this.state.serviceStates}
          />
        </div>
      </>
    );
  }

  componentDidMount() {
    this.fetchNewStatus();
    this.updateTimer = setInterval(this.fetchNewStatus, 10000);
  }

  componentWillUnmount() {
    clearInterval(this.updateTimer);
  }

  fetchNewStatus = () => {
    const { loading, fetchStatus } = this.props;
    if (!loading) {
      fetchStatus();
    }
  };

  /*
   * SERVICE STATE DIALOG
   */
  setServiceStateDialogShown = (showDialog) => {
    this.setState({
      isServiceStateDialogShown: showDialog,
    });
  };

  showServiceStateDialog = (serviceStates) => {
    this.setServiceStateDialogShown(true);
    this.setState({
      serviceStates: serviceStates,
    });
  };

  /*
   * DOWNLOAD DIALOG
   */
  setShowDownloadDialog = (showDialog) => {
    this.setState({
      showDownloadDialog: showDialog,
    });
  };

  setShowHealthStateDialog = (showDialog) => {
    this.setState({
      showHealthStateDialog: showDialog,
    });
  };

  showDownloadDialog = (agentId, attributes, contentType) => {
    this.setShowDownloadDialog(true);
    this.setState(
      {
        agentId,
        attributes,
        contentValue: '',
        contentType,
      },
      () => {
        switch (contentType) {
          case 'config':
            this.fetchConfiguration(attributes);
            break;
          case 'log':
            this.fetchLog(agentId);
            break;
          case 'archive':
            this.downloadSupportArchive(agentId, attributes);
            break;
          default:
            this.setShowDownloadDialog(false);
            break;
        }
      }
    );
  };

  showHealthStateDialog = (agentId, attributes) => {
    this.setShowHealthStateDialog(true);
    this.setState({
      agentId,
      attributes,
    });
  };

  downloadSupportArchive = (agentId, agentVersion) => {
    this.setState(
      {
        isLoading: true,
        isDownloadDialogFooterHidden: true,
      },
      () => {
        axios
          .get('/agent/supportArchive', {
            params: { 'agent-id': agentId },
          })
          .then((res) => {
            downloadArchiveFromJson(res.data, agentId, agentVersion);
            this.setShowDownloadDialog(false);
            this.setState({
              isLoading: false,
              isDownloadDialogFooterHidden: false,
            });
          })
          .catch(() => {
            this.setState({
              contentValue: '',
              contentLoadingFailed: true,
              isLoading: false,
              isDownloadDialogFooterHidden: false,
            });
          });
      }
    );
  };

  fetchConfiguration = (attributes) => {
    const requestParams = attributes;
    if (!requestParams) {
      return;
    }
    this.setState(
      {
        isLoading: true,
      },
      () => {
        axios
          .get('/configuration/agent-configuration', {
            params: { ...requestParams },
          })
          .then((res) => {
            this.setState({
              contentValue: res.data,
              contentLoadingFailed: false,
              isLoading: false,
            });
          })
          .catch(() => {
            this.setState({
              contentValue: null,
              contentLoadingFailed: true,
              isLoading: false,
            });
          });
      }
    );
  };

  fetchLog = (agentId) => {
    this.setState(
      {
        isLoading: true,
      },
      () => {
        axios
          .get('/command/logs', {
            params: { 'agent-id': agentId },
          })
          .then((res) => {
            this.setState({
              contentValue: res.data,
              contentLoadingFailed: false,
              isLoading: false,
            });
          })
          .catch(() => {
            this.setState({
              contentValue: null,
              contentLoadingFailed: true,
              isLoading: false,
            });
          });
      }
    );
  };
}

function mapStateToProps(state) {
  const { pendingRequests, agents } = state.agentStatus;
  return {
    loading: pendingRequests > 0,
    agents,
    readOnly: !state.authentication.permissions.write,
  };
}

const mapDispatchToProps = {
  fetchStatus: agentStatusActions.fetchStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(StatusView);
