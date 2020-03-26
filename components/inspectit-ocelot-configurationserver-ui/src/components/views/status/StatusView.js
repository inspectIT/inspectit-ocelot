import React from 'react';
import { connect } from 'react-redux'
import { agentStatusActions } from '../../../redux/ducks/agent-status'
import StatusTable from './StatusTable';
import StatusToolbar from './StatusToolbar';

/**
 * The view presenting a list of connected agents, their mapping and when they last connected to the server.
 * The view is automatically refreshed and can also be refreshed manually using a refresh button.
 */
class StatusView extends React.Component {

    state = {
        filter: ""
    }

    render() {
        const { filter } = this.state;
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
                        <StatusTable filter={filter} />
                    </div>
                </div>
            </>
        );
    }

    componentDidMount() {
        this.fetchNewStatus()
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
    }
}

function mapStateToProps(state) {
    const { pendingRequests } = state.agentStatus;
    return {
        loading: pendingRequests > 0,
    }
}

const mapDispatchToProps = {
    fetchStatus: agentStatusActions.fetchStatus,
}

export default connect(mapStateToProps, mapDispatchToProps)(StatusView);