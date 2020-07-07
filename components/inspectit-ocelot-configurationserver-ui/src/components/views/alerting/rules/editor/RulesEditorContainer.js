import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import { omit, isEqual, extend, cloneDeep } from 'lodash';
import RulesEditor from './RulesEditor';
import RulesEditorToolbar from './RulesEditorToolbar';
import * as rulesAPI from '../RulesAPI';
import { alertingActions } from '../../../../../redux/ducks/alerting';


const RulesEditorContainer = ({ selectedRuleName, selectedTemplateName, readOnly, availableTopics, onRuleRenamed, unsavedRuleContents, unsavedRuleContentsChanged }) => {
  const [numVariableErrors, setNumVariableErrors] = useState(0);
  const [ruleContent, setRuleContent] = useState(undefined);
  const [templateContent, setTemplateContent] = useState(undefined);

  useContentFetch(selectedRuleName, selectedTemplateName, setRuleContent, setTemplateContent);

  const onContentChanged = (ruleName, changedContent) => {
    if (isEqual(ruleContent, changedContent)) {
      unsavedRuleContentsChanged(omit(unsavedRuleContents, ruleName));
    } else {
      unsavedRuleContentsChanged(extend(unsavedRuleContents, { [ruleName]: changedContent }));
    }
  };

  const renameRule = (oldName, newName) => {
    rulesAPI.renameRule(oldName, newName, () => {
      if (oldName in unsavedRuleContents) {
        rContent = unsavedRuleContents[oldName];
        unsavedRuleContentsChanged(extend(omit(unsavedRuleContents, oldName), { [newName]: rContent }));
      }
      onRuleRenamed(oldName, newName);
    });
  };

  const updateEnabledState = (value) => {
    var newContent = cloneDeep(content);
    newContent.status = value ? 'enabled' : 'disabled';
    onContentChanged(newContent.id, newContent);
  }

  const { content, isUnsaved, isRule } = getContentWrapper(ruleContent, templateContent, unsavedRuleContents);

  const mappedVars = getMappedVars(content, templateContent);

  return (

    <div className='this'>
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
        isRule={isRule}
        ruleEnabled={content && content.status === 'enabled'}
        savedRuleIsEnabled={ruleContent && ruleContent.status === 'enabled'}
        savedRuleHasError={ruleContent && !!ruleContent.error}
        isUnsaved={isUnsaved}
        readOnly={readOnly || !isRule}
        onEnabledStateChanged={updateEnabledState}
        onNameChanged={renameRule}
        variablesHaveErrors
        numErrors={numVariableErrors}
        onSave={() => {
          if (selectedRuleName in unsavedRuleContents) {
            rulesAPI.updateRule(unsavedRuleContents[selectedRuleName], (ruleContent) => {
              unsavedRuleContentsChanged(omit(unsavedRuleContents, selectedRuleName));
              setRuleContent(ruleContent);
            }, () => setRuleContent(undefined));
          }
        }}
      />
      <RulesEditor
        availableTopics={availableTopics}
        readOnly={readOnly || !isRule}
        content={content}
        mappedVars={mappedVars}
        isRule={isRule}
        hint={'Select a rule to start editing.'}
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
      rulesAPI.fetchTemplate(selectedTemplateName, (content) => setTemplateContent(content), () => setTemplateContent(undefined));
    }
  }, [selectedTemplateName]);

  useEffect(() => {
    if (!selectedRuleName) {
      setRuleContent(undefined);
    } else {
      rulesAPI.fetchRule(selectedRuleName, (content) => setRuleContent(content), () => setRuleContent(undefined));
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
    ruleVariables = defaultVars.map(defVar => {
      var newVarObj = { ...defVar, ...{ defaultValue: defVar.value } }
      if (ruleContent && ruleContent.vars && ruleContent.vars.some(v => v.name === defVar.name)) {
        newVarObj.value = ruleContent.vars.find(v => v.name === defVar.name).value;
      } else {
        delete newVarObj.value;
      }
      return newVarObj;
    });
  }

  return ruleVariables;
};

const mapStateToProps = (state) => {
  const { unsavedRuleContents } = state.alerting;

  return {
    unsavedRuleContents
  };
}

const mapDispatchToProps = {
  unsavedRuleContentsChanged: alertingActions.ruleContentsChanged,
};

export default connect(mapStateToProps, mapDispatchToProps)(RulesEditorContainer);