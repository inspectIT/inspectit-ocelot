import React, { useState } from 'react';
import { useSelector } from 'react-redux';

import MappingsToolbar from './MappingToolbar';
import MappingsTable from './MappingsTable';

import EditDialog from './dialogs/EditDialog';
import DownloadDialog from './dialogs/DownloadDialog';
import MappingSidebar from './MappingSidebar';

/** View to display and change mappings */
const AgentMappingView = () => {
  const readOnly = useSelector((state) => !state.authentication.permissions.write);

  const [mappingsFilter, setMappingsFilter] = useState('');
  const [mappingToEdit, setMappingToEdit] = useState(null);
  const [isEditDialogShown, setEditDialogShown] = useState(false);
  const [isDownloadDialogShown, setDownloadDialogShown] = useState(false);

  const showEditMappingDialog = (selectedMapping = null) => {
    setMappingToEdit(selectedMapping);
    setEditDialogShown(true);
  };

  const contentHeight = 'calc(100vh - 7rem)';
  return (
    <div className="this">
      <style jsx>{`
        .fixed-toolbar {
          position: fixed;
          top: 4rem;
          width: calc(100vw - 4rem);
        }
        .content {
          margin-top: 3rem;
          height: ${contentHeight};
          overflow: hidden;
        }
      `}</style>
      <div className="fixed-toolbar">
        <MappingsToolbar
          filterValue={mappingsFilter}
          onChangeFilter={setMappingsFilter}
          onAddNewMapping={showEditMappingDialog}
          onDownload={() => setDownloadDialogShown(true)}
          readOnly={readOnly}
        />
      </div>
      <div className="content">
        <MappingsTable
          filterValue={mappingsFilter}
          onEditMapping={showEditMappingDialog}
          onDuplicateMapping={showEditMappingDialog}
          maxHeight={`calc(${contentHeight} - 2.5em)`}
          readOnly={readOnly}
          sidebar={<MappingSidebar />}
        />
      </div>
      <EditDialog visible={isEditDialogShown} onHide={() => setEditDialogShown(false)} mapping={mappingToEdit} />
      <DownloadDialog visible={isDownloadDialogShown} onHide={() => setDownloadDialogShown(false)} />
    </div>
  );
};

export default AgentMappingView;
