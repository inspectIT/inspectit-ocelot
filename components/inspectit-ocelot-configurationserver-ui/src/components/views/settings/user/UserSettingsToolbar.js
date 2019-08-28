import React from 'react'
import { connect } from 'react-redux'
import {settingsActions} from '../../../../redux/ducks/settings'
import  {Toolbar} from 'primereact/toolbar'
import {Button} from 'primereact/button'
import {InputText} from 'primereact/inputtext'

class UserToolbar extends React.Component {

  render(){
    return(
      <div className='this'>
        <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
        }

        .this :global(.p-toolbar-group-left) :global(.p-button) {
          margin-right: 0.25rem;
        }
        .searchbox {
          font-size: 1rem;
          display: flex;
          align-items: center;
          height: 2rem;
        }
        h4 {
          margin-right: 1rem;
          font-weight: normal;
        }
        .searchbox :global(.pi) {
          font-size: 1.5rem;
          color: #aaa;
          margin-right: 0.5rem;
        }
        `}</style>
        <Toolbar>
          <div className='p-toolbar-group-left'>
            <div className='searchbox'>
              <i className="pi pi-users"></i>
              <h4>Users</h4>
              <InputText
                placeholder='Search'
                tooltip='Search trough entering an ID or a username'
                value={this.props.filterValue}
                onChange={this.props.changeFilter}
              />
            </div>
          </div>
          <div className='p-toolbar-group-right'>
            <Button icon='pi pi-refresh' onClick={(e) => {this.props.getUsers()}} />
          </div>
        </Toolbar>
      </div>
    )
  }
}

const mapDispatchToProps = {
  getUsers: settingsActions.fetchUsers,
}

export default connect(null, mapDispatchToProps)(UserToolbar)