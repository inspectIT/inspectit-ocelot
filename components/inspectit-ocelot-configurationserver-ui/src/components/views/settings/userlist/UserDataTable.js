import React from 'react';
import { connect } from 'react-redux';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { settingsActions } from '../../../../redux/ducks/settings';
import DeleteDialog from '../dialogs/DeleteDialog';

/**
 * Fetches and lists all users.
 */
class UserDataTable extends React.Component {
  state = {
    userToDelete: null,
  };

  showDeleteUserDialog = (user) => {
    this.setState({ userToDelete: user });
  };
  hideDeleteUserDialog = () => {
    this.setState({ userToDelete: null });
  };

  render() {
    const { users, filterValue, maxHeight } = this.props;

    return (
      <div>
        <DataTable value={users} globalFilter={filterValue} scrollable={true} scrollHeight={maxHeight}>
          <Column field="id" header="ID" />
          <Column field="username" header="Username" />
          <Column field="ldapUser" header="LDAP User" body={(data) => <i className={data.ldapUser ? 'pi pi-check' : 'pi pi-times'}></i>} />
          <Column
            style={{ width: '3.5rem' }}
            body={(data) => <Button icon="pi pi-trash" onClick={() => this.showDeleteUserDialog(data)} />}
          />
        </DataTable>
        <DeleteDialog visible={this.state.userToDelete !== null} onHide={this.hideDeleteUserDialog} user={this.state.userToDelete || {}} />
      </div>
    );
  }

  componentDidMount = () => {
    this.props.fetchUsers();
  };
}

function mapStateToProps(state) {
  const { users } = state.settings;
  return {
    users,
  };
}

const mapDispatchToProps = {
  fetchUsers: settingsActions.fetchUsers,
};

export default connect(mapStateToProps, mapDispatchToProps)(UserDataTable);
