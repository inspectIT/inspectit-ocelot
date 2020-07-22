import React from 'react';
import PropTypes from 'prop-types';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

const AlertingChannelsTreeToolbar = ({
  readOnly,
  onShowDeleteDialog,
  onShowCreateDialog,
  onShowEditDialog,
  onShowCopyDialog,
  onRefresh,
  handlerSelected,
}) => {
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
        .this :global(.p-button-outlined) {
          color: #005b9f;
          background-color: rgba(0, 0, 0, 0);
        }
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <Button
            disabled={readOnly}
            tooltip="New handler"
            icon="pi pi-plus"
            tooltipOptions={tooltipOptions}
            onClick={onShowCreateDialog}
          />
          <Button
            disabled={readOnly || !handlerSelected}
            tooltip="Edit handler"
            icon="pi pi-pencil"
            tooltipOptions={tooltipOptions}
            onClick={onShowEditDialog}
          />
          <Button
            disabled={readOnly || !handlerSelected}
            tooltip="Copy handler"
            icon="pi pi-copy"
            tooltipOptions={tooltipOptions}
            onClick={onShowCopyDialog}
          />
          <Button
            disabled={readOnly || !handlerSelected}
            tooltip="Delete handler"
            icon="pi pi-trash"
            tooltipOptions={tooltipOptions}
            onClick={onShowDeleteDialog}
          />
        </div>
        <div className="p-toolbar-group-right">
          <Button onClick={onRefresh} tooltip="Reload" icon={'pi pi-refresh'} tooltipOptions={tooltipOptions} />
        </div>
      </Toolbar>
    </div>
  );
};

AlertingChannelsTreeToolbar.propTypes = {
  /** read only mode */
  readOnly: PropTypes.bool,
  onShowDeleteDialog: PropTypes.func.isRequired,
  onShowCreateDialog: PropTypes.func.isRequired,
  onShowEditDialog: PropTypes.func.isRequired,
  onShowCopyDialog: PropTypes.func.isRequired,
  onRefresh: PropTypes.func.isRequired,
  handlerSelected: PropTypes.bool.isRequired,
};

AlertingChannelsTreeToolbar.defaultProps = {
  readOnly: false,
};

export default AlertingChannelsTreeToolbar;
