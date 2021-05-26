import { Fieldset } from 'primereact/fieldset';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';

/** data */
import { classMatchers, matcherTypes, tooltipOptions } from './ScopeWizardConstants';

const ClassMatcher = ({ classMatcher, onClassMatcherChange }) => {
  let heightClassFieldset = window.innerHeight * 0.15;

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

      <Fieldset legend="Class Matcher" style={{ paddingTop: 0, height: heightClassFieldset }}>
        <div className="row-center row-margin meta-row fill">
          <Dropdown
            className="class-matcher-dropdown"
            value={classMatcher.currentClassMatcher}
            options={classMatchers}
            onChange={(e) => onClassMatcherChange({ currentClassMatcher: e.value })}
            placeholder="Select a Class Matcher"
          />
          <label className="inner-label" htmlFor="which">
            which
          </label>
          <Dropdown
            className="class-matcher-type-dropdown"
            value={classMatcher.classMatcherType}
            options={matcherTypes}
            onChange={(e) => onClassMatcherChange({ classMatcherType: e.value })}
            placeholder="Select a Matcher Type"
          />
          <InputText className="in-name" onChange={(e) => onClassMatcherChange({ className: e.target.value })} />
          <Button
            tooltip="Class Browser"
            icon="pi pi-search"
            tooltipOptions={tooltipOptions}
            onClick={() => alert('Todo: Open Class Browser')} //showClassBrowserDialog()}
          />
        </div>
      </Fieldset>
    </>
  );
};

ClassMatcher.propTypes = {
  /** Class Matcher state */
  classMatcher: PropTypes.object,
  /** Callback on class matcher change */
  onClassMatcherChange: PropTypes.func,
};

ClassMatcher.defaultProps = {
  onClassMatcherChange: () => {},
};

export default ClassMatcher;
