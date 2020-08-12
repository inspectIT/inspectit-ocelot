import React from 'react';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import StatusTable from './StatusTable';
import StatusToolbar from './StatusToolbar';
import StatusFooterToolbar from './StatusFooterToolbar';
import AgentConfigurationDialog from './dialogs/AgentConfigurationDialog';
import axios from '../../../lib/axios-api';
import { map, isEqual } from 'lodash';

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
      error: false,
      filteredAgents: props.agents,
      isAgentConfigurationShown: false,
      attributes: '',
      configurationValue: '',
      errorConifg: false,
      isLoading: false,
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

  updateFilter = (filter) => {
    this.setState({ filter }, this.filterAgents);
  };

  filterAgents = () => {
    const { agents } = this.props;
    const { filter, useRegexFilter } = this.state;

    if (filter === '') {
      this.setState({
        error: false,
        filteredAgents: agents,
      });
    } else {
      try {
        let filterValue;
        if (useRegexFilter) {
          filterValue = filter;
        } else {
          filterValue = filter.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        }

        const regex = RegExp(filterValue, 'i');

        const filteredAgents = agents.filter((agent) => {
          const agentFilter = this.getAgentFilter(agent);
          return this.checkRegex(agentFilter, regex);
        });

        this.setState({
          error: false,
          filteredAgents,
        });
      } catch (error) {
        this.setState({
          error: true,
          filteredAgents: agents,
        });
      }
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
      filteredAgents,
      useRegexFilter,
      error,
      readOnly,
      isAgentConfigurationShown,
      configurationValue,
      configurationLoadingFailed,
      isLoading,
      agentId,
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
              useRegexFilter={useRegexFilter}
              error={error}
              disableClear={readOnly}
            />
          </div>
          <div className="data-table">
            <StatusTable data={filteredAgents} filter={filter} onShowConfiguration={this.showAgentConfigurationForAttributes} />
          </div>
          <div>
            <StatusFooterToolbar fullData={agents} filteredData={filteredAgents} />
          </div>
          <AgentConfigurationDialog
            visible={isAgentConfigurationShown}
            onHide={() => this.setAgentConfigurationShown(false)}
            configurationValue={configurationValue}
            error={configurationLoadingFailed}
            loading={isLoading}
            agentName={agentId}
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

  setAgentConfigurationShown = (showDialog) => {
    this.setState({
      isAgentConfigurationShown: showDialog,
    });
  };

  showAgentConfigurationForAttributes = (agentId, attributes) => {
    this.setAgentConfigurationShown(true);
    this.setState(
      {
        agentId,
        attributes,
        configurationValue: '',
      },
      () => {
        this.fetchConfiguration(attributes);
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
              configurationValue: res.data,
              configurationLoadingFailed: false,
              isLoading: false,
            });
          })
          .catch(() => {
            this.setState({
              configurationValue: null,
              configurationLoadingFailed: true,
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
