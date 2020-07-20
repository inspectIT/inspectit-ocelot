import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';
import { InputSwitch } from 'primereact/inputswitch';

const RulesEditorToolbar = ({
  ruleName,
  templateName,

  isUnsaved,
  readOnly,
  numErrors,
  ruleEnabled,
  onEnabledStateChanged,
  onSave,
}) => {
  return (
    <>
      <style jsx>{`
        .this :global(.p-toolbar) {
          border: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
          border-radius: 0;
        }
        .p-toolbar-group-left,
        .p-toolbar-group-right {
          display: flex;
          height: 2rem;
          align-items: center;
        }
        .p-toolbar-group-left :global(.pi) {
          font-size: 1.75rem;
          color: #aaa;
          margin-right: 1rem;
        }
        .h4 {
          font-weight: normal;
          margin-right: 1rem;
        }
        .template-details {
          color: #9e9e9e;
          font-weight: normal;
        }
        .template-details.spacer {
          margin: 0 0.5rem;
        }
        .enable-switch {
          margin-right: 1rem;
          display: flex;
          align-items: center;
        }
        .enable-switch span {
          margin-right: 0.5rem;
        }
        .text-addition {
          font-style: italic;
          color: #8a8a8a;
          margin-left: 0.5rem;
        }
      `}</style>

      <div className="this">
        <Toolbar>
          <div className="p-toolbar-group-left">
            <i className={'pi pi-bell'}></i>
            <h4>
              <span className="template-details">{templateName}</span>
              <span className="template-details spacer">{'>'}</span>
              {ruleName}
            </h4>
            {isUnsaved && <div className="text-addition">*</div>}
            {readOnly && <div className="text-addition">(read only)</div>}
          </div>

          <div className="p-toolbar-group-right">
            <div className="enable-switch">
              <span>Enabled:</span>
              <InputSwitch disabled={readOnly} checked={ruleEnabled} onChange={(e) => onEnabledStateChanged(e.value)} />
            </div>

            {!readOnly && <Button disabled={numErrors > 0 || !isUnsaved} label="Save" icon="pi pi-save" onClick={onSave} />}
          </div>
        </Toolbar>
      </div>
    </>
  );
};

RulesEditorToolbar.propTypes = {
  /** The name of the current selection */
  ruleName: PropTypes.string,
  /** Additional info to show in the tool bar */
  templateName: PropTypes.string.isRequired,
  /** Whether selection is unsaved */
  isUnsaved: PropTypes.bool,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** Whether the selected rule is enabled */
  ruleEnabled: PropTypes.bool,
  /** Callback on enabled state change */
  onEnabledStateChanged: PropTypes.func,
  /** Callback on save */
  onSave: PropTypes.func,
};

RulesEditorToolbar.defaultProps = {
  readOnly: false,
  isUnsaved: false,
  numErrors: 0,
  onEnabledStateChanged: () => {},
  onSave: () => {},
};

export default RulesEditorToolbar;
