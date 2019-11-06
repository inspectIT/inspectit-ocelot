import React from 'react';

import MappingsToolbar from './MappingToolbar';
import MappingsTable from './MappingsTable';

import EditDialog from './dialogs/EditDialog';
import DownloadDialog from './dialogs/DownloadDialog';

/** View to display and change mappings */

class AgentMappingView extends React.Component {
  state = {
    mappingsFilter: '',
    selectedMapping: null
  }

  handleFilterChange = (newFilter) => this.setState({ mappingsFilter: newFilter });

  showEditMappingDialog = (selectedMapping = null) => this.setState({ isEditDialogShown: true, selectedMapping: selectedMapping });
  hideEditMappingDialog = () => this.setState({ isEditDialogShown: false, selectedMapping: null });

  showDownloadDialog = () => this.setState({ isDownloadDialogShown: true });
  hideDownloadDialog = () => this.setState({ isDownloadDialogShown: false });

  render() {
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
            filterValue={this.state.mappingsFilter}
            onChangeFilter={this.handleFilterChange}
            onAddNewMapping={this.showEditMappingDialog}
            onDownload={this.showDownloadDialog}
          />
        </div>
        <div className='content'>
          <MappingsTable
            filterValue={this.state.mappingsFilter}
            onEditMapping={this.showEditMappingDialog}
            maxHeight={`calc(${contentHeight} - 2.5em)`}
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