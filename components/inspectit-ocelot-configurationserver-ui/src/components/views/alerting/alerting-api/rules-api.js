import axiosBearer from '../../../../lib/axios-api';
import { cloneDeep } from 'lodash';
import { fetchTemplate } from './templates-api';
import { fetch } from './common';

export const fetchRules = () => {
  return fetch('/alert/kapacitor/tasks');
};

export const fetchRule = (ruleName) => {
  return fetch(`/alert/kapacitor/tasks/${ruleName}`);
};

export const createRule = (ruleName, templateName, description) => {
  return fetchTemplate(templateName)
    .then((template) => {
      if (template) {
        const initialVars = template.vars
          ? template.vars
              .filter((variable) => variable.value === null || variable.value === undefined)
              .map((variable) => varWithDefaultValue(variable))
          : [];

        return {
          id: ruleName,
          template: templateName,
          description: description,
          topic: '',
          vars: initialVars,
        };
      } else {
        return null;
      }
    })
    .then((ruleObject) => {
      return ruleObject ? axiosBearer.post('/alert/kapacitor/tasks/', ruleObject).then((res) => res.data) : null;
    });
};

const varWithDefaultValue = (variable) => {
  let newVar = cloneDeep(variable);
  switch (variable.type) {
    case 'int':
      newVar.value = 1;
      break;
    case 'float':
      newVar.value = 1.0;
      break;
    case 'bool':
      newVar.value = false;
      break;
    case 'duration':
      newVar.value = '1m';
      break;
    default:
      newVar.value = '';
      break;
  }
  return newVar;
};

export const updateRule = (ruleObject, oldId) => {
  const ruleId = oldId ? oldId : ruleObject.id;
  return axiosBearer.patch(`/alert/kapacitor/tasks/${ruleId}`, ruleObject).then((res) => res.data);
};

export const deleteRule = (ruleName) => {
  return axiosBearer.delete(`/alert/kapacitor/tasks/${ruleName}`);
};

export const copyRule = (sourceName, targetName) => {
  return fetchRule(sourceName)
    .then((rule) => {
      let newRule = cloneDeep(rule);
      newRule.id = targetName;
      return newRule;
    })
    .then((newRule) => axiosBearer.post('/alert/kapacitor/tasks/', newRule));
};

export const renameRule = (oldName, newName) => {
  return fetchRule(oldName)
    .then((rule) => {
      let newRule = cloneDeep(rule);
      newRule.id = newName;
      return newRule;
    })
    .then((newRule) => {
      return updateRule(newRule, oldName);
    });
};
