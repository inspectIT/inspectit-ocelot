import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import MappingsToolbar from './MappingToolbar';
import MappingsTable from './MappingsTable';

import EditDialog from './dialogs/EditDialog';
import DownloadDialog from './dialogs/DownloadDialog';
import MappingSidebar from './MappingSidebar';
import { mappingsActions, mappingsSelectors } from '../../../redux/ducks/mappings';

/** View to display and change mappings */
const AgentMappingView = () => {
  const dispatch = useDispatch();
  let readOnly = useSelector((state) => !state.authentication.permissions.write);

  const [mappingsFilter, setMappingsFilter] = useState('');
  const [mappingToEdit, setMappingToEdit] = useState(null);
  const [isEditDialogShown, setEditDialogShown] = useState(false);
  const [isDownloadDialogShown, setDownloadDialogShown] = useState(false);

  // global state variables
  const currentVersion = useSelector((state) => state.mappings.selectedVersion);
  const isLatest = useSelector(mappingsSelectors.isLatestVersion);

  // derived variables
  const isLiveSelected = currentVersion === 'live';

  const selectLatestVersion = () => {
    dispatch(mappingsActions.selectMappingsVersion(null));
  };
  const showEditMappingDialog = (selectedMapping = null) => {
    setMappingToEdit(selectedMapping);
    setEditDialogShown(true);
  };

  const contentHeight = 'calc(100vh - 7rem)';
  // Disable editing, if not latest workspace is selected
  readOnly = !isLatest ? true : readOnly;
  return (
    <div className="this">
      <style jsx>{`
        .fixed-toolbar {
          position: static;
          top: 4rem;
          width: calc(100vw - 5rem);
        }
        .content {
          height: ${contentHeight};
          overflow: hidden;
        }
        .version-notice {
          background-color: #ffcc80;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0.5rem 1rem;
          border-bottom: 1px solid #dddddd;
        }
        .version-notice i {
          margin-right: 1rem;
          color: #212121;
        }
        .gotoLatest {
          margin-left: 1rem;
          color: #007ad9;
          text-decoration: underline;
          cursor: pointer;
          white-space: nowrap;
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
      {!isLatest && (
        <div className="version-notice">
          <i className="pi pi-info-circle" />
          {isLiveSelected ? (
            <div>
              You are viewing the latest <b>live</b> agent mappings. Modifications are only possible on the <b>latest workspace</b> agent
              mappings.
            </div>
          ) : (
            <div>
              You are viewing not the latest workspace agent mappings. Modifications are only possible on the <b>latest workspace</b> agent
              mappings.
            </div>
          )}
          <div className="gotoLatest" onClick={selectLatestVersion}>
            Go to latest workspace
          </div>
        </div>
      )}
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
