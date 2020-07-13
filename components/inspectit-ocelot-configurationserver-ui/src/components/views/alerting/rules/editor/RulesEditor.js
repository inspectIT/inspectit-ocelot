import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { cloneDeep, remove, extend } from 'lodash';
import DescriptionSection from './DescriptionSection';
import VariableView from './VariableView';
import SelectionInformation from '../../../../editor/SelectionInformation';
import Section from '../../Section';
import dateformat from 'dateformat';

/**
 * The RulesEditor component views alerting rules and templates and provides means to edit the content of an alerting rule.
 */
const RulesEditor = ({ availableTopics, content, isRule, mappedVars, readOnly, onContentChanged, onErrorStatusUpdate }) => {
  // state variables
  const [errornuousVariables, setErrornuousVariables] = useState({});

  useEffect(() => {
    onErrorStatusUpdate(0);
    setErrornuousVariables({});
  }, [content]);

  if (!content) {
    return <SelectionInformation hint="Select a rule or template to start editing." />;
  }

  const contentName = content.id;
  const hasExecutionError = !!content.error;
  const format = 'yyyy-mm-dd HH:MM:ss';
  const creationDate = content.created ? dateformat(content.created, format) : '-';
  const modificationDate = content.modified ? dateformat(content.modified, format) : '-';

  const updateAttribute = (attribute, newValue) => {
    const newContent = cloneDeep(content);
    newContent[attribute] = newValue;
    onContentChanged(newContent);
  };

  const updateVariable = (name, value) => {
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

    onContentChanged(newContent);
  };

  const updateErrorStatus = (name, value) => {
    setErrornuousVariables(extend(errornuousVariables, { [name]: value }));
    onErrorStatusUpdate(Object.entries(errornuousVariables).filter((keyValPair) => keyValPair[1] === true).length);
  };

  let statusText;
  let statusIconClass;
  if (isRule) {
    if (content.status === 'enabled') {
      if (hasExecutionError) {
        statusText = 'Execution Error';
        statusIconClass = 'pi-times-circle red';
      } else {
        statusText = 'Enabled';
        statusIconClass = 'pi-circle-on green';
      }
    } else {
      statusText = 'Disabled';
      statusIconClass = 'pi-circle-off grey';
    }
  }

  return (
    <>
      <style jsx>{`
        .this {
          display: flex;
          flex-direction: column;
          overflow-y: auto;
          flex-grow: 1;
        }
        .this :global(.error.p-col-fixed) {
          padding: 0.5rem 0 0;
        }
        .detail-row {
          display: flex;
          align-items: center;
        }
        .detail-row:not(:first-child) {
          margin-top: 0.25rem;
        }
        .detail-row span {
          width: 10rem;
          display: inline-block;
          color: #333;
        }
        .detail-row .pi {
          margin-right: 0.5rem;
        }
        .error {
          font-family: monospace;
        }
        .green {
          color: #4caf50;
        }
        .grey {
          color: #9e9e9e;
        }
        .red {
          color: #f44336;
        }
      `}</style>

      <div className="this">
        {hasExecutionError && (
          <Section title={'Execution Error'} backgroundColor="#ff8181">
            <div className="error">{content.error}</div>
          </Section>

        )}

        <Section title={(isRule ? 'Rule' : 'Template') + ' Details'}>
          {isRule && (
            <div className="detail-row">
              <span>Status:</span> <i className={'pi ' + statusIconClass} /> {statusText}
            </div>
          )}
          <div className="detail-row">
            <span>Creation Date:</span> {creationDate}
          </div>
          <div className="detail-row">
            <span>Last Modification:</span> {modificationDate}
          </div>
        </Section>

        <DescriptionSection
          value={content.description}
          updateValue={(value) => updateAttribute('description', value)}
          readOnly={readOnly}
        />

        <Section title="Variables">
          {isRule && (
            <VariableView
              options={!availableTopics ? [] : availableTopics.map((topic) => topic.id)}
              name={'Notification Channel'}
              description={'The destination channel where notifications should be sent to when an alert is triggered by this rule.'}
              value={content.topic || 'Not defined'}
              type={'selection'}
              readOnly={readOnly}
              onVarUpdate={(name, value) => updateAttribute('topic', value)}
              isDefault={!content.topic}
            />
          )}

          {mappedVars.map((variable) => (
            <VariableView
              key={contentName + '-' + variable.name}
              name={variable.name}
              description={variable.description}
              value={variable.value !== undefined ? variable.value : variable.defaultValue}
              type={variable.type}
              readOnly={readOnly}
              hasError={errornuousVariables[variable.name]}
              onVarUpdate={updateVariable}
              onErrorStatusUpdate={updateErrorStatus}
              isDefault={variable.value === undefined}
            />
          ))}
        </Section>
      </div>
    </>
  );
};

RulesEditor.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** The rule or template content to show (and edit) in this editor */
  content: PropTypes.object,
  /** Whether the content is a rule content */
  isRule: PropTypes.bool,
  /** An array of variables mapped with default values from the template. */
  mappedVars: PropTypes.array,
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
  readOnly: false,
  onContentChanged: () => {},
  onErrorStatusUpdate: () => {},
};

export default RulesEditor;
