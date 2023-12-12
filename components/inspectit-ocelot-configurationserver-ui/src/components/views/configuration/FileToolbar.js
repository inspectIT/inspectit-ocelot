import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { configurationActions } from '../../../redux/ducks/configuration';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { SplitButton } from 'primereact/splitbutton';

/** Data */
import { CONFIGURATION_TYPES, TOOLTIP_OPTIONS } from '../../../data/constants';

/**
 * The toolbar used in the configuration view's file tree.
 */
const FileToolbar = ({
  readOnly,
  showCreateFileDialog,
  showCreateDirectoryDialog,
  showMoveDialog,
  showDeleteFileDialog,
  showUploadDialog,
  showSearchDialog,
  toggleShowHiddenFiles,
}) => {
  const dispatch = useDispatch();

  // global state variables
  const loading = useSelector((state) => state.configuration.pendingRequests) > 0;
  const selection = useSelector((state) => state.configuration.selection) || '';

  const showHiddenFiles = useSelector((state) => state.configuration.showHiddenFiles) || '';

  const reloadFiles = () => {
    dispatch(configurationActions.selectConfigurationVersion(null));
  };

  return (
    <div className="this">
      <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
          padding-top: 0.5rem;
          padding-bottom: 0.5rem;
        }

        .this :global(.p-toolbar-group-left > *) {
          margin-right: 0.25rem;
        }
        .this :global(.p-toolbar-group-right > *) {
          margin-left: 0.25rem;
        }

        .this :global(.p-toolbar-group-left .p-menu) {
          width: 15rem;
        }
      `}</style>

      <Toolbar
        left={
          <div className="p-toolbar-group-left">
            <SplitButton
              disabled={readOnly || loading}
              tooltip="New file"
              icon="pi pi-file"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showCreateFileDialog(selection, CONFIGURATION_TYPES.YAML)}
              model={[
                {
                  label: 'Method Configuration',
                  icon: 'pi pi-cog',
                  command: () => showCreateFileDialog(selection, CONFIGURATION_TYPES.METHOD_CONFIGURATION),
                },
              ]}
            />
            <Button
              disabled={readOnly || loading}
              tooltip="New directory"
              icon="pi pi-folder-open"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showCreateDirectoryDialog(selection)}
            />
            <Button
              disabled={readOnly || loading || !selection}
              tooltip="Move/Rename file or directory"
              icon="pi pi-pencil"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showMoveDialog(selection)}
            />
            <Button
              disabled={readOnly || loading || !selection}
              tooltip="Delete file or directory"
              icon="pi pi-trash"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showDeleteFileDialog(selection)}
            />
            <Button
              disabled={readOnly || loading}
              tooltip="Upload File or Directory"
              icon="pi pi-upload"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showUploadDialog(selection)}
            />
            <Button
              disabled={readOnly || loading}
              tooltip="Upload File or Directory"
              icon="pi pi-upload"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={() => showUploadDialog(selection)}
            />
          </div>
        }
        right={
          <div className="p-toolbar-group-right">
            <Button
              disabled={loading}
              onClick={toggleShowHiddenFiles}
              tooltip={showHiddenFiles ? 'Hide Files' : 'Show Hidden Files'}
              icon={showHiddenFiles ? 'pi pi-eye' : 'pi pi-eye-slash'}
              tooltipOptions={TOOLTIP_OPTIONS}
            />
            <Button
              disabled={loading}
              onClick={toggleShowHiddenFiles}
              tooltip={showHiddenFiles ? 'Hide Files' : 'Show Hidden Files'}
              icon={showHiddenFiles ? 'pi pi-eye' : 'pi pi-eye-slash'}
              tooltipOptions={TOOLTIP_OPTIONS}
            />
            <Button
              disabled={loading}
              onClick={showSearchDialog}
              tooltip="Find in File"
              icon={'pi pi-search'}
              tooltipOptions={TOOLTIP_OPTIONS}
            />
            <Button
              onClick={reloadFiles}
              tooltip="Reload"
              icon={'pi pi-refresh' + (loading ? 'pi-spin' : '')}
              tooltipOptions={TOOLTIP_OPTIONS}
            />
          </div>
        }
      />
    </div>
  );
};

export default FileToolbar;
