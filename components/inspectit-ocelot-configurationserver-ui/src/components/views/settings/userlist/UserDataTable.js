import React from 'react';
import { connect } from 'react-redux';

import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { settingsActions } from '../../../../redux/ducks/settings';

/**
 * Fetches and lists all users.
 */
class UserDataTable extends React.Component {

    render() {
        const { users, filterValue, maxHeight } = this.props;

        return (
            <DataTable
                value={users}
                globalFilter={filterValue}
                scrollable={true}
                scrollHeight={maxHeight}
            >
                <Column field='id' header='ID' />
                <Column field='username' header='Username' />
                <Column
                    field='ldapUser'
                    header='LDAP User'
                    body={(data) => (<i class={data.ldapUser ? 'pi pi-check' : 'pi pi-times'}></i>)}
                />
            </DataTable>
        )
    }

    componentDidMount = () => {
        this.props.fetchUsers();
    }

}

function mapStateToProps(state) {
    const { users } = state.settings;
    return {
        users
    }
}

const mapDispatchToProps = {
    fetchUsers: settingsActions.fetchUsers,
}

export default connect(mapStateToProps, mapDispatchToProps)(UserDataTable); 