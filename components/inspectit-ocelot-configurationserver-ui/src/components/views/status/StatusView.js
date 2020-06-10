import React from 'react';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import StatusTable from './StatusTable';
import StatusToolbar from './StatusToolbar';
import StatusFooterToolbar from './StatusFooterToolbar';
import { map } from 'lodash';

/**
 * The view presenting a list of connected agents, their mapping and when they last connected to the server.
 * The view is automatically refreshed and can also be refreshed manually using a refresh button.
 */
class StatusView extends React.Component {
  state = {
    filter: '',
    filterStatus: true,
  };

  changeFilter = () => {
    this.setState({
      filterStatus: !this.state.filterStatus,
    });
  };

  filterAgents(filter, agents) {
    if (filter.length === 0) {
      return agents;
    }
    const filterAgents = [];

    const agentValues = map(agents, (agent) => {
      return {
        ...agent,
        name: this.getAgentName(agent),
        mappingFilter: this.getMappingFilter(agent),
      };
    });

    // Without regex
    if (!this.state.filterStatus) {
      for (let i = 0; i < agentValues.length; i++) {
        if (this.checkWithoutRegex(agentValues[i], filter)) {
          filterAgents.push(agents[i]);
        }
      }
      return filterAgents;
    }

    // With regex
    let regex;
    const position = filter.indexOf('*');

    if (position !== -1) {
      filter = filter.substring(0, position) + '.' + filter.substring(position);
    }

    try {
      regex = RegExp(filter, 'i');
    } catch (error) {
      return agents;
    }

    for (let i = 0; i < agentValues.length; i++) {
      if (this.checkRedux(agentValues[i], regex)) {
        filterAgents.push(agents[i]);
      }
    }
    return filterAgents;
  }

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

  checkWithoutRegex = (agent, filter) => {
    filter = filter.toLowerCase();

    if (agent.attributes.id) {
      if (agent.attributes.id.toLowerCase().includes(filter)) {
        return true;
      }
    }
    if (agent.attributes.service) {
      if (agent.attributes.service.toLowerCase().includes(filter)) {
        return true;
      }
    }
    if (agent.mappingName) {
      if (agent.mappingName.toLowerCase().includes(filter)) {
        return true;
      }
    }
    if (agent.metaInformation != null && agent.attributes.id != null) {
      if (agent.metaInformation.agentVersion.toLowerCase().includes(filter)) {
        return true;
      }
      if (agent.metaInformation.javaVersion.toLowerCase().includes(filter)) {
        return true;
      }
    }
    return false;
  };

  checkRedux = (agent, regex) => {
    if (agent.attributes.id) {
      if (regex.test(agent.attributes.id)) {
        return true;
      }
    }
    if (agent.attributes.service) {
      if (regex.test(agent.attributes.service)) {
        return true;
      }
    }
    if (agent.mappingName) {
      if (regex.test(agent.mappingName)) {
        return true;
      }
    }
    if (agent.metaInformation != null && agent.attributes.id != null) {
      if (regex.test(agent.metaInformation.agentVersion)) {
        return true;
      }
      if (regex.test(agent.metaInformation.javaVersion)) {
        return true;
      }
    }
    return false;
  };

  render() {
    const { filter, filterStatus } = this.state;
    const { agents } = this.props;
    const filteredAgents = this.filterAgents(filter, agents);

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
              onFilterChange={(filter) => this.setState({ filter })}
              changeFilter={() => this.changeFilter}
              filterStatus={this.state.filterStatus}
            />
          </div>
          <div className="data-table">
            <StatusTable data={filteredAgents} />
          </div>
          <div>
            <StatusFooterToolbar data={filteredAgents} />
          </div>
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
}

function mapStateToProps(state) {
  const { pendingRequests, agents } = state.agentStatus;
  return {
    loading: pendingRequests > 0,
    agents,
  };
}

const mapDispatchToProps = {
  fetchStatus: agentStatusActions.fetchStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(StatusView);
