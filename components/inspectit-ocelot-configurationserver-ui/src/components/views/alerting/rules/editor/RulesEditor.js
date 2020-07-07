import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { cloneDeep, remove, extend } from 'lodash';
import Notificationbar from '../../../../editor/Notificationbar';
import DescriptionSection from './DescriptionSection';
import VariableView from './VariableView';

/**
 * The RulesEditor component views alerting rules and templates and provides means to edit the content of an alerting rule.
 */
const RulesEditor = ({ availableTopics, content, isRule, mappedVars, readOnly, onContentChanged, onErrorStatusUpdate }) => {
  const [errornuousVariables, setErrornuousVariables] = useState({});

  useEffect(() => {
    onErrorStatusUpdate(0);
    setErrornuousVariables({});
  }, [content]);

  if (!content) {
    return <SelectionInformation hint="Select a rule to start editing." />;
  }

  return (
    <>
      <style jsx>{`
        .error.p-col-fixed {
          padding: 0.5rem 0 0;
        }
        .scroll-vars {
          overflow-y: auto;
        }
        .section-container {
          margin: 0 1rem 0;
          border: 1px solid #c8c8c8;
          flex-grow: 1;
        }
        .var-header {
          margin: 0 1rem 0;
          padding: 0.5rem 1rem 0.5rem;
          background-color: #eee;
          border: 1px solid #c8c8c8;
          font-weight: bold;
        }
        .time-details {
          display: flex;
          flex-direction: row;
          justify-content: space-between;
          color: #ccc;
          font-size: 0.75rem;
          padding: 0.25rem 1rem;
        }
      `}</style>
      <div className="time-details">
        {content.created && <div>Created: {new Date(content.created).toLocaleString()}</div>}
        {content.modified && <div>Updated: {new Date(content.modified).toLocaleString()}</div>}
      </div>
      <DescriptionSection
        value={content.description}
        updateValue={(value) => updateDescription(content, onContentChanged, value)}
        readOnly={readOnly}
      />
      <div className="var-header">Variables</div>
      <div className="scroll-vars section-container">
        {isRule && (
          <VariableView
            options={!availableTopics ? [] : availableTopics.map((t) => t.id)}
            name={'Notification Channel'}
            description={'The destination channel where notifications should be sent to when an alert is triggered by this rule.'}
            value={content.topic || 'Not defined'}
            type={'selection'}
            readOnly={readOnly}
            onVarUpdate={(name, value) => updateTopic(content, onContentChanged, value)}
            isDefault={!content.topic}
          />
        )}
        <VariablesPart
          selectionName={content ? content.id : ''}
          ruleVariables={mappedVars}
          readOnly={readOnly}
          errornuousVariables={errornuousVariables}
          onValueUpdate={(name, value) => updateVariable(content, mappedVars, onContentChanged, name, value)}
          onErrorUpdate={(name, value) => {
            setErrornuousVariables(extend(errornuousVariables, { [name]: value }));
            onErrorStatusUpdate(Object.entries(errornuousVariables).filter((keyValPair) => keyValPair[1] === true).length);
          }}
        />
      </div>
      {content.error && (
        <div className="error p-col-fixed">
          <Notificationbar text={content.error} isError={true} icon={'pi-exclamation-triangle'} />
        </div>
      )}
    </>
  );
};

/**
 * Placeholder component for the case that no content is selected.
 */
const SelectionInformation = ({ hint }) => {
  return (
    <div className="p-col">
      <style jsx>{`
        .this {
          flex-grow: 1;
          align-items: stretch;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          min-width: 760px;
        }
        .selection-information {
          display: flex;
          height: 100%;
          align-items: center;
          justify-content: center;
          color: #bbb;
        }
      `}</style>
      <div className="selection-information">
        <div>{hint}</div>
      </div>
    </div>
  );
};

/**
 * Lists all variables as components.
 */
const VariablesPart = ({ selectionName, ruleVariables, readOnly, errornuousVariables, onValueUpdate, onErrorUpdate }) => {
  return ruleVariables.map((variable) => (
    <VariableView
      key={selectionName + '-' + variable.name}
      name={variable.name}
      description={variable.description}
      value={variable.value !== undefined ? variable.value : variable.defaultValue}
      type={variable.type}
      readOnly={readOnly}
      hasError={errornuousVariables[variable.name] === true}
      onVarUpdate={onValueUpdate}
      onErrorStatusUpdate={onErrorUpdate}
      isDefault={variable.value === undefined}
    />
  ));
};

const updateTopic = (content, onContentChanged, value) => {
  var newContent = cloneDeep(content);
  newContent.topic = value;
  onContentChanged(newContent.id, newContent);
};

const updateDescription = (content, onContentChanged, value) => {
  var newContent = cloneDeep(content);
  newContent.description = value;
  onContentChanged(newContent.id, newContent);
};

const updateVariable = (content, mappedVars, onContentChanged, name, value) => {
  var newContent = cloneDeep(content);
  if (value === null && newContent.vars) {
    remove(newContent.vars, (v) => v.name === name);
  } else if (value !== null) {
    if (!newContent.vars) {
      newContent.vars = [];
    }
    remove(newContent.vars, (v) => v.name === name);
    const defaultVar = mappedVars.find((v) => v.name === name);
    const changedVar = { name: name, description: defaultVar.description, value: value, type: defaultVar.type };
    newContent.vars = [...newContent.vars, changedVar];
  }

  onContentChanged(newContent.id, newContent);
};

RulesEditor.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** The rule or template content to show (and edit) in this editor */
  content: PropTypes.object.isRequired,
  /** Whether the content is a rule content */
  isRule: PropTypes.bool,
  /** An array of variables mapped with default values from the template. */
  mappedVars: PropTypes.array.isRequired,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** Callback on content update */
  onContentChanged: PropTypes.func,
  /** Callback on error status update */
  onErrorStatusUpdate: PropTypes.func,
};

RulesEditor.defaultProps = {
  availableTopics: [],
  isRule: false,
  readOnly: true,
  onContentChanged: () => {},
  onErrorStatusUpdate: () => {},
};

export default RulesEditor;
