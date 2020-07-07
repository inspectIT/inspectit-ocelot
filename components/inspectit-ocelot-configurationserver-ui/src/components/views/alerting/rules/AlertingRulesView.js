import React, { useState } from 'react';
import { connect } from 'react-redux';
import { useSelector } from 'react-redux';
import AlertingRulesTreeContainer from './tree/AlertingRulesTreeContainer';
import RulesEditorContainer from './editor/RulesEditorContainer';

/**
 * The component for managing alerting rules.
 */
const AlertingRulesView = ({ availableTopics, unsavedRules }) => {
  const readOnly = useSelector((state) => !state.authentication.permissions.write).readOnly;
  const [selectedRuleName, setSelectedRuleName] = useState(undefined);
  const [selectedTemplateName, setSelectedTemplateName] = useState(undefined);

  return (
    <div className="this">
      <style jsx>{`
            .this {
                height: 100%;
                display: flex;
                flex-grow: 1;
            }            
            .this :global(.editorContainer) {
              height: 100%;
              flex-grow: 1;
              align-items: stretch;
              display: flex;
              flex-direction: column;
              justify-content: flex-start;
              min-width: 760px;
            }
            `}</style>
      <AlertingRulesTreeContainer
        readOnly={readOnly}
        onSelectionChanged={(ruleName, templateName) => {
          setSelectedRuleName(ruleName);
          setSelectedTemplateName(templateName);
        }}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        unsavedRules={unsavedRules}
      />
      <RulesEditorContainer
        readOnly={readOnly}
        availableTopics={availableTopics}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        onRuleRenamed={(oldName, newName) => {
          if (oldName === selectedRuleName) {
            setSelectedRuleName(newName);
          }
        }}
      />
    </div>
  );
};

const mapStateToProps = (state) => {
  const { unsavedRuleContents } = state.alerting;

  return {
    unsavedRules: Object.keys(unsavedRuleContents)
  };
}

export default connect(mapStateToProps, {})(AlertingRulesView);
