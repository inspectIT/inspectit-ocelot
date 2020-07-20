import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';
import * as alertingConstants from '../../constants';

const HandlerEditorToolbar = ({ handlerName, handlerKind, topicName, readOnly, isUnsaved, onSave }) => {
  const icon = alertingConstants.handlerIcons(handlerKind);

  return (
    <div className="this">
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
        .topic-details {
          color: #9e9e9e;
          font-weight: normal;
        }
        .topic-details.spacer {
          margin: 0 0.5rem;
        }
        .text-addition {
          font-style: italic;
          color: #8a8a8a;
          margin-left: 0.5rem;
        }
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <i className={classNames('pi', icon)}></i>
          <h4>
            <span className="topic-details">{topicName}</span>
            <span className="topic-details spacer">{'>'}</span>
            {handlerName}
          </h4>
          {isUnsaved && <div className="text-addition">*</div>}
          {readOnly && <div className="text-addition">(read only)</div>}
        </div>

        <div className="p-toolbar-group-right button-not-active">
          <Button disabled={readOnly || !isUnsaved} label="Save" icon="pi pi-save" onClick={onSave} />
        </div>
      </Toolbar>
    </div>
  );
};

HandlerEditorToolbar.propTypes = {
  /** name of the selected handler */
  handlerName: PropTypes.string.isRequired,
  /** the type of the selected handler */
  handlerKind: PropTypes.string,
  /** name of the selected topic*/
  topicName: PropTypes.string.isRequired,
  /** Whether selection is unsaved */
  isUnsaved: PropTypes.bool,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** Callback on save */
  onSave: PropTypes.func,
};

HandlerEditorToolbar.defaultProps = {
  handlerKind: undefined,
  readOnly: false,
  isUnsaved: false,
  onSave: () => {},
};

export default HandlerEditorToolbar;
