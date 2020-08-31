import React from 'react';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';

/**
 * The toolbar used within the editor view.
 *
 * onPropsSplit = (propsSplit: boolean, propsSplitHotizontal: boolean)
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
  selectedVersion
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
            <Button disabled={!enableButtons || isRefreshing || selectedVersion !== 0} icon={'pi pi-refresh' + (isRefreshing ? ' pi-spin' : '')} onClick={onRefresh} />
          )}
          {!visualConfig && (
            <>
              <Button disabled={!enableButtons || selectedVersion !== 0} icon="pi pi-question" onClick={onHelp} />
              <Button disabled={!enableButtons || selectedVersion !== 0} icon="pi pi-search" onClick={onSearch} />
            </>
          )}
          <Button disabled={!enableButtons || !canSave || selectedVersion !== 0} onClick={onSave} label="Save" icon="pi pi-save" />
        </div>
      </Toolbar>
    </div>
  );

export default EditorToolbar;
