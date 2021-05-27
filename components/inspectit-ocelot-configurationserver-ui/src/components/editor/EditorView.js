import dynamic from 'next/dynamic';
import PropTypes from 'prop-types';
import React, { useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import editorConfig from '../../data/yaml-editor-config.json';
import EditorToolbar from './EditorToolbar';
import Notificationbar from './Notificationbar';
import YamlParser from './visual-editor/YamlParser';
import SelectionInformation from './SelectionInformation';
import { configurationSelectors, configurationActions } from '../../redux/ducks/configuration';
import { getConfigurationType } from '../../lib/configuration-utils';
import { CONFIGURATION_TYPES } from '../../data/constants';
import MethodConfigurationEditor from './method-configuration-editor/MethodConfigurationEditor';

const AceEditor = dynamic(() => import('./yaml-editor/AceEditor'), { ssr: false });
const TreeTableEditor = dynamic(() => import('./visual-editor/TreeTableEditor'), { ssr: false });

/**
 * Editor view consisting of the AceEditor and a toolbar.
 *
 */
const EditorView = ({
  value,
  schema,
  showEditor,
  hint,
  onRefresh,
  onChange,
  onCreate,
  onSave,
  isRefreshing,
  enableButtons,
  isErrorNotification,
  notificationIcon,
  notificationText,
  canSave,
  loading,
  children,
  readOnly,
  showVisualConfigurationView,
  onToggleVisualConfigurationView,
  sidebar,
}) => {
  const dispatch = useDispatch();

  const editorRef = useRef(null);

  // global state variables
  const currentVersion = useSelector((state) => state.configuration.selectedVersion);
  const isLatest = useSelector(configurationSelectors.isLatestVersion);

  // derived variables
  const isLiveSelected = currentVersion === 'live';
  const configurationType = getConfigurationType(value);

  const selectlatestVersion = () => {
    dispatch(configurationActions.selectVersion(null));
  };

  let editorContent;

  if (configurationType == CONFIGURATION_TYPES.METHOD_CONFIGURATION) {
    editorContent = <MethodConfigurationEditor yamlConfiguration={value} />;
  } else if (configurationType == CONFIGURATION_TYPES.YAML && showVisualConfigurationView) {
    editorContent = (
      <YamlParser yamlConfig={value} onUpdate={onChange}>
        {(onUpdate, config) => (
          <TreeTableEditor config={config} schema={schema} loading={loading} readOnly={readOnly} onUpdate={onUpdate} />
        )}
      </YamlParser>
    );
  } else {
    editorContent = (
      <AceEditor
        editorRef={(editor) => (editorRef.current = editor)}
        onCreate={onCreate}
        mode="yaml"
        theme="cobalt"
        options={editorConfig}
        value={value}
        onChange={onChange}
        history-view
        canSave={canSave}
        onSave={onSave}
        readOnly={readOnly}
      />
    );
  }

  return (
    <div className="this">
      <style jsx>{`
        .this {
          flex: 1;
          display: flex;
          flex-direction: column;
          overflow-y: hidden;
        }
        .selection-information {
          display: flex;
          height: 100%;
          align-items: center;
          justify-content: center;
          color: #bbb;
        }
        .editor-menu {
        }
        .editor-content {
          flex: 1;
          display: flex;
          flex-direction: column;
        }
        .editor-container {
          position: relative;
          flex-grow: 1;
        }
        .loading-overlay {
          position: absolute;
          left: 0;
          top: 0;
          right: 0;
          bottom: 0;
          background-color: #00000080;
          color: white;
          z-index: 100;
          justify-content: center;
          align-items: center;
          display: flex;
        }
        .editor-row {
          display: flex;
          flex: 1 1 auto;
          overflow: hidden;
          position: relative;
        }
        .version-notice {
          background-color: #ffcc80;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0.5rem 1rem;
          border-bottom: 1px solid #dddddd;
        }
        .version-notice i {
          margin-right: 1rem;
          color: #212121;
        }
        .gotoLatest {
          margin-left: 1rem;
          color: #007ad9;
          text-decoration: underline;
          cursor: pointer;
          white-space: nowrap;
        }
      `}</style>
      <div className="editor-menu">
        <EditorToolbar
          enableButtons={enableButtons}
          canSave={canSave}
          onRefresh={onRefresh}
          isRefreshing={isRefreshing}
          onSave={onSave}
          onSearch={() => editorRef.current.executeCommand('find')}
          onHelp={() => editorRef.current.showShortcuts()}
          visualConfig={showVisualConfigurationView}
          onVisualConfigChange={onToggleVisualConfigurationView}
          showOnlySave={configurationType === CONFIGURATION_TYPES.METHOD_CONFIGURATION}
        >
          {children}
        </EditorToolbar>
      </div>

      <div className="editor-row">
        <div className="editor-content">
          {!isLatest && (
            <div className="version-notice">
              <i className="pi pi-info-circle" />
              {isLiveSelected ? (
                <div>
                  You are viewing the latest <b>live</b> configuration. Modifications are only possible on the <b>latest workspace</b>{' '}
                  configuration.
                </div>
              ) : (
                <div>
                  You are viewing not the latest workspace configuration. Modifications are only possible on the <b>latest workspace</b>{' '}
                  configuration.
                </div>
              )}
              <div className="gotoLatest" onClick={selectlatestVersion}>
                Go to latest workspace
              </div>
            </div>
          )}

          {showEditor && <div className="editor-container">{editorContent}</div>}
          {!showEditor && <SelectionInformation hint={hint} />}
        </div>

        {sidebar}

        {loading && (
          <div className="loading-overlay">
            <i className="pi pi-spin pi-spinner" style={{ fontSize: '2em' }}></i>
          </div>
        )}
      </div>

      {notificationText && configurationType == CONFIGURATION_TYPES.YAML ? (
        <Notificationbar text={notificationText} isError={isErrorNotification} icon={notificationIcon} />
      ) : null}
    </div>
  );
};

EditorView.propTypes = {
  /** The value of the editor */
  value: PropTypes.string,
  /** The configuration schema */
  schema: PropTypes.object,
  /** Whether the editor should be shown or hidden. */
  showEditor: PropTypes.bool,
  /** The hint which will be shown if the editor is hidden. */
  hint: PropTypes.string,
  /** Callback which is triggered when the save button is pressed. */
  onSave: PropTypes.func,
  /** Callback which is executed when the refresh button is pressed. The refresh button is only shown if this callback is specified. */
  onRefresh: PropTypes.func,
  /** If true, the refresh button is disabled and showing a spinner. */
  isRefreshing: PropTypes.bool,
  /** Whether the toolbar buttons should be enabled or disabled. */
  enableButtons: PropTypes.bool,
  /** The children will be shown in the toolbar. Can be used e.g. to show additional information. */
  children: PropTypes.element,
  /** Whether the save button is enabled or not. The save button is enabled only if the `enableButtons` is true.  */
  canSave: PropTypes.bool,
  /** Whether the notification bar is showing an error or not. */
  isErrorNotification: PropTypes.bool,
  /** The icon class to show in the notification bar. */
  notificationIcon: PropTypes.string,
  /** The text to show in the notification bar. */
  notificationText: PropTypes.string,
  /** Whether the editor should show an loading indicator */
  loading: PropTypes.bool,
  /** Wheter the editor should be in read-only mode */
  readOnly: PropTypes.bool,
  /** Weather a visual configuration view is active showing config properties in a tree */
  showVisualConfigurationView: PropTypes.bool,
  /** Function to react on the change of the enable disable visual configuration view */
  onToggleVisualConfigurationView: PropTypes.func,
};

EditorView.defaultProps = {
  showEditor: true,
  enableButtons: true,
  canSave: true,
  loading: false,
  showVisualConfigurationView: false,
};

export default EditorView;
