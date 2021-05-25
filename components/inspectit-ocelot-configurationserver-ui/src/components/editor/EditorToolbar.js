import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';

/**
 * The toolbar used within the editor view.
 */
const EditorToolbar = ({
  enableButtons,
  onSave,
  onSearch,
  onHelp,
  onRefresh,
  isRefreshing,
  canSave,
  visualConfig,
  onVisualConfigChange,
  children,
}) => (
  <div className="this">
    <style jsx>
      {`
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
      `}
    </style>
    <Toolbar>
      <div className="p-toolbar-group-left">{children}</div>
      <div className="p-toolbar-group-right button-not-active">
        <Button
          disabled={!enableButtons}
          icon="pi pi-table"
          className={!visualConfig && 'p-button-outlined'}
          onClick={onVisualConfigChange}
        />
        {onRefresh && (
          <Button disabled={!enableButtons || isRefreshing} icon={'pi pi-refresh' + (isRefreshing ? ' pi-spin' : '')} onClick={onRefresh} />
        )}
        {!visualConfig && (
          <>
            <Button disabled={!enableButtons} icon="pi pi-question" onClick={onHelp} />
            <Button disabled={!enableButtons} icon="pi pi-search" onClick={onSearch} />
          </>
        )}
        <Button disabled={!enableButtons || !canSave} onClick={onSave} label="Save" icon="pi pi-save" />
      </div>
    </Toolbar>
  </div>
);

EditorToolbar.propTypes = {
  enableButtons: PropTypes.bool,
  isRefreshing: PropTypes.bool,
  canSave: PropTypes.bool,
  visualConfig: PropTypes.bool,
  children: PropTypes.node,
  onSave: PropTypes.func,
  onSearch: PropTypes.func,
  onHelp: PropTypes.func,
  onRefresh: PropTypes.func,
  onVisualConfigChange: PropTypes.func,
};

EditorToolbar.defaultProps = {
  enableButtons: true,
  isRefreshing: false,
  canSave: true,
  visualConfig: false,
  children: null,
  onSave: () => {},
  onSearch: () => {},
  onHelp: () => {},
  onRefresh: () => {},
  onVisualConfigChange: () => {},
};

export default EditorToolbar;
