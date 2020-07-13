import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import AlertingRulesTreeContainer from './tree/AlertingRulesTreeContainer';
import RulesEditorContainer from './editor/RulesEditorContainer';

/**
 * The component for managing alerting rules.
 */
const AlertingRulesView = ({ updateDate, availableTopics, rules, templates, onRefresh }) => {
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
      `}</style>
      <AlertingRulesTreeContainer
        readOnly={readOnly}
        onSelectionChanged={(ruleName, templateName) => {
          setSelectedRuleName(ruleName);
          setSelectedTemplateName(templateName);
        }}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
        updateDate={updateDate}
        rules={rules}
        templates={templates}
        onRefresh={onRefresh}
      />
      <RulesEditorContainer
        readOnly={readOnly}
        availableTopics={availableTopics}
        selectedRuleName={selectedRuleName}
        selectedTemplateName={selectedTemplateName}
      />
    </div>
  );
};

AlertingRulesView.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** Recent update date */
  updateDate: PropTypes.object,
  /** List of available rules */
  rules: PropTypes.array,
  /** List of available templates */
  templates: PropTypes.array,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingRulesView.defaultProps = {
  availableTopics: [],
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

export default AlertingRulesView;
