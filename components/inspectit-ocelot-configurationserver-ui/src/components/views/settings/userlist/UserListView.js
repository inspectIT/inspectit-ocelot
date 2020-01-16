import React from 'react';

import UserToolbar from './UserToolbar';
import UserDataTable from './UserDataTable';

/**
 * Contains toolbar and list component.
 */
class UserListView extends React.Component {

    state = {
        filter: ''
    };

    render() {
        const contentHeight = 'calc(100vh - 10rem)';

        return (
            <div className='this'>
                <style jsx>{`
                    .fixed{
                        position: fixed;
                        top: 7rem;
                        width: calc(100vw - 4rem);
                    }
                    .content{
                        margin-top: 3rem;
                        height: ${contentHeight};
                        overflow: hidden;
                    }
                `}</style>
                <div className='fixed'>
                    <UserToolbar
                        filterValue={this.state.filter}
                        onFilterChange={this.handleFilterChange}
                    />
                </div>
                <div className='content'>
                    <UserDataTable
                        filterValue={this.state.filter}
                        maxHeight={`calc(${contentHeight} - 2.5em)`}
                    />
                </div>
            </div>
        )
    }

    handleFilterChange = (value) => {
        this.setState({ filter: value });
    }

}

export default UserListView; 