import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'primereact/button';
import { Toolbar } from 'primereact/toolbar';
import { SplitButton } from 'primereact/splitbutton';

/** data */
import { TOOLTIP_OPTIONS } from '../../data/constants';

/**
 * The toolbar used within the editor view.
 */
const EditorToolbar = ({
  enableButtons,
  onSave,
  onConvert,
  onShowYaml,
  onSearch,
  onHelp,
  onRefresh,
  isRefreshing,
  canSave,
  visualConfig,
  onVisualConfigChange,
  children,
  showMethodConfiguration,
}) => (
  <div className="this">
    <style jsx>
      {`
        .this :global(.p-toolbar) {
          border: 0;
          border-radius: 0;
          background-color: #eee;
          border-bottom: 1px solid #ddd;
          padding-top: 0.5rem;
          padding-bottom: 0.5rem;
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
    <Toolbar
      className="p-toolbar-group-left"
      left={children}
      right={
        <div className="p-toolbar-group-right button-not-active">
          {!showMethodConfiguration && (
            <Button
              disabled={!enableButtons}
              icon="pi pi-table"
              className={!visualConfig && 'p-button-outlined'}
              onClick={onVisualConfigChange}
              tooltip="Show Table View"
              tooltipOptions={TOOLTIP_OPTIONS}
            />
          )}
          {!showMethodConfiguration && onRefresh && (
            <Button
              disabled={!enableButtons || isRefreshing}
              icon={'pi pi-refresh' + (isRefreshing ? ' pi-spin' : '')}
              onClick={onRefresh}
              tooltip="Refresh File"
              tooltipOptions={TOOLTIP_OPTIONS}
            />
          )}
          {!showMethodConfiguration && !visualConfig && (
            <>
              <Button
                disabled={!enableButtons}
                icon="pi pi-question"
                onClick={onHelp}
                tooltip="Show Keyboard Shortcuts"
                tooltipOptions={TOOLTIP_OPTIONS}
              />
              <Button
                disabled={!enableButtons}
                icon="pi pi-search"
                onClick={onSearch}
                tooltip="Search in File"
                tooltipOptions={TOOLTIP_OPTIONS}
              />
            </>
          )}
          {showMethodConfiguration && (
            <SplitButton
              disabled={!enableButtons}
              label="Show as YAML"
              tooltipOptions={TOOLTIP_OPTIONS}
              onClick={onShowYaml}
              model={[
                {
                  label: 'Convert to YAML',
                  icon: 'pi pi-images',
                  command: () => onConvert(),
                },
              ]}
            />
          )}
          <Button
            disabled={!enableButtons || !canSave}
            onClick={onSave}
            label="Save"
            icon="pi pi-save"
            tooltip="Save File"
            tooltipOptions={TOOLTIP_OPTIONS}
          />
        </div>
      }
    ></Toolbar>
  </div>
);

EditorToolbar.propTypes = {
  enableButtons: PropTypes.bool,
  isRefreshing: PropTypes.bool,
  canSave: PropTypes.bool,
  visualConfig: PropTypes.bool,
  showMethodConfiguration: PropTypes.bool,
  children: PropTypes.node,
  onSave: PropTypes.func,
  onConvert: PropTypes.func,
  onShowYaml: PropTypes.func,
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
  showMethodConfiguration: false,
  children: null,
  onSave: () => {},
  onConvert: () => {},
  onShowYaml: () => {},
  onSearch: () => {},
  onHelp: () => {},
  onRefresh: null,
  onVisualConfigChange: () => {},
};

export default EditorToolbar;
