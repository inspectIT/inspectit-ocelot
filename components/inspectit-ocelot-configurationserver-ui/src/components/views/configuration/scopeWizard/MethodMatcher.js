import { Fieldset } from 'primereact/fieldset';
import { matcherTypes, methodVisibility, tooltipOptions } from './ScopeWizardConstants';
import { Checkbox } from 'primereact/checkbox';
import { RadioButton } from 'primereact/radiobutton';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';

const MethodMatcher = ({
  onMethodMatcherChange,
  selectedMethodMatchers,
  setSelectedMethodMatchers,
  setIsConstructor,
  isConstructor,
  methodMatcherType,
  setMethodMatcherType,
  isSelectedParameter,
  setIsSelectedParameter,
  parameterList,
  removeParameter,
  addParameter,
  parameterInput,
  setParameterInput,
}) => {
  let heightMethodFieldset = window.innerHeight * 0.55;

  return (
    <Fieldset legend="Method Matcher" style={{ paddingTop: 0, height: heightMethodFieldset }}>
      <div className="row-center row-margin meta-row">
        {methodVisibility.map((methodMatcher) => {
          return (
            <div key={methodMatcher.key} className="p-field-checkbox">
              <Checkbox
                inputId={methodMatcher.key}
                name="methodMatcher"
                value={methodMatcher}
                onChange={(e) => onMethodMatcherChange(e, selectedMethodMatchers, setSelectedMethodMatchers)}
                checked={selectedMethodMatchers.some((item) => item.key === methodMatcher.key)}
              />
              <label htmlFor={methodMatcher.key}>{methodMatcher.name}</label>
            </div>
          );
        })}
      </div>
      <div className="method-matcher-radio row-center meta-row">
        <div className="p-field-radiobutton">
          <RadioButton
            inputId="methodName"
            name="methodName"
            value="false"
            onChange={(e) => setIsConstructor(e.value)}
            checked={isConstructor === 'false'}
          />
          <label htmlFor="methodName">Method name</label>
        </div>
        <div className="p-field-radiobutton">
          <RadioButton
            inputId="constructor"
            name="constructor"
            value="true"
            onChange={(e) => setIsConstructor(e.value)}
            checked={isConstructor === 'true'}
          />
          <label htmlFor="constructor">Constructor</label>
        </div>
      </div>
      <div className="row-center meta-row fill">
        <Dropdown
          className="method-matcher-dropdown"
          value={methodMatcherType}
          options={matcherTypes}
          onChange={(e) => setMethodMatcherType(e.value)}
          placeholder="Select a Matcher Type"
        />
        <InputText className="in-name" />
      </div>
      <div className="p-field-checkbox row-center row-margin fill">
        <Checkbox
          inputId="selected-parameters"
          name="selectedParameters"
          value={isSelectedParameter}
          onChange={(e) => setIsSelectedParameter(e.checked)}
          checked={isSelectedParameter}
        />
        <label htmlFor={'onlyWithSelectedParameters'}>Only with selected Parameters</label>
      </div>
      <div className="parameterInputFields">
        <Fieldset legend="Method Arguments" style={{ paddingTop: 0, overflow: 'hidden' }}>
          {parameterList.map((parameter, i) => {
            return (
              <div key={i} className="row-center meta-row argument-fields-height ">
                <InputText className="in-name" disabled name="parameter" value={parameter.parameter} />
                <Button
                  tooltip="Remove Parameter"
                  icon="pi pi-fw pi-trash"
                  tooltipOptions={tooltipOptions}
                  onClick={() => removeParameter(i)}
                />
              </div>
            );
          })}
          <div className="row-center meta-row argument-fields-height" style={{ marginBottom: '0.5em' }}>
            <InputText name="parameter" className="in-name" value={parameterInput} onChange={(e) => setParameterInput(e.target.value)} />
            <Button tooltip="Add Parameter" icon="pi pi-plus" tooltipOptions={tooltipOptions} onClick={(e) => addParameter(e)} />
          </div>
        </Fieldset>
      </div>
    </Fieldset>
  );
};

export default MethodMatcher;
