import React from 'react';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import StatusTable from './StatusTable';
import StatusToolbar from './StatusToolbar';
import StatusFooterToolbar from './StatusFooterToolbar';

/**
 * The view presenting a list of connected agents, their mapping and when they last connected to the server.
 * The view is automatically refreshed and can also be refreshed manually using a refresh button.
 */
class StatusView extends React.Component {
  state = {
    filter: '',
  };

  filterAgents(filter, agents) {
    var newAgents = [];
    if (filter.length !== 0) {
      for (let i = 0; i < agents.length; i++) {

        let found = false;
        if (agents[i].attributes.id.includes(filter) && !found) {
          newAgents.push(agents[i]);
          found = true;
        } else if (agents[i].attributes.service.includes(filter) && !found) {
          newAgents.push(agents[i]);
          found = true;
        } else if (agents[i].mappingName.includes(filter) && !found) {
          newAgents.push(agents[i]);
          found = true;
        } else if (agents[i].metaInformation.agentVersion.includes(filter) && !found) {
          newAgents.push(agents[i]);
        } else if (agents[i].metaInformation.javaVersion.includes(filter) && !found) {
          newAgents.push(agents[i]);
        }

      }
      return newAgents
    } else{
      return agents;
    }
  }

  render() {
    const { filter } = this.state;
    const { agents } = this.props;
    const filterAgents = this.filterAgents(filter, agents);
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
            <StatusToolbar filter={filter} onFilterChange={(filter) => this.setState({ filter })} />
          </div>
          <div className="data-table">
            <StatusTable data={filterAgents} />
          </div>
          <div>
            <StatusFooterToolbar data={agents} filterAgents={filterAgents} />
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
