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
      mapping: {}
    }
  }

  handleFilterChange = (e) => this.setState({filter: e.target.value});

  showEditMappingDialog = (mapping = {}) => this.setState({isEditDialogShown: true, mapping: mapping} );
  hideEditMappingDialog = () => this.setState({isEditDialogShown: false, mapping: {} });

  showDownloadDialog = () => this.setState({isDownloadDialogShown: true});
  hideDownloadDialog = () => this.setState({isDownloadDialogShown: false});

  render(){
    const contentHeight = 'calc(100vh - 7rem)';
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
            height: ${contentHeight};
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