import React from 'react';
import { connect } from 'react-redux';
import { settingsActions } from '../../../../redux/ducks/settings';

import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';

/**
 * Toolbar for user list component. Contains refresh button and input field to enter a filter value.
 */
class UserToolbar extends React.Component {
  render() {
    return (
      <div className="this">
        <style jsx>
          {`
            .this :global(.p-toolbar) {
              border: 0;
              border-radius: 0;
              background-color: #eee;
              border-bottom: 1px solid #ddd;
            }
            .p-toolbar-group-right > :global(*) {
              margin-left: 0.25rem;
            }
          `}
        </style>
        <Toolbar>
          <div className="p-toolbar-group-left">
            <div className="p-inputgroup" style={{ display: 'inline-flex', verticalAlign: 'middle' }}>
              <span className="pi p-inputgroup-addon pi-search" />
              <InputText
                style={{ width: '300px' }}
                value={this.props.filterValue}
                placeholder="Filter Users"
                onChange={(e) => this.props.onFilterChange(e.target.value)}
              />
            </div>
          </div>
          <div className="p-toolbar-group-right">
            <Button icon="pi pi-plus" onClick={this.props.onCreateUser} />
            <Button icon="pi pi-refresh" onClick={() => this.props.fetchUsers()} />
          </div>
        </Toolbar>
      </div>
    );
  }
}

const mapDispatchToProps = {
  fetchUsers: settingsActions.fetchUsers,
};

export default connect(null, mapDispatchToProps)(UserToolbar);
