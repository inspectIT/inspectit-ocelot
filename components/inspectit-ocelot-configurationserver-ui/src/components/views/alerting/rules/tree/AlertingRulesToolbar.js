import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { alertingActions } from '../../../../../redux/ducks/alerting';

/**
 * The toolbar used in the alerting view's rules list.
 */
const AlertingRulesToolbar = ({
  loading,
  selectedRuleName,
  selectedTemplateName,
  readOnly,
  groupingOptions,
  onShowCreateRuleDialog,
  onShowDeleteRuleDialog,
  onGroupingChanged,
  onRefresh,
}) => {
  const tooltipOptions = {
    showDelay: 500,
    position: 'top',
  };

  const { groupByTemplates, groupByTopics } = groupingOptions;

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
            icon="pi pi-briefcase"
            className={!groupByTemplates && 'p-button-outlined'}
            tooltip="Toggle Template Grouping"
            onClick={() => onGroupingChanged({ groupByTemplates: !groupByTemplates, groupByTopics })}
          />
          <Button
            icon="pi pi-bars"
            className={!groupByTopics && 'p-button-outlined'}
            tooltip="Toggle Notification Channel Grouping"
            onClick={() => onGroupingChanged({ groupByTemplates, groupByTopics: !groupByTopics })}
          />
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
  /**  Name of the selected rule */
  selectedRuleName: PropTypes.string.isRequired,
  /**  Name of the selected template (template in the current context) */
  selectedTemplateName: PropTypes.string.isRequired,
  /** If the toolbar is in loading mode */
  loading: PropTypes.bool,
  /**  Whether the contents are read only */
  readOnly: PropTypes.bool,
  /**  Whether to group by templates and/or topics */
  groupingOptions: PropTypes.object,
  /**  Callback on triggering create rule dialog */
  onShowCreateRuleDialog: PropTypes.func,
  /**  Callback on triggering delete rule dialog */
  onShowDeleteRuleDialog: PropTypes.func,
  /**  Callback on triggering refresh */
  onRefresh: PropTypes.func,
  /** Global state update callback when rule grouping options change */
  onGroupingChanged: PropTypes.func,
};

AlertingRulesToolbar.defaultProps = {
  loading: false,
  readOnly: false,
  groupingOptions: {
    groupByTemplates: true,
    groupByTopics: false,
  },
  onShowCreateRuleDialog: () => {},
  onShowDeleteRuleDialog: () => {},
  onRefresh: () => {},
};

const mapStateToProps = (state) => {
  return {
    groupingOptions: state.alerting.ruleGrouping,
  };
};

const mapDispatchToProps = {
  onGroupingChanged: alertingActions.ruleGroupingOptionsChanged,
};

export default connect(mapStateToProps, mapDispatchToProps)(AlertingRulesToolbar);
