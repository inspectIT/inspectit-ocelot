import React from 'react';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used in the alerting view's rules list.
 */
const AlertingRulesToolbar = ({ loading, selectedRuleName, selectedTemplateName, readOnly, onShowCreateRuleDialog, onShowRenameDialog, onShowDeleteRuleDialog, onRefresh }) => {
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
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <Button
            disabled={readOnly || loading}
            tooltip="New rule"
            icon="pi pi-plus"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowCreateRuleDialog(selectedTemplateName)}
          />
          <Button
            disabled={readOnly || loading || !selectedRuleName}
            tooltip="Delete rule"
            icon="pi pi-trash"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowDeleteRuleDialog(selectedRuleName)}
          />
        </div>
        <div className="p-toolbar-group-right">
          <Button
            disabled={loading}
            onClick={() => onRefresh()}
            tooltip="Reload"
            icon={'pi pi-refresh' + (loading ? ' pi-spin' : '')}
            tooltipOptions={tooltipOptions}
          />
        </div>
      </Toolbar>
    </div>
  );
};

export default AlertingRulesToolbar;