import { Fieldset } from 'primereact/fieldset';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

/** data */
import { TOOLTIP_OPTIONS } from '../../../../data/constants';
import { MATCHER_MODE_DESCRIPTION_S } from '../../../editor/method-configuration-editor/constants';

const TypeMatcher = ({ typeMatcher, onTypeMatcherChange, onShowClassBrowser }) => {
  /** Possible Types */
  const types = [
    { label: 'Class', value: 'type' },
    { label: 'Superclass', value: 'superclass' },
    { label: 'Interface', value: 'interfaces' },
  ];

  /** Possible Matcher Types*/
  const matcherTypes = _.map(MATCHER_MODE_DESCRIPTION_S, (key, value) => {
    return { label: key, value };
  });

  const setState = (stateArgument, value) => {
    const currentTypeMatcher = {
      ...typeMatcher,
      [stateArgument]: value,
    };

    onTypeMatcherChange(currentTypeMatcher);
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
          <Dropdown style={{ width: '7rem' }} value={typeMatcher.type} options={types} onChange={(e) => setState('type', e.value)} />
          <label className="inner-label" htmlFor="which">
            name
          </label>
          <Dropdown
            style={{ width: '14rem' }}
            value={typeMatcher.matcherType}
            options={matcherTypes}
            onChange={(e) => setState('matcherType', e.value)}
          />
          <div className="p-inputgroup input-text fill">
            <InputText
              className="fill"
              value={typeMatcher.name}
              onChange={(e) => setState('name', e.target.value)}
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

TypeMatcher.propTypes = {
  /** Class Matcher state */
  typeMatcher: PropTypes.object,
  /** Callback on class matcher change */
  onTypeMatcherChange: PropTypes.func,
  /** Callback for show class browser */
  onShowClassBrowser: PropTypes.func,
};

TypeMatcher.defaultProps = {
  onTypeMatcherChange: () => {},
  onShowClassBrowser: () => {},
};

export default TypeMatcher;
