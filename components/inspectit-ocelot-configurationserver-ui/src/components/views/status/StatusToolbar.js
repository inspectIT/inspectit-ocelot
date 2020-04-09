import React from 'react';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import ClearDialog from './dialogs/ClearDialog';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';

/**
 * Toolbar in the status view. Allows filtering of statuses, refreshing and clearing all statuses.
 */
class StatusToolbar extends React.Component {
  state = {
    isClearDialogShown: false,
  };

  render() {
    const { clearing, refreshing, fetchStatus, filter, onFilterChange } = this.props;

    const tooltipOptions = {
      showDelay: 500,
      position: 'top',
    };

    return (
      <div className="this">
        <style jsx>{`
          .this :global(.p-toolbar) {
            border: 0;
            border-radius: 0;
            background-color: #eee;
            border-bottom: 1px solid #ddd;
          }
          .p-toolbar-group-right > :global(*) {
            margin-left: 0.25rem;
          }
        `}</style>
        <Toolbar>
          <div className="p-toolbar-group-left">
            <div className="p-inputgroup" style={{ display: 'inline-flex', verticalAlign: 'middle' }}>
              <span className="pi p-inputgroup-addon pi-search" />
              <InputText
                onKeyPress={this.onKeyPress}
                style={{ width: '300px' }}
                value={filter}
                placeholder={'Filter Agents'}
                onChange={(e) => onFilterChange(e.target.value)}
              />
            </div>
          </div>
          <div className="p-toolbar-group-right">
            <Button
              onClick={fetchStatus}
              tooltip="Reload"
              icon={'pi pi-refresh' + (refreshing ? ' pi-spin' : '')}
              tooltipOptions={tooltipOptions}
            />
            <Button disabled={clearing} onClick={() => this.setState({ isClearDialogShown: true })} label="Clear All" />
          </div>
        </Toolbar>
        <ClearDialog visible={this.state.isClearDialogShown} onHide={() => this.setState({ isClearDialogShown: false })} />
      </div>
    );
  }
}

function mapStateToProps(state) {
  const { pendingClearRequests, pendingRequests } = state.agentStatus;
  return {
    clearing: pendingClearRequests > 0,
    refreshing: pendingRequests > 0,
  };
}

const mapDispatchToProps = {
  fetchStatus: agentStatusActions.fetchStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(StatusToolbar);
