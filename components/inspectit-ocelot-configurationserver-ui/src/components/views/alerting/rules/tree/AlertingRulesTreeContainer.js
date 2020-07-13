import React, { useState } from 'react';
import { connect } from 'react-redux';
import { omit, extend } from 'lodash';
import PropTypes from 'prop-types';
import * as rulesAPI from '../RulesAPI';
import AlertingRulesToolbar from './AlertingRulesToolbar';
import AlertingRulesTree from './AlertingRulesTree';
import CreateDialog from './CreateDialog';
import DeleteDialog from '../../../../common/dialogs/DeleteDialog';
import RenameCopyDialog from '../../../../common/dialogs/RenameCopyDialog';
import { alertingActions } from '../../../../../redux/ducks/alerting';

/**
 * The container element of the alerting rules tree, the corresponding toolbar and the action dialogs.
 */
const AlertingRulesTreeContainer = ({
  rules,
  templates,
  updateDate,
  readOnly,
  onSelectionChanged,
  selectedRuleName,
  selectedTemplateName,
  unsavedRuleContents,
  onRefresh,
  unsavedRuleContentsChanged,
}) => {
  const [isDeleteRuleDialogShown, setDeleteRuleDialogShown] = useState(false);
  const [isCreateRuleDialogShown, setCreateRuleDialogShown] = useState(false);
  const [isRenameRuleDialogShown, setRenameRuleDialogShown] = useState(false);
  const [isCopyRuleDialogShown, setCopyRuleDialogShown] = useState(false);
  const [groupByTemplates, setGroupByTemplates] = useState(true);
  const [groupByTopics, setGroupByTopics] = useState(false);

  return (
    <div className="treeContainer">
      <style jsx>{`
        .treeContainer {
          height: 100%;
          display: flex;
          flex-direction: column;
          border-right: 1px solid #ddd;
        }
        .treeContainer :global(.p-tree) {
          height: 100%;
          border: 0;
          border-radius: 0;
          display: flex;
          flex-direction: column;
          background: 0;
        }
        .treeContainer :global(.details) {
          color: #ccc;
          font-size: 0.75rem;
          text-align: center;
          padding: 0.25rem 0;
        }
      `}</style>
      <AlertingRulesToolbar
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        groupByTemplates={groupByTemplates}
        groupByTopics={groupByTopics}
        onGroupingChanged={(gbTemplateValue, gbTopicValue) => {
          if (gbTemplateValue !== groupByTemplates) {
            setGroupByTemplates(gbTemplateValue);
          }
          if (gbTopicValue !== groupByTopics) {
            setGroupByTopics(gbTopicValue);
          }
        }}
        onShowDeleteRuleDialog={() => setDeleteRuleDialogShown(true)}
        onShowCreateRuleDialog={() => setCreateRuleDialogShown(true)}
        onShowRenameRuleDialog={() => setRenameRuleDialogShown(true)}
        onShowCopyRuleDialog={() => setCopyRuleDialogShown(true)}
        onRefresh={onRefresh}
        readOnly={readOnly}
      />
      <AlertingRulesTree
        rules={rules}
        templates={templates}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        unsavedRules={unsavedRuleContents && Object.keys(unsavedRuleContents)}
        onSelectionChanged={onSelectionChanged}
        readOnly={readOnly}
        groupByTemplates={groupByTemplates}
        groupByTopics={groupByTopics}
      />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateDialog
        templates={templates && templates.map((t) => t.id)}
        initialTemplate={selectedTemplateName}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCreateRuleDialogShown}
        onHide={() => setCreateRuleDialogShown(false)}
        onSuccess={(ruleName, templateName, description) => {
          setCreateRuleDialogShown(false);
          rulesAPI.createRule(
            ruleName,
            templateName,
            description,
            (rule) => onSelectionChanged(rule.id, rule.template),
            () => onSelectionChanged(undefined, undefined)
          );
        }}
      />
      <RenameCopyDialog
        name={selectedRuleName}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isRenameRuleDialogShown}
        onHide={() => setRenameRuleDialogShown(false)}
        text={'Rename alerting rule:'}
        onSuccess={(oldName, newName) => {
          setRenameRuleDialogShown(false);
          rulesAPI.renameRule(oldName, newName, () => {
            if (oldName in unsavedRuleContents) {
              const rContent = unsavedRuleContents[oldName];
              unsavedRuleContentsChanged(extend(omit(unsavedRuleContents, oldName), { [newName]: rContent }));
            }
            onSelectionChanged(newName, selectedTemplateName);
            onRefresh();
          });
        }}
        intention="rename"
      />
      <RenameCopyDialog
        name={selectedRuleName}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCopyRuleDialogShown}
        onHide={() => setCopyRuleDialogShown(false)}
        text={'Copy alerting rule:'}
        onSuccess={(oldName, newName) => {
          setCopyRuleDialogShown(false);
          rulesAPI.copyRule(oldName, newName, () => {
            onSelectionChanged(newName, selectedTemplateName);
            onRefresh();
          });
        }}
        intention="copy"
      />
      <DeleteDialog
        visible={isDeleteRuleDialogShown}
        onHide={() => setDeleteRuleDialogShown(false)}
        name={selectedRuleName}
        text="Delete Rule"
        onSuccess={(ruleName) =>
          rulesAPI.deleteRule(ruleName, (deletedRuleName) => {
            onRefresh();
            if (deletedRuleName === selectedRuleName) {
              onSelectionChanged(undefined, selectedTemplateName);
            }
          })
        }
      />
    </div>
  );
};

AlertingRulesTreeContainer.propTypes = {
  /** List of available rules */
  rules: PropTypes.array.isRequired,
  /** List of available templates */
  templates: PropTypes.array.isRequired,
  /** Recent update time */
  updateDate: PropTypes.object.isRequired,
  /**  Name of the selected rule */
  selectedRuleName: PropTypes.string.isRequired,
  /**  Name of the selected template (template in the current context) */
  selectedTemplateName: PropTypes.string.isRequired,
  /**  Whether the contents are read only */
  readOnly: PropTypes.bool,
  /**  Mapping of rules that are unsaved */
  unsavedRuleContents: PropTypes.array.isRequired,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
  /** Function to manipulate the global state of the unsaved rules */
  unsavedRuleContentsChanged: PropTypes.func,
};

AlertingRulesTreeContainer.defaultProps = {
  readOnly: false,
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

const mapStateToProps = (state) => {
  const { unsavedRuleContents } = state.alerting;

  return {
    unsavedRuleContents,
  };
};

const mapDispatchToProps = {
  unsavedRuleContentsChanged: alertingActions.ruleContentsChanged,
};

export default connect(mapStateToProps, mapDispatchToProps)(AlertingRulesTreeContainer);
