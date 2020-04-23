import React from 'react';

import UserToolbar from './UserToolbar';
import UserDataTable from './UserDataTable';
import CreateDialog from '../dialogs/CreateDialog';

/**
 * Contains toolbar and list component.
 */
class UserListView extends React.Component {
  state = {
    filter: '',
  };

  showCreateUserDialog = () => this.setState({ isCreateUserDialogShown: true });
  hideCreateUserDialog = () => this.setState({ isCreateUserDialogShown: false });

  render() {
    const contentHeight = 'calc(100vh - 10rem)';

    return (
      <div className="this">
        <style jsx>{`
          .fixed {
            position: fixed;
            top: 7rem;
            width: calc(100vw - 4rem);
          }
          .content {
            margin-top: 3rem;
            height: ${contentHeight};
            overflow: hidden;
          }
        `}</style>
        <div className="fixed">
          <UserToolbar filterValue={this.state.filter} onFilterChange={this.handleFilterChange} onCreateUser={this.showCreateUserDialog} />
        </div>
        <div className="content">
          <UserDataTable filterValue={this.state.filter} maxHeight={`calc(${contentHeight} - 2.5em)`} />
        </div>
        <CreateDialog visible={this.state.isCreateUserDialogShown} onHide={this.hideCreateUserDialog} />
      </div>
    );
  }

  handleFilterChange = (value) => {
    this.setState({ filter: value });
  };
}

export default UserListView;
