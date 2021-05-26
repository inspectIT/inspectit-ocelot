import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import ClassMatcher from './ClassMatcher';
import MethodMatcher from './MethodMatcher';

/**
 * The scope wizard dialog itself.
 */
const ScopeWizardDialog = ({ visible, onHide }) => {
  const [classMatcher, setClassMatcher] = useState({ currentClassMatcher: '', classMatcherType: '', className: '' });
  const [methodMatcher, setMethodMatcher] = useState({
    selectedMethodMatchers: [],
    methodMatcherType: '',
    isConstructor: 'false',
    isSelectedParameter: false,
    parameterInput: '',
    parameterList: [],
    methodName: '',
  });

  const onMethodMatcherChange = (e, selectedMethodMatchers, setSelectedMethodMatchers) => {
    if (e.checked) {
      selectedMethodMatchers.push(e.value);
    } else {
      for (let i = 0; i < selectedMethodMatchers.length; i++) {
        const selectedMethodMatcher = selectedMethodMatchers[i];

        if (selectedMethodMatcher.key === e.value.key) {
          selectedMethodMatchers.splice(i, 1);
          break;
        }
      }
    }
    setSelectedMethodMatchers(selectedMethodMatchers);
  };

  const handleParameterChange = (e, index) => {
    const parameterList = this.state.parameterList;
    parameterList[index].parameter = e.target.value;
    this.setState(parameterList);
  };

  const removeParameter = (index) => {
    const parameterList = this.state.parameterList;
    parameterList.splice(index, 1);
    this.setState(parameterList);
  };

  const addParameter = () => {
    this.setState({ parameterList: [...this.state.parameterList, { parameter: this.state.parameterInput }] });
    this.setState({ parameterInput: '' });
  };

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Add" />
      <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
    </div>
  );

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

        .meta-row label:not(:first-child) {
          margin-left: 0.5rem;
        }

        .meta-row .inner-label {
          margin-left: 0.5rem;
          margin-right: 0.5rem;
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

      <div className="this">
        <Dialog
          className="scope-wizard-dialog"
          header="Define Selection"
          visible={visible}
          style={{ width: '50rem', minWidth: '50rem' }}
          onHide={onHide}
          blockScroll
          footer={footer}
          focusOnShow={false}
        >
          <ClassMatcher classMatcher={classMatcher} onClassMatcherChange={setClassMatcher} />

          {/*<MethodMatcher*/}
          {/*  onMethodMatcherChange={this.onMethodMatcherChange}*/}
          {/*  selectedMethodMatchers={this.state.selectedMethodMatchers}*/}
          {/*  setSelectedMethodMatchers={this.setSelectedMethodMatchers}*/}
          {/*  setIsConstructor={this.setIsConstructor}*/}
          {/*  isConstructor={this.state.isConstructor}*/}
          {/*  methodMatcherType={this.state.methodMatcherType}*/}
          {/*  setMethodMatcherType={this.setMethodMatcherType}*/}
          {/*  isSelectedParameter={this.state.isSelectedParameter}*/}
          {/*  setIsSelectedParameter={this.setIsSelectedParameter}*/}
          {/*  parameterList={this.state.parameterList}*/}
          {/*  removeParameter={this.removeParameter}*/}
          {/*  addParameter={this.addParameter}*/}
          {/*  parameterInput={this.state.parameterInput}*/}
          {/*  setParameterInput={this.setParameterInput}*/}
          {/*/>*/}
        </Dialog>
      </div>
    </>
  );
};

ScopeWizardDialog.propTypes = {
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
};

ScopeWizardDialog.defaultProps = {
  visible: true,
  onHide: () => {},
};

export default ScopeWizardDialog;
