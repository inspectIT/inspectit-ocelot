import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import AlertingRulesTreeContainer from './tree/AlertingRulesTreeContainer';
import RulesEditorContainer from './editor/RulesEditorContainer';

/**
 * The component for managing alerting rules.
 */
const AlertingRulesView = ({ updateDate, topics, rules, templates, onRefresh }) => {
  const [currentSelection, setCurrentSelection] = useState({ rule: null, template: null });

  const readOnly = useSelector((state) => !state.authentication.permissions.write);

  return (
    <>
      <style jsx>{`
        .this {
          height: 100%;
          display: flex;
          flex-grow: 1;
        }
      `}</style>

      <div className="this">
        <AlertingRulesTreeContainer
          readOnly={readOnly}
          onSelectionChanged={setCurrentSelection}
          updateDate={updateDate}
          rules={rules}
          templates={templates}
          onRefresh={onRefresh}
          selection={currentSelection}
        />

        <RulesEditorContainer readOnly={readOnly} availableTopics={topics} selection={currentSelection} />
      </div>
    </>
  );
};

AlertingRulesView.propTypes = {
  /** An array of strings denoting the available notification topics */
  topics: PropTypes.array,
  /** Recent update date */
  updateDate: PropTypes.number,
  /** List of available rules */
  rules: PropTypes.array,
  /** List of available templates */
  templates: PropTypes.array,
  /** Callback on refresh */
  onRefresh: PropTypes.func,
};

AlertingRulesView.defaultProps = {
  topics: [],
  onSelectionChanged: () => {},
  onRefresh: () => {},
};

export default AlertingRulesView;
