import React from 'react';
import PropTypes from 'prop-types';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

/**
 * The toolbar used in the alerting view's rules list.
 */
const AlertingRulesToolbar = ({
  loading,
  readOnly,
  onShowCreateRuleDialog,
  onShowDeleteRuleDialog,
  onShowRenameRuleDialog,
  onShowCopyRuleDialog,
  onRefresh,
  ruleSelected,
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
            disabled={readOnly || loading}
            tooltip="New rule"
            icon="pi pi-plus"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowCreateRuleDialog()}
          />
          <Button
            disabled={readOnly || loading || !ruleSelected}
            tooltip="Rename rule"
            icon="pi pi-pencil"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowRenameRuleDialog()}
          />
          <Button
            disabled={readOnly || loading || !ruleSelected}
            tooltip="Copy rule"
            icon="pi pi-copy"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowCopyRuleDialog()}
          />
          <Button
            disabled={readOnly || loading || !ruleSelected}
            tooltip="Delete rule"
            icon="pi pi-trash"
            tooltipOptions={tooltipOptions}
            onClick={() => onShowDeleteRuleDialog()}
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

AlertingRulesToolbar.propTypes = {
  /**  whether a rule is selected or not */
  isRuleSelected: PropTypes.bool,
  /** If the toolbar is in loading mode */
  loading: PropTypes.bool,
  /**  Whether the contents are read only */
  readOnly: PropTypes.bool,
  /**  Callback on triggering create rule dialog */
  onShowCreateRuleDialog: PropTypes.func,
  /**  Callback on triggering delete rule dialog */
  onShowDeleteRuleDialog: PropTypes.func,
  /**  Callback on triggering rename rule dialog */
  onShowRenameRuleDialog: PropTypes.func,
  /**  Callback on triggering copy rule dialog */
  onShowCopyRuleDialog: PropTypes.func,
  /**  Callback on triggering refresh */
  onRefresh: PropTypes.func,
};

AlertingRulesToolbar.defaultProps = {
  isRuleSelected: false,
  loading: false,
  readOnly: false,
  onShowCreateRuleDialog: () => {},
  onShowDeleteRuleDialog: () => {},
  onShowRenameRuleDialog: () => {},
  onShowCopyRuleDialog: () => {},
  onRefresh: () => {},
};

export default AlertingRulesToolbar;
