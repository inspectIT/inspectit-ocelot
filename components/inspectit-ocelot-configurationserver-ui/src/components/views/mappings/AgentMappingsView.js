import React from 'react';

import MappingsToolbar from './MappingToolbar';
import MappingsTable from './MappingsTable';

import RefreshDialog from './dialogs/RefreshDialog';
import DeleteDialog from './dialogs/DeleteDialog';
import EditDialog from './dialogs/EditDialog';
import DownloadDialog from './dialogs/DownloadDialog';

/** View to display and change mappings */

class AgentMappingView extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      filter: '',
      isSaveDisabled: true,
      mapping: {}
    }
  }

  handleFilterChange = (e) => this.setState({filter: e.target.value});

  handleDisableSaveOptionChange = (bool) => this.setState({isSaveDisabled: bool});
  
  showRefreshDialog = () => this.setState({ isRefreshDialogShown: true });
  hideRefreshDialog = () => this.setState({ isRefreshDialogShown: false });

  showDeleteMappingDialog = (mapping) => this.setState({isDeleteDialogShown: true, mapping: mapping});
  hideDeleteMappingDialog = () => this.setState({isDeleteDialogShown: false, mapping: {}});

  showEditMappingDialog = (mapping = {}) => this.setState({isEditDialogShown: true, mapping: mapping} );
  hideEditMappingDialog = () => this.setState({isEditDialogShown: false, mapping: {} });

  showDownloadDialog = () => this.setState({isDownloadDialogShown: true});
  hideDownloadDialog = () => this.setState({isDownloadDialogShown: false});

  render(){
    return (
      <div className='this'>
        <style jsx>{`
          .fixed-toolbar{
            position: fixed;
            top: 4rem;
            width: calc(100vw - 4rem);
          }
          .content{
            margin-top: 3rem;
            height: calc(100vh - 7rem);
            overflow: auto auto;
          }
        `}</style>
        <div className='fixed-toolbar'>
          <MappingsToolbar 
            filterValue={this.state.filter} 
            onChangeFilter={this.handleFilterChange} 
            isSaveDisabled={this.state.isSaveDisabled} 
            onClickRefresh={this.showRefreshDialog} 
            onAddNewMapping={this.showEditMappingDialog}
            onDownload={this.showDownloadDialog}
          />
        </div>
        <div className='content'>
          <MappingsTable 
            filterValue={this.state.filter} 
            onMappingsChanged={this.handleDisableSaveOptionChange}
            onDeleteMapping={this.showDeleteMappingDialog} 
            onEditMapping={this.showEditMappingDialog}
          />
        </div>
        <RefreshDialog  
          visible={this.state.isRefreshDialogShown} 
          onHide={this.hideRefreshDialog} 
        />
        <DeleteDialog 
          visible={this.state.isDeleteDialogShown} 
          onHide={this.hideDeleteMappingDialog} 
          mapping={this.state.mapping} 
        />
        <EditDialog 
          visible={this.state.isEditDialogShown}
          onHide={this.hideEditMappingDialog}
          mapping={this.state.mapping}
        />
        <DownloadDialog 
          visible={this.state.isDownloadDialogShown}
          onHide={this.hideDownloadDialog}
        />
      </div>
    )
  }
}

export default AgentMappingView