import { Fieldset } from 'primereact/fieldset';
import { InputText } from 'primereact/inputtext';
import React from 'react';
import PropTypes from 'prop-types';

/** data */
import { TOOLTIP_OPTIONS } from '../../../../data/constants';
import { DEFAULT_SCOPE_PROPS_STATE } from './ScopeWizardDialog';

const ScopeProps = ({ scopeProperties, onScopePropsChange }) => {
  const setState = (stateArgument, value) => {
    const currentScopePropertiesMatcher = {
      ...scopeProperties,
      [stateArgument]: value,
    };
    onScopePropsChange(currentScopePropertiesMatcher);
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

      <Fieldset legend="Scope properties" style={{ paddingTop: 0, paddingBottom: '1rem' }}>
        <div className="row-center">
          <label className="inner-label" htmlFor="which">
            Name:
          </label>
          <div className="p-inputgroup input-text fill">
            <InputText
              className="fill"
              value={scopeProperties?.name === undefined ? DEFAULT_SCOPE_PROPS_STATE.name : scopeProperties.name}
              onChange={(e) => setState('name', e.target.value)}
              placeholder="Name of scope"
              tooltip="The name of the scope for the type matcher."
              tooltipOptions={TOOLTIP_OPTIONS}
            />
          </div>
          <label className="inner-label" htmlFor="which">
            Description:
          </label>
          <div className="p-inputgroup input-text fill">
            <InputText
              className="fill"
              value={scopeProperties?.description === undefined ? DEFAULT_SCOPE_PROPS_STATE.description : scopeProperties.description}
              onChange={(e) => setState('description', e.target.value)}
              placeholder="Description of scope"
              tooltip="The description of the scope for the type matcher."
              tooltipOptions={TOOLTIP_OPTIONS}
            />
          </div>
        </div>
      </Fieldset>
    </>
  );
};

ScopeProps.propTypes = {
  /** Scope Properties state */
  scopeProperties: PropTypes.object,
  /** Callback on scope properties change */
  onScopePropsChange: PropTypes.func,
};

ScopeProps.defaultProps = {
  onScopePropsChange: () => {},
};

export default ScopeProps;
