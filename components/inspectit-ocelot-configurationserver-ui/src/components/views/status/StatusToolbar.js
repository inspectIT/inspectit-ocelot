import React from 'react';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import ClearDialog from './dialogs/ClearDialog';
import { connect } from 'react-redux';
import { agentStatusActions } from '../../../redux/ducks/agent-status';
import { Checkbox } from 'primereact/checkbox';
import { Tooltip } from 'react-tooltip';

/**
 * Toolbar in the status view. Allows filtering of statuses, refreshing and clearing all statuses.
 */
class StatusToolbar extends React.Component {
  state = {
    isClearDialogShown: false,
  };

  render() {
    const {
      clearing,
      refreshing,
      fetchStatus,
      filter,
      onFilterChange,
      disableClear,
      onModeChange,
      useRegexFilter,
      useServiceMerge,
      onServiceMergeChange,
      error,
    } = this.props;

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
            padding-top: 0.5rem;
            padding-bottom: 0.5rem;
          }

          .p-toolbar-group-left {
            align-items: center;
            display: flex;
          }

          .p-toolbar-group-right > :global(*) {
            margin-left: 0.25rem;
          }

          .p-toolbar-warning {
            margin-top: 0.3rem;
            margin-left: 0.7rem;
            font-size: 1.1rem;
            font-weight: 900;
            color: red;
          }

          .checkbox-group {
            margin-left: 1rem;
          }

          .checkbox-group label {
            margin-right: 0.5rem;
          }

          .regex-error {
            color: red !important;
          }

          .hint-badge {
            display: inline-flex;
            height: 1.2rem;
            width: 1.2rem;
            margin-left: 0.5rem;
            border-radius: 50%;
            background-color: #a4a3a3;
            justify-content: center;
            color: white;
          }

          &.__react_component_tooltip.show :global(*) {
            opacity: 1 !important;
          }
        `}</style>
        <Toolbar
          left={
            <div className="p-toolbar-group-left">
              <div className="p-inputgroup">
                <span className="pi p-inputgroup-addon pi-search" />

                <InputText
                  onKeyPress={this.onKeyPress}
                  style={{ width: '300px' }}
                  value={filter}
                  placeholder={'Filter Agents'}
                  onChange={(e) => onFilterChange(e.target.value)}
                />

                {error && <span className="p-inputgroup-addon regex-error">Invalid regular expression</span>}
              </div>

              <div className="p-inputgroup checkbox-group">
                <label>Use Regex</label>
                <Checkbox onChange={onModeChange} checked={useRegexFilter} />
              </div>
              <div className="p-inputgroup checkbox-group">
                <label style={{ width: 'max-content' }}>Combine duplicate services</label>
                <Checkbox onChange={onServiceMergeChange} checked={useServiceMerge} />
                <span
                  data-tooltip-html="Combine agents with the same name into a single entry.<br>
                Only the most recently started agent will be displayed.<br>
                A badge behind the agents name will indicate the number of combined agents."
                  data-tooltip-id="tooltip"
                  data-tooltip-variant="info"
                  className="hint-badge"
                >
                  ?
                </span>
                <Tooltip id="tooltip" place="right" />
              </div>
            </div>
          }
          right={
            <div className="p-toolbar-group-right">
              <Button
                onClick={fetchStatus}
                tooltip="Reload"
                icon={'pi pi-refresh' + (refreshing ? ' pi-spin' : '')}
                tooltipOptions={tooltipOptions}
              />
              <Button disabled={disableClear || clearing} onClick={() => this.setState({ isClearDialogShown: true })} label="Clear All" />
            </div>
          }
        />
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
