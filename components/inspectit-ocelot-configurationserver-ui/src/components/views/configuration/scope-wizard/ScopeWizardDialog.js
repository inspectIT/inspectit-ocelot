import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import TypeMatcher from './TypeMatcher';
import MethodMatcher from './MethodMatcher';

/** data */
import { METHOD_VISIBILITY } from './ScopeWizardConstants';

/**
 * The scope wizard dialog itself.
 */
const ScopeWizardDialog = ({ visible, onHide, onApply }) => {
  const [typeMatcher, setTypeMatcher] = useState({ type: 'class', matcherType: 'EQUALS_FULLY', name: null });
  const [methodMatcher, setMethodMatcher] = useState({
    visibilities: _.clone(METHOD_VISIBILITY),
    matcherType: null,
    isConstructor: false,
    isSelectedParameter: false,
    parameterInput: null,
    parameterList: [],
    name: null,
  });

  const showClassBrowser = () => {
    return alert('Todo: Open Class Browser');
  };

  // the dialogs footer
  const footer = (
    <div>
      <Button label="Apply" onClick={() => onApply(typeMatcher, methodMatcher)} />
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
          <TypeMatcher typeMatcher={typeMatcher} onTypeMatcherChange={setTypeMatcher} onShowClassBrowser={showClassBrowser} />
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
  onApply: PropTypes.func,
};

ScopeWizardDialog.defaultProps = {
  visible: true,
  onHide: () => {},
  onApply: () => {},
};

export default ScopeWizardDialog;
