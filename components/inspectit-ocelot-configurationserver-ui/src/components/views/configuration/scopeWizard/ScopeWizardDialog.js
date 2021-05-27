import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import ClassMatcher from './ClassMatcher';
import MethodMatcher from './MethodMatcher';

/** data */
import { methodVisibility } from './ScopeWizardConstants';

/**
 * The scope wizard dialog itself.
 */
const ScopeWizardDialog = ({ visible, onHide, onScopeWizardApply }) => {
  const [classMatcher, setClassMatcher] = useState({ currentClassMatcher: '', classMatcherType: '', className: '' });
  const [methodMatcher, setMethodMatcher] = useState({
    selectedMethodVisibilities: _.clone(methodVisibility),
    methodMatcherType: '',
    isConstructor: false,
    isSelectedParameter: false,
    parameterInput: '',
    parameterList: [],
    methodName: '',
  });

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Add" onClick={() => onScopeWizardApply(classMatcher, methodMatcher)} />
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
          <MethodMatcher methodMatcher={methodMatcher} onMethodMatcherChange={setMethodMatcher} />
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
  /** Callback on dialog apply */
  onScopeWizardApply: PropTypes.func,
};

ScopeWizardDialog.defaultProps = {
  visible: true,
  onHide: () => {},
  onScopeWizardApply: () => {},
};

export default ScopeWizardDialog;
