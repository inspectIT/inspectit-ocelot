import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../redux/ducks/configuration';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used in the configuration view's file tree.
 */
const FileToolbar = ({
  readOnly,
  showCreateFileDialog,
  showCreateDirectoryDialog,
  showMoveDialog,
  showDeleteFileDialog,
  showSearchDialog,
}) => {
  const dispatch = useDispatch();

  // global state variables
  const loading = useSelector((state) => state.configuration.pendingRequests) > 0;
  const selection = useSelector((state) => state.configuration.selection) || '';

  const reloadFiles = () => {
    dispatch(configurationActions.selectVersion(null));
  };

  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  return (
    <div className="this">
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
        .this :global(.p-toolbar-group-right) :global(.p-button) {
          margin-left: 0.25rem;
        }
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <Button
            disabled={readOnly || loading}
            tooltip="New file"
            icon="pi pi-file"
            tooltipOptions={tooltipOptions}
            onClick={() => showCreateFileDialog(selection)}
          />
          <Button
            disabled={readOnly || loading}
            tooltip="New directory"
            icon="pi pi-folder-open"
            tooltipOptions={tooltipOptions}
            onClick={() => showCreateDirectoryDialog(selection)}
          />
          <Button
            disabled={readOnly || loading || !selection}
            tooltip="Move/Rename file or directory"
            icon="pi pi-pencil"
            tooltipOptions={tooltipOptions}
            onClick={() => showMoveDialog(selection)}
          />
          <Button
            disabled={readOnly || loading || !selection}
            tooltip="Delete file or directory"
            icon="pi pi-trash"
            tooltipOptions={tooltipOptions}
            onClick={() => showDeleteFileDialog(selection)}
          />
        </div>
        <div className="p-toolbar-group-right">
          <Button
            disabled={loading}
            onClick={showSearchDialog}
            tooltip="Find in File"
            icon={'pi pi-search'}
            tooltipOptions={tooltipOptions}
          />
          <Button
            onClick={reloadFiles}
            tooltip="Reload"
            icon={'pi pi-refresh' + (loading ? 'pi-spin' : '')}
            tooltipOptions={tooltipOptions}
          />
        </div>
      </Toolbar>
    </div>
  );
};

export default FileToolbar;
