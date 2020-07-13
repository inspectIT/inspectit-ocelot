import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';
import { InputSwitch } from 'primereact/inputswitch';

const RulesEditorToolbar = ({
  selectionName,
  selectionNameAddition,
  isRule,
  isUnsaved,
  readOnly,
  numErrors,
  ruleEnabled,
  savedRuleHasError,
  savedRuleIsEnabled,
  onEnabledStateChanged,
  onSave,
}) => {
  const headerIconClassNames = classNames({
    pi: true,
    'pi-bell': isRule,
    'pi-briefcase': !isRule && selectionName,
    'pi-info': !isRule && !selectionName,
    'red-icon': isRule && savedRuleHasError,
    'green-icon': isRule && !savedRuleHasError && savedRuleIsEnabled,
    'grey-icon': !isRule || !savedRuleIsEnabled,
  });

  const saveButtonClassNames = classNames({ 'p-button-danger': numErrors > 0, tooltip: numErrors > 0 });

  return (
    <div className="this">
      <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
        }
        .p-toolbar-group-right > :global(*) {
          margin-left: 0.25rem;
        }
        .this :global(.p-button-outlined) {
          color: #005b9f;
          background-color: rgba(0, 0, 0, 0);
        }
        .this :global(.p-toolbar div) {
          vertical-align: middle;
        }
        .this :global(.tooltip) {
          position: relative;
        }
        .this :global(.tooltip .tooltiptext) {
          visibility: hidden;
          width: 300px;
          background-color: black !important;
          color: #fff;
          text-align: center;
          padding: 5px 0;
          border-radius: 6px;
          position: absolute;
          top: -45px;
          bottom: auto;
          right: 0;
          z-index: 1;
        }
        .this :global(.tooltip:hover .tooltiptext) {
          visibility: visible;
        }
        .this :global(.header) {
          font-size: 1rem;
          display: flex;
          align-items: center;
          height: 2rem;
        }
        .this :global(.header .pi) {
          font-size: 1.75rem;
          color: #aaa;
          margin-right: 1rem;
        }
        .this :global(.text-addition) {
          margin-left: 1rem;
          color: #999;
        }
        .this :global(.green-icon) {
          color: green !important;
        }
        .this :global(.grey-icon) {
          color: grey !important;
        }
        .this :global(.red-icon) {
          color: red !important;
        }
        .this :global(.p-inplace-display) {
          font-size: 1rem;
          vertical-align: middle;
          display: block;
        }
        .this :global(.p-inplace-display .pi-pencil) {
          font-size: 1rem;
        }
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <div className="header">
            <i className={headerIconClassNames}></i>
            <span>{'' + (selectionName || 'Select Rule or Template')}</span>
            {isUnsaved && <div className="text-addition">*</div>}
            {selectionName && readOnly && <div className="text-addition">(read only)</div>}
            {!!selectionNameAddition && <div className="text-addition">|</div>}
            {!!selectionNameAddition && <div className="text-addition">{selectionNameAddition}</div>}
          </div>
        </div>
        {isRule && (
          <div className="p-toolbar-group-right button-not-active">
            <span>Enabled:</span>
            <InputSwitch disabled={readOnly} checked={ruleEnabled} onChange={(e) => onEnabledStateChanged(e.value)} />
            <Button
              className={saveButtonClassNames}
              disabled={numErrors > 0 || readOnly || !isUnsaved}
              label="Save"
              icon="pi pi-save"
              onClick={onSave}
            >
              {numErrors > 0 ? <span className="tooltiptext">{getErrorText(numErrors)}</span> : <></>}
            </Button>
          </div>
        )}
      </Toolbar>
    </div>
  );
};

const getErrorText = (numErrors) => {
  var errorText = 'Cannot Save! ';
  if (numErrors === 1) {
    errorText = errorText + 'A variable contains an error.';
  } else if (numErrors > 1) {
    errorText = errorText + 'The variables contain ' + numErrors + ' errors.';
  }
  return errorText;
};

RulesEditorToolbar.propTypes = {
  /** The name of the current selection */
  selectionName: PropTypes.string.isRequired,
  /** Additional info to show in the tool bar */
  selectionNameAddition: PropTypes.string,
  /** Whether selection is a rule */
  isRule: PropTypes.bool,
  /** Whether selection is unsaved */
  isUnsaved: PropTypes.bool,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** Number of editing errors on the selected content */
  numErrors: PropTypes.number,
  /** Whether the selected rule is enabled */
  ruleEnabled: PropTypes.bool.isReuiqred,
  /** Whether the saved version of the selected rule has execution errors or not */
  savedRuleHasError: PropTypes.bool,
  /** Whether the saved version of the selected rule is enabled */
  savedRuleIsEnabled: PropTypes.bool,
  /** Callback on enabled state change */
  onEnabledStateChanged: PropTypes.func,
  /** Callback on save */
  onSave: PropTypes.func,
};

RulesEditorToolbar.defaultProps = {
  selectionNameAddition: undefined,
  readOnly: false,
  isRule: false,
  isUnsaved: false,
  numErrors: 0,
  savedRuleHasError: false,
  savedRuleIsEnabled: false,
  onEnabledStateChanged: () => {},
  onSave: () => {},
};

export default RulesEditorToolbar;
