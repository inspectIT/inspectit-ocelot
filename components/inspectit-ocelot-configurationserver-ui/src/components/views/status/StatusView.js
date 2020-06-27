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
    isAgentConfigurationShown: false,
  };
  
  render() {
    const { filter } = this.state;
    const { agents, readOnly } = this.props;
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
            <StatusToolbar filter={filter} onFilterChange={(filter) => this.setState({ filter })} disableClear={readOnly} />
          </div>
          <div className="data-table">
            <StatusTable data={agents} filter={filter} isAgentConfigurationShown={this.state.isAgentConfigurationShown} setAgentConfigurationShown={this.setAgentConfigurationShown}/>
          </div>
          <div>
            <StatusFooterToolbar data={agents} />
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

  setAgentConfigurationShown = (isShown) =>{
    this.setState({
      isAgentConfigurationShown: isShown
    })
  }



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
