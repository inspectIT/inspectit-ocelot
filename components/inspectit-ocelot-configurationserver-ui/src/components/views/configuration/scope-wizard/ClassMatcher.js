import { Fieldset } from 'primereact/fieldset';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';

/** data */
import { TOOLTIP_OPTIONS } from '../../../../data/constants';
import { TYPE_MATCHERS, MATCHER_TYPES } from './ScopeWizardConstants';

const ClassMatcher = ({ classMatcher, onClassMatcherChange, onShowClassBrowser }) => {
  const setState = (stateArgument, value) => {
    const currentClassMatcher = {
      ...classMatcher,
      [stateArgument]: value,
    };

    onClassMatcherChange(currentClassMatcher);
  };

  return (
    <>
      <style jsx>{`
        .row-center {
          display: flex;
          align-items: center;
          margin-top: 0.5rem;
        }

        .row-center :global(.fill),
        .fill {
          flex-grow: 1;
        }

        .row-center label {
          margin-right: 2rem;
        }

        .row-center label:not(:first-child) {
          margin-left: 0.5rem;
        }

        .row-center .inner-label {
          margin-left: 0.5rem;
          margin-right: 0.5rem;
        }

        .row-center .input-text {
          margin-left: 0.5rem;
        }
      `}</style>

      <Fieldset legend="Type Matcher" style={{ paddingTop: 0, paddingBottom: '1rem' }}>
        <div className="row-center">
          <Dropdown
            style={{ width: '7rem' }}
            value={classMatcher.type}
            options={TYPE_MATCHERS}
            onChange={(e) => setState('currentClassMatcher', e.value)}
          />
          <label className="inner-label" htmlFor="which">
            which name
          </label>
          <Dropdown
            style={{ width: '14rem' }}
            value={classMatcher.matcherType}
            options={MATCHER_TYPES}
            onChange={(e) => setState('classMatcherType', e.value)}
          />
          <div className="p-inputgroup input-text fill">
            <InputText
              className="fill"
              value={classMatcher.name}
              onChange={(e) => setState('className', e.target.value)}
              placeholder="Name Pattern"
              tooltip="The name or pattern which is used to match against the fully qualified class or interface name."
              tooltipOptions={TOOLTIP_OPTIONS}
            />
            <Button tooltip="Class Browser" icon="pi pi-search" tooltipOptions={TOOLTIP_OPTIONS} onClick={() => onShowClassBrowser()} />
          </div>
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
  /** Callback for show class browser */
  onShowClassBrowser: PropTypes.func,
};

ClassMatcher.defaultProps = {
  onClassMatcherChange: () => {},
  onShowClassBrowser: () => {},
};

export default ClassMatcher;
