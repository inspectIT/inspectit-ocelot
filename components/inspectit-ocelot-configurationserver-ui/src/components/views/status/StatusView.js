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

    try {
      regex = RegExp(filter, 'i');
    } catch (error) {
      return agents;
    }

    for (let i = 0; i < agentValues.length; i++) {
      if (this.checkRegex(agentValues[i], regex)) {
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

    if(agent.name){
      if(agent.name.toLowerCase().includes(filter)){
        return true;
      }
    }
    for(let i=0; i<agent.mappingFilter.length;i++){
      if(agent.mappingFilter[i].toLowerCase().includes(filter)){
        return true;
      }
    }
    if (agent.metaInformation != null) {
      if (agent.metaInformation.agentVersion.toLowerCase().includes(filter)) {
        return true;
      }
      if (agent.metaInformation.javaVersion.toLowerCase().includes(filter)) {
        return true;
      }
      if(agent.metaInformation.agentId.toLowerCase().includes(filter)){
        return true;
      }
    }
    return false;
  };

  checkRegex = (agent, regex) => {
    if(agent.name){
      if(regex.test(agent.name)){
        return true;
      }
    }
    for(let i=0; i<agent.mappingFilter.length;i++){
      if(regex.test(agent.mappingFilter[i])){
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
      if(regex.test(agent.metaInformation.agentId)){
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
