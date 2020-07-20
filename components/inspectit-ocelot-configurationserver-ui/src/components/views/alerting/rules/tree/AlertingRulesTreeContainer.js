import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { omit, extend } from 'lodash';
import PropTypes from 'prop-types';
import * as rulesAPI from '../RulesAPI';
import AlertingRulesToolbar from './AlertingRulesToolbar';
import AlertingRulesTree from './AlertingRulesTree';
import CreateRuleDialog from './CreateRuleDialog';
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
  onRefresh,

  selection,
}) => {
  const dispatch = useDispatch();

  const [isDeleteRuleDialogShown, setDeleteRuleDialogShown] = useState(false);
  const [isCreateRuleDialogShown, setCreateRuleDialogShown] = useState(false);
  const [isRenameRuleDialogShown, setRenameRuleDialogShown] = useState(false);
  const [isCopyRuleDialogShown, setCopyRuleDialogShown] = useState(false);

  // global state variables
  const unsavedRuleContents = useSelector((state) => state.alerting.unsavedRuleContents);

  const isRuleSelected = !!selection.rule;

  const createRule = (ruleName, templateName, description) => {
    setCreateRuleDialogShown(false);
    rulesAPI.createRule(
      ruleName,
      templateName,
      description,
      (rule) => onSelectionChanged({ rule: rule.id, template: rule.template }),
      () => onSelectionChanged({ rule: null, template: null })
    );
  };

  const renameRule = (oldName, newName) => {
    setRenameRuleDialogShown(false);
    rulesAPI.renameRule(oldName, newName, () => {
      if (oldName in unsavedRuleContents) {
        const rContent = unsavedRuleContents[oldName];
        dispatch(alertingActions.ruleContentsChanged(extend(omit(unsavedRuleContents, oldName), { [newName]: rContent })));
      }
      onSelectionChanged({ rule: newName, template: selection.template });
      onRefresh();
    });
  };

  const copyRule = (oldName, newName) => {
    setCopyRuleDialogShown(false);
    rulesAPI.copyRule(oldName, newName, () => {
      onSelectionChanged({ rule: newName, template: selection.template });
      onRefresh();
    });
  };

  const deleteRule = (ruleName) => {
    rulesAPI.deleteRule(ruleName, (deletedRuleName) => {
      onRefresh();
      if (deletedRuleName === selection.rule) {
        onSelectionChanged({ rule: null, template: selection.template });
      }
    });
  };

  return (
    <>
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

      <div className="treeContainer">
        <AlertingRulesToolbar
          onShowDeleteRuleDialog={() => setDeleteRuleDialogShown(true)}
          onShowCreateRuleDialog={() => setCreateRuleDialogShown(true)}
          onShowRenameRuleDialog={() => setRenameRuleDialogShown(true)}
          onShowCopyRuleDialog={() => setCopyRuleDialogShown(true)}
          onRefresh={onRefresh}
          readOnly={readOnly}
          ruleSelected={isRuleSelected}
        />
        <AlertingRulesTree
          rules={rules}
          templates={templates}
          unsavedRules={unsavedRuleContents && Object.keys(unsavedRuleContents)}
          onSelectionChanged={onSelectionChanged}
          selection={selection}
        />
        <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      </div>

      <CreateRuleDialog
        templates={templates && templates.map((t) => t.id)}
        initialTemplate={selection.template}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCreateRuleDialogShown}
        onHide={() => setCreateRuleDialogShown(false)}
        onSuccess={createRule}
      />
      <RenameCopyDialog
        name={selection.rule}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isRenameRuleDialogShown}
        onHide={() => setRenameRuleDialogShown(false)}
        text={'Rename alerting rule:'}
        onSuccess={renameRule}
        intention="rename"
      />
      <RenameCopyDialog
        name={selection.rule}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCopyRuleDialogShown}
        onHide={() => setCopyRuleDialogShown(false)}
        text={'Copy alerting rule:'}
        onSuccess={copyRule}
        intention="copy"
      />
      <DeleteDialog
        visible={isDeleteRuleDialogShown}
        onHide={() => setDeleteRuleDialogShown(false)}
        name={selection.rule}
        text="Delete Rule"
        onSuccess={deleteRule}
      />
    </>
  );
};

AlertingRulesTreeContainer.propTypes = {
  /** List of available rules */
  rules: PropTypes.array.isRequired,
  /** List of available templates */
  templates: PropTypes.array.isRequired,
  /** Recent update time */
  updateDate: PropTypes.number,
  /**  Whether the contents are read only */
  readOnly: PropTypes.bool,
  /**  Callback on changed selection */
  onSelectionChanged: PropTypes.func,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingRulesTreeContainer.defaultProps = {
  readOnly: false,
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

export default AlertingRulesTreeContainer;
