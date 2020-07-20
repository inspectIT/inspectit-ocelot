import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import _, { omit, isEqual, extend, cloneDeep } from 'lodash';
import RulesEditor from './RulesEditor';
import RulesEditorToolbar from './RulesEditorToolbar';
import * as rulesAPI from '../RulesAPI';
import { alertingActions } from '../../../../../redux/ducks/alerting';
import DefaultToolbar from '../../DefaultToolbar';
import Notificationbar from '../../../../editor/Notificationbar';
import useDeepEffect from '../../../../../hooks/use-deep-effect';
import { templateIcon } from '../../constants';

const RulesEditorContainer = ({ readOnly, availableTopics, selection }) => {
  const dispatch = useDispatch();

  // state variables
  const [errorCount, setErrorCount] = useState(0);
  const [ruleContent, setRuleContent] = useState(undefined);
  const [templateContent, setTemplateContent] = useState(undefined);

  // global state variables
  const unsavedRuleContents = useSelector((state) => state.alerting.unsavedRuleContents);

  // fetching data - its done in a single effect to prevent "flickering", otherwise in case a rule
  // is selected, it can happen that the screen first renders the template because the
  // rule data is not loaded, yet
  useDeepEffect(() => {
    const fetchData = async () => {
      let newRuleContent;
      let newTemplateContent;

      if (selection.template) {
        try {
          newTemplateContent = await rulesAPI.fetchTemplate(selection.template);
        } catch (error) {
          console.log(error);
        }
      }
      if (selection.rule) {
        try {
          newRuleContent = await rulesAPI.fetchRule(selection.rule);
        } catch (error) {
          console.log(error);
        }
      }
      setTemplateContent(newTemplateContent);
      setRuleContent(newRuleContent);
    };

    fetchData();
  }, [selection]);

  // stores the given rule content as unsaved chagnes
  const onContentChanged = (newContent) => {
    const contentId = newContent.id;

    let data;
    if (isEqual(ruleContent, newContent)) {
      data = omit(cloneDeep(unsavedRuleContents), contentId);
    } else {
      data = extend(cloneDeep(unsavedRuleContents), { [contentId]: newContent });
    }
    dispatch(alertingActions.ruleContentsChanged(data));
  };

  const updateEnabledState = (value) => {
    var newContent = cloneDeep(content);
    newContent.status = value ? 'enabled' : 'disabled';
    onContentChanged(newContent);
  };

  // callback when the user hits the save button
  const onSave = () => {
    if (selection.rule in unsavedRuleContents) {
      rulesAPI.updateRule(
        unsavedRuleContents[selection.rule],
        (ruleContent) => {
          const unsavedRuleData = omit(unsavedRuleContents, selection.rule);
          dispatch(alertingActions.ruleContentsChanged(unsavedRuleData));

          setRuleContent(ruleContent);
        },
        () => setRuleContent(undefined)
      );
    }
  };

  const { content, isUnsaved, isRule } = getContentWrapper(ruleContent, templateContent, unsavedRuleContents);
  const currentName = _.get(content, 'id');

  const mappedVars = getVariablesWithDefaults(content, templateContent);

  return (
    <>
      <style jsx>{`
        .this {
          height: calc(100vh - 7rem);
          flex-grow: 1;
          align-items: stretch;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          min-width: 760px;
        }
      `}</style>

      <div className="this">
        {content && isRule ? (
          <RulesEditorToolbar
            ruleName={currentName}
            templateName={selection.template}
            ruleEnabled={content.status === 'enabled'}
            isUnsaved={isUnsaved}
            readOnly={readOnly}
            onEnabledStateChanged={updateEnabledState}
            numErrors={errorCount}
            onSave={onSave}
          />
        ) : (
          <DefaultToolbar name={currentName} icon={currentName ? templateIcon : ''} />
        )}

        <RulesEditor
          availableTopics={availableTopics}
          readOnly={readOnly || !isRule}
          content={content}
          mappedVars={mappedVars}
          isRule={isRule}
          onErrorStatusUpdate={setErrorCount}
          onContentChanged={onContentChanged}
        />

        {errorCount > 0 && (
          <Notificationbar
            text={'You have ' + errorCount + ' error(s) in your variable specification!'}
            isError={true}
            icon={'pi-exclamation-triangle'}
          />
        )}
      </div>
    </>
  );
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

const getVariablesWithDefaults = (ruleContent, templateContent) => {
  if (!templateContent || !templateContent.vars) {
    return null;
  }

  const varsWithDefault = _.map(templateContent.vars, (defaultVar) => {
    const result = {
      ...defaultVar,
      defaultValue: defaultVar.value,
    };

    const ruleVariable = _.find(ruleContent.vars, { name: defaultVar.name });
    if (ruleVariable) {
      result.value = ruleVariable.value;
    } else {
      delete result.value;
    }

    return result;
  });

  return varsWithDefault;
};

RulesEditorContainer.propTypes = {
  /** An array of strings denoting the available notification topics */
  availableTopics: PropTypes.array,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,

  selection: PropTypes.object,
};

RulesEditorContainer.defaultProps = {
  availableTopics: [],
  unsavedRuleContents: {},
  readOnly: false,
};

export default RulesEditorContainer;
