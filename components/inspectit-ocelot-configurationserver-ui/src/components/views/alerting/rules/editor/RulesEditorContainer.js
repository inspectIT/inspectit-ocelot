import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { omit, isEqual, extend, cloneDeep } from 'lodash';
import RulesEditor from './RulesEditor';
import RulesEditorToolbar from './RulesEditorToolbar';
import * as rulesAPI from '../RulesAPI';
import { alertingActions } from '../../../../../redux/ducks/alerting';

const RulesEditorContainer = ({
  selectedRuleName,
  selectedTemplateName,
  readOnly,
  availableTopics,
  unsavedRuleContents,
  unsavedRuleContentsChanged,
}) => {
  const [numVariableErrors, setNumVariableErrors] = useState(0);
  const [ruleContent, setRuleContent] = useState(undefined);
  const [templateContent, setTemplateContent] = useState(undefined);

  useContentFetch(selectedRuleName, selectedTemplateName, setRuleContent, setTemplateContent);

  const onContentChanged = (ruleName, changedContent) => {
    if (isEqual(ruleContent, changedContent)) {
      unsavedRuleContentsChanged(omit(cloneDeep(unsavedRuleContents), ruleName));
    } else {
      unsavedRuleContentsChanged(extend(cloneDeep(unsavedRuleContents), { [ruleName]: changedContent }));
    }
  };

  const updateEnabledState = (value) => {
    var newContent = cloneDeep(content);
    newContent.status = value ? 'enabled' : 'disabled';
    onContentChanged(newContent.id, newContent);
  };

  let { content, isUnsaved, isRule } = getContentWrapper(ruleContent, templateContent, unsavedRuleContents);

  const mappedVars = getMappedVars(content, templateContent);
  const selectionNameAddition = isRule ? 'Base-Template: ' + selectedTemplateName + '' : undefined;
  return (
    <div className="this">
      <style jsx>{`
        .this {
          height: 100%;
          flex-grow: 1;
          align-items: stretch;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          min-width: 760px;
        }
      `}</style>
      <RulesEditorToolbar
        selectionName={content ? content.id : undefined}
        selectionNameAddition={selectionNameAddition}
        isRule={isRule}
        ruleEnabled={content && content.status === 'enabled'}
        savedRuleIsEnabled={ruleContent && ruleContent.status === 'enabled'}
        savedRuleHasError={ruleContent && !!ruleContent.error}
        isUnsaved={isUnsaved}
        readOnly={readOnly || !isRule}
        onEnabledStateChanged={updateEnabledState}
        variablesHaveErrors
        numErrors={numVariableErrors}
        onSave={() => {
          if (selectedRuleName in unsavedRuleContents) {
            rulesAPI.updateRule(
              unsavedRuleContents[selectedRuleName],
              (ruleContent) => {
                unsavedRuleContentsChanged(omit(unsavedRuleContents, selectedRuleName));
                setRuleContent(ruleContent);
              },
              () => setRuleContent(undefined)
            );
          }
        }}
      />
      <RulesEditor
        availableTopics={availableTopics}
        readOnly={readOnly || !isRule}
        content={content}
        mappedVars={mappedVars}
        isRule={isRule}
        onErrorStatusUpdate={(value) => setNumVariableErrors(value)}
        onContentChanged={onContentChanged}
      />
    </div>
  );
};

const useContentFetch = (selectedRuleName, selectedTemplateName, setRuleContent, setTemplateContent) => {
  useEffect(() => {
    if (!selectedTemplateName) {
      setTemplateContent(undefined);
    } else {
      rulesAPI.fetchTemplate(
        selectedTemplateName,
        (content) => setTemplateContent(content),
        () => setTemplateContent(undefined)
      );
    }
  }, [selectedTemplateName]);

  useEffect(() => {
    if (!selectedRuleName) {
      setRuleContent(undefined);
    } else {
      rulesAPI.fetchRule(
        selectedRuleName,
        (content) => setRuleContent(content),
        () => setRuleContent(undefined)
      );
    }
  }, [selectedRuleName]);
};

const getContentWrapper = (ruleContent, templateContent, unsavedRuleContents) => {
  if (ruleContent) {
    if (ruleContent.id in unsavedRuleContents) {
      const content = unsavedRuleContents[ruleContent.id];
      return { content: content, isUnsaved: true, isRule: true };
    } else {
      return { content: ruleContent, isUnsaved: false, isRule: true };
    }
  } else if (templateContent) {
    return { content: templateContent, isUnsaved: false, isRule: false };
  } else {
    return { content: undefined, isUnsaved: false, isRule: false };
  }
};

const getMappedVars = (ruleContent, templateContent) => {
  if (!templateContent) {
    return undefined;
  }

  const defaultVars = templateContent.vars;
  var ruleVariables = undefined;

  // map values from template content to default value properties and set assigned variable values
  if (defaultVars) {
    ruleVariables = defaultVars.map((defVar) => {
      var newVarObj = { ...defVar, ...{ defaultValue: defVar.value } };
      if (ruleContent && ruleContent.vars && ruleContent.vars.some((v) => v.name === defVar.name)) {
        newVarObj.value = ruleContent.vars.find((v) => v.name === defVar.name).value;
      } else {
        delete newVarObj.value;
      }
      return newVarObj;
    });
  }

  return ruleVariables;
};

RulesEditorContainer.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** The name of the selected rule */
  selectedRuleName: PropTypes.string.isRequired,
  /** The name of the template currently in the context */
  selectedTemplateName: PropTypes.string.isRequired,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** A map of rule names to corresponding unsaved contents */
  unsavedRuleContents: PropTypes.object,
  /** Callback on content update */
  unsavedRuleContentsChanged: PropTypes.func,
};

RulesEditorContainer.defaultProps = {
  availableTopics: [],
  unsavedRuleContents: {},
  readOnly: false,
  unsavedRuleContentsChanged: () => { },
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

export default connect(mapStateToProps, mapDispatchToProps)(RulesEditorContainer);
