import { Fieldset } from 'primereact/fieldset';
import { Checkbox } from 'primereact/checkbox';
import { RadioButton } from 'primereact/radiobutton';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';

/** data */
import { matcherTypes, methodVisibility, tooltipOptions } from './ScopeWizardConstants';

const MethodMatcher = ({ methodMatcher, onMethodMatcherChange }) => {
  const disableArguments = !methodMatcher.isSelectedParameter;

  const setState = (stateArgument, value) => {
    const currentMethodMatcher = {
      ...methodMatcher,
      [stateArgument]: value,
    };

    onMethodMatcherChange(currentMethodMatcher);
  };

  const onMethodVisibilityChange = (e, selectedMethodVisibilities) => {
    const currentSelectedMethodVisibilities = selectedMethodVisibilities;

    if (e.checked) {
      currentSelectedMethodVisibilities.push(e.value);
    } else {
      for (let i = 0; i < currentSelectedMethodVisibilities.length; i++) {
        const currentSelectedMethodVisibility = currentSelectedMethodVisibilities[i];

        if (currentSelectedMethodVisibility.key === e.value.key) {
          currentSelectedMethodVisibilities.splice(i, 1);
          break;
        }
      }
    }

    setState('selectedMethodVisibilities', currentSelectedMethodVisibilities);
  };

  const handleParameterChange = (e, index) => {
    const currentParameterList = methodMatcher.parameterList;
    currentParameterList[index].parameter = e.target.value;

    setState('parameterList', currentParameterList);
  };

  const removeParameter = (index) => {
    const currentParameterList = methodMatcher.parameterList;
    currentParameterList.splice(index, 1);

    setState('parameterList', currentParameterList);
  };

  const addParameter = () => {
    const currentParameterList = methodMatcher.parameterList;
    currentParameterList.push({ parameter: methodMatcher.parameterInput });

    setState('parameterList', currentParameterList);
    setState('parameterInput', '');
  };

  return (
    <>
      <style jsx>{`
        .this :global(.p-dialog-content) {
          border-left: 1px solid #ddd;
          border-right: 1px solid #ddd;
        }

        .row-center {
          display: flex;
          align-items: center;
        }

        .fill {
          flex-grow: 1;
        }

        .meta-row label {
          margin-right: 2rem;
        }

        .row-margin {
          margin-top: 0.5rem;
        }

        .argument-fields-height {
          margin-top: 0.5rem;
        }

        .this :global(.method-matcher-dropdown) {
          width: 14rem;
        }

        .this :global(.in-name) {
          width: 100%;
        }
      `}</style>

      <Fieldset legend="Method Matcher" style={{ paddingTop: 0, paddingBottom: '1rem' }}>
        <div className="row-center row-margin meta-row">
          {methodVisibility.map((methodVisible) => {
            return (
              <div key={methodVisible.key} className="p-field-checkbox">
                <Checkbox
                  inputId={methodVisible.key}
                  name="methodVisible"
                  value={methodVisible}
                  onChange={(e) => onMethodVisibilityChange(e, methodMatcher.selectedMethodVisibilities)}
                  checked={methodMatcher.selectedMethodVisibilities.some((item) => item.key === methodVisible.key)}
                />
                <label htmlFor={methodVisible.key}>{methodVisible.name}</label>
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
              onChange={(e) => setState('isConstructor', e.value)}
              checked={methodMatcher.isConstructor === 'false'}
            />
            <label htmlFor="methodName">Method name</label>
          </div>
          <div className="p-field-radiobutton">
            <RadioButton
              inputId="constructor"
              name="constructor"
              value="true"
              onChange={(e) => setState('isConstructor', e.value)}
              checked={methodMatcher.isConstructor === 'true'}
            />
            <label htmlFor="constructor">Constructor</label>
          </div>
        </div>
        <div className="row-center meta-row fill">
          <Dropdown
            className="method-matcher-dropdown"
            value={methodMatcher.methodMatcherType}
            options={matcherTypes}
            onChange={(e) => setState('methodMatcherType', e.value)}
            placeholder="Select a Matcher Type"
          />
          <InputText className="in-name" />
        </div>
        <div className="p-field-checkbox row-center row-margin fill" style={{ paddingTop: '1rem' }}>
          <Checkbox
            inputId="selected-parameters"
            name="selectedParameters"
            value={methodMatcher.isSelectedParameter}
            onChange={(e) => setState('isSelectedParameter', e.checked)}
            checked={methodMatcher.isSelectedParameter}
          />
          <label htmlFor={'onlyWithSelectedParameters'}>Only with specified arguments:</label>
        </div>
        <div className="parameterInputFields">
          <div style={{ paddingTop: 0, overflowY: 'auto', height: '15rem' }}>
            <label>Method Arguments</label>
            {methodMatcher.parameterList.map((parameter, i) => {
              return (
                <div key={i} className="p-inputgroup row-center meta-row argument-fields-height">
                  <span className="p-inputgroup-addon">{i + 1}</span>
                  <InputText
                    disabled={disableArguments}
                    className="in-name"
                    name="parameter"
                    value={parameter.parameter}
                    tooltip={i + 1 + '. argument'}
                    tooltipOptions={tooltipOptions}
                    onChange={(e) => handleParameterChange(e, i)}
                  />
                  <Button
                    disabled={disableArguments}
                    tooltip="Remove Parameter"
                    icon="pi pi-fw pi-trash"
                    tooltipOptions={tooltipOptions}
                    onClick={() => removeParameter(i)}
                  />
                </div>
              );
            })}
            <div className="p-inputgroup in-name row-center meta-row argument-fields-height" style={{ marginBottom: '0.5em' }}>
              <InputText
                disabled={disableArguments}
                name="parameter"
                className="in-name"
                value={methodMatcher.parameterInput}
                onChange={(e) => setState('parameterInput', e.target.value)}
              />
              <Button
                disabled={disableArguments}
                tooltip="Add Parameter"
                icon="pi pi-plus"
                tooltipOptions={tooltipOptions}
                onClick={(e) => addParameter(e)}
              />
            </div>
          </div>
        </div>
      </Fieldset>
    </>
  );
};

MethodMatcher.propTypes = {
  /** Class Matcher state */
  methodMatcher: PropTypes.object,
  /** Callback on class matcher change */
  onMethodMatcherChange: PropTypes.func,
};

MethodMatcher.defaultProps = {
  onMethodMatcherChange: () => {},
};

export default MethodMatcher;
