import React from 'react';

import MappingsToolbar from './MappingToolbar';
import MappingsTable from './MappingsTable';

import EditDialog from './dialogs/EditDialog';
import DownloadDialog from './dialogs/DownloadDialog';

/** View to display and change mappings */

class AgentMappingView extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      filter: '',
      selectedMapping: {}
    }
  }

  handleFilterChange = (e) => this.setState({filter: e.target.value});

  showEditMappingDialog = (selectedMapping = {}) => this.setState({isEditDialogShown: true, selectedMapping: selectedMapping} );
  hideEditMappingDialog = () => this.setState({isEditDialogShown: false, selectedMapping: {} });

  showDownloadDialog = () => this.setState({isDownloadDialogShown: true});
  hideDownloadDialog = () => this.setState({isDownloadDialogShown: false});

  render(){
    const contentHeight = 'calc(100vh - 7rem - 2.5em)';
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
            overflow: hidden;
          }
        `}</style>
        <div className='fixed-toolbar'>
          <MappingsToolbar 
            filterValue={this.state.filter} 
            onChangeFilter={this.handleFilterChange} 
            onAddNewMapping={this.showEditMappingDialog}
            onDownload={this.showDownloadDialog}
          />
        </div>
        <div className='content'>
          <MappingsTable 
            filterValue={this.state.filter} 
            onEditMapping={this.showEditMappingDialog}
            maxHeight={contentHeight}
          />
        </div>
        <EditDialog 
          visible={this.state.isEditDialogShown}
          onHide={this.hideEditMappingDialog}
          mapping={this.state.selectedMapping}
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