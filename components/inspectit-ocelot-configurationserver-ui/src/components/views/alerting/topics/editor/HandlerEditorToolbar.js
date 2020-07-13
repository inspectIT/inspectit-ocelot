import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';
import * as alertingConstants from '../../constants';

const HandlerEditorToolbar = ({ selectedHandlerName, selectedHandlerKind, selectedTopicName, readOnly, isUnsaved, onSave }) => {
  const headerIconClassNames = classNames({
    pi: true,
    [alertingConstants.handlerIcons(selectedHandlerKind)]: !!selectedHandlerName,
    [alertingConstants.topicIcon]: !selectedHandlerName && selectedTopicName,
    'pi-info': !selectedHandlerName && !selectedHandlerKind,
  });

  const selectionName = selectedHandlerName || selectedTopicName;

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
        .this :global(.text-addition) {
          margin-left: 1rem;
          color: #999;
        }
      `}</style>
      <Toolbar>
        <div className="p-toolbar-group-left">
          <div className="header">
            <i className={headerIconClassNames}></i>
            <span>{'' + (selectionName || 'Select Alerting Handler')}</span>
            {isUnsaved && <div className="text-addition">*</div>}
            {selectionName && readOnly && <div className="text-addition">(read only)</div>}
            {!!selectedHandlerName && !!selectedTopicName && <div className="text-addition">|</div>}
            {!!selectedHandlerName && !!selectedTopicName && <div className="text-addition">{'Topic: ' + selectedTopicName}</div>}
          </div>
        </div>
        {!!selectedHandlerName && (
          <div className="p-toolbar-group-right button-not-active">
            <Button disabled={readOnly || !isUnsaved} label="Save" icon="pi pi-save" onClick={onSave} />
          </div>
        )}
      </Toolbar>
    </div>
  );
};

HandlerEditorToolbar.propTypes = {
  /** name of the selected handler */
  selectedHandlerName: PropTypes.string,
  /** the type of the selected handler */
  selectedHandlerKind: PropTypes.string,
  /** name of the selected topic*/
  selectedTopicName: PropTypes.string,
  /** Whether selection is unsaved */
  isUnsaved: PropTypes.bool,
  /** Whether the content is read only */
  readOnly: PropTypes.bool,
  /** Callback on save */
  onSave: PropTypes.func,
};

HandlerEditorToolbar.defaultProps = {
  selectedHandlerName: undefined,
  selectedHandlerKind: undefined,
  selectedTopicName: undefined,
  readOnly: false,
  isUnsaved: false,
  onSave: () => {},
};

export default HandlerEditorToolbar;
