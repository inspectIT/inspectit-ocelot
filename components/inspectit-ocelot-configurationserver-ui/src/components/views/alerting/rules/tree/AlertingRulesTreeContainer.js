import React, { useState, useEffect } from 'react';
import * as rulesAPI from '../RulesAPI';
import AlertingRulesToolbar from './AlertingRulesToolbar';
import AlertingRulesTree from './AlertingRulesTree';
import CreateDialog from '../../../../common/dialogs/CreateDialog';
import DeleteDialog from '../../../../common/dialogs/DeleteDialog';

/**
 * The container element of the alerting rules tree, the corresponding toolbar and the action dialogs.
 */
const AlertingRulesTreeContainer = ({ readOnly, onSelectionChanged, selectedRuleName, selectedTemplateName, unsavedRules }) => {
  const [updateDate, setUpdateDate] = useState(Date.now());
  const [isDeleteRuleDialogShown, setDeleteRuleDialogShown] = useState(false);
  const [isCreateRuleDialogShown, setCreateRuleDialogShown] = useState(false);
  const [rules, setRules] = useState(undefined);
  const [templates, setTemplates] = useState(undefined);

  const refreshRulesAndTemplates = () => {
    rulesAPI.fetchAlertingRules((rules) => setRules(rules), (error) => setRules([]));
    rulesAPI.fetchAlertingTemplates((templates) => setTemplates(templates), (error) => setTemplates([]));
    setUpdateDate(Date.now());
  };

  const ruleDeleted = (ruleName) => {
    refreshRulesAndTemplates();
    if (ruleName === selectedRuleName) {
      onSelectionChanged(undefined, selectedTemplateName);
    }
  };

  useEffect(() => {
    refreshRulesAndTemplates();
  }, [selectedRuleName, selectedTemplateName]);

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
        onShowDeleteRuleDialog={() => setDeleteRuleDialogShown(true)}
        onShowCreateRuleDialog={() => setCreateRuleDialogShown(true)}
        onShowRenameDialog={() => setRenameRuleDialogShown(true)}
        onRefresh={() => refreshRulesAndTemplates()}
        readOnly={readOnly}
      />
      <AlertingRulesTree
        rules={rules}
        templates={templates}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        unsavedRules={unsavedRules}
        onSelectionChanged={onSelectionChanged}
        readOnly={readOnly}
      />
      <div className="details">Last refresh: {updateDate ? new Date(updateDate).toLocaleString() : '-'}</div>
      <CreateDialog
        categories={templates && templates.map((t) => t.id)}
        useDescription={true}
        title={'Create Alerting Rule'}
        categoryTitle={'Template'}
        elementTitle={'Rule'}
        text={'Create an alerting rule:'}
        categoryIcon={'pi-briefcase'}
        targetElementIcon={'pi-bell'}
        reservedNames={rules && rules.map((r) => r.id)}
        visible={isCreateRuleDialogShown}
        onHide={() => setCreateRuleDialogShown(false)}
        initialCategory={selectedTemplateName}
        onSuccess={(ruleName, templateName, description) => {
          setCreateRuleDialogShown(false);
          rulesAPI.createRule(ruleName, templateName, description,
            (rule) => onSelectionChanged(rule.id, rule.template),
            () => onSelectionChanged(undefined, undefined));
        }}
      />
      <DeleteDialog
        visible={isDeleteRuleDialogShown}
        onHide={() => setDeleteRuleDialogShown(false)}
        name={selectedRuleName}
        text='Delete Rule'
        onSuccess={(ruleName) => rulesAPI.deleteRule(ruleName, (deletedRule) => ruleDeleted(deletedRule))}
      />
    </div>
  );
};

export default AlertingRulesTreeContainer;
