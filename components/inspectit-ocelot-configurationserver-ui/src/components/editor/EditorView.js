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
import { TabView, TabPanel } from 'primereact/tabview';
import { Button } from 'primereact/button';

const AceEditor = dynamic(() => import('./yaml-editor/AceEditor'), { ssr: false });
const TreeTableEditor = dynamic(() => import('./visual-editor/TreeTableEditor'), { ssr: false });

let tabId = 0
let tabs = []

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
  showConfigurationDialog,
  showConvertWarning,
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

  // header template for the tab + click handler
  const tabHeaderTemplate = (tab) => {
    return (
      <div>
        <label>{tab.path}{tab.name}</label>
      </div>
    );
  }

  const closeTabHandler = (id) => {
    console.log("CLOSED", id)
    console.log("INDEX", id.index)
    // console.log("This tabs", tabs);
    let newTabs = tabs.filter(tab => tab.id != id.index);
    tabs = []
    tabs = [...newTabs]
    if(tabs.length == 0) {
      tabId = 0
    }

    console.log("new tabs", tabs);
    updateTabView(newTabs)
  }

  const changeTabHandler = (e) => {
    console.log("CHANGED", e)

    let selectedTab = tabs.filter((tab) => tab.id == e.index)

    try {
      let selectedFileName = selectedTab[0].path + selectedTab[0].name
      console.log("Selected File Name", selectedFileName)

      configurationActions.selectFile(selectedFileName)
    }catch(e) {
    
    }
  }

  const updateTabView = (tabs) => {
    return (
      <TabView scrollable>
            {tabs.map((tab) => {
              console.log("in loop", tab);
              return (
              <TabPanel key={tab.id} header={tabHeaderTemplate(tab)} closable>
                <div style={{ height: "500px"}}>{displayContent(tab)}</div>
              </TabPanel>
              );
            })}
      </TabView>
    );
  }

  const displayContent = (tab) => {
    if (configurationType == CONFIGURATION_TYPES.METHOD_CONFIGURATION) {
      return <MethodConfigurationEditor yamlConfiguration={tab.value} />;
    } else if (configurationType == CONFIGURATION_TYPES.YAML && showVisualConfigurationView) {
      return (
        <YamlParser yamlConfig={tab.value} onUpdate={onChange}>
          {(onUpdate, config) => (
            <TreeTableEditor
              config={config || { inspectit: null }}
              schema={schema}
              loading={loading}
              readOnly={readOnly}
              onUpdate={onUpdate}
            />
          )}
        </YamlParser>
      );
    } else {
      return (
        <AceEditor
          editorRef={(editor) => (editorRef.current = editor)}
          onCreate={onCreate}
          theme="cobalt"
          options={editorConfig}
          value={tab.value}
          onChange={onChange}
          history-view
          canSave={canSave}
          onSave={onSave}
          readOnly={readOnly}
        />
      );
    }
  }

  let editorContent;

  if (configurationType == CONFIGURATION_TYPES.METHOD_CONFIGURATION) {
    editorContent = <MethodConfigurationEditor yamlConfiguration={value} />;
  } else if (configurationType == CONFIGURATION_TYPES.YAML && showVisualConfigurationView) {
    editorContent = (
      <YamlParser yamlConfig={value} onUpdate={onChange}>
        {(onUpdate, config) => (
          <TreeTableEditor
            config={config || { inspectit: null }}
            schema={schema}
            loading={loading}
            readOnly={readOnly}
            onUpdate={onUpdate}
          />
        )}
      </YamlParser>
    );
  } else {
    editorContent = (
      <AceEditor
        editorRef={(editor) => (editorRef.current = editor)}
        onCreate={onCreate}
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

  let path;
  let name;
  try {
    path = children.props.path;
    name = children.props.name;
  } catch (e) {
    path = '';
    name = '';
  }

  // Check if the file is already opened in a tab or not
  let unique = true;
  tabs.map((tab) => {
    console.log("IN MAP", tab)
    if(tab.path == path && tab.name == name) {
      tab.value = value
      unique = false;
    }
  });

  console.log("unique", unique)
  console.log("path", path)
  console.log("name", name)
  console.log("value", value)
  console.log(unique && path != '' && name != '')
  if(unique && path != '' && name != '') {
    console.log("IN IF")
    let tab = {path: path, name: name, content: editorContent, id: tabId, value: ""}
    console.log('Value: \n', value);
    tab.value = value
    tabs.push(tab);
    tabId++
  }

  console.log("Tabs", tabs);

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
          onShowYaml={showConfigurationDialog}
          onConvert={showConvertWarning}
          onSearch={() => editorRef.current.executeCommand('find')}
          onHelp={() => editorRef.current.showShortcuts()}
          visualConfig={showVisualConfigurationView}
          onVisualConfigChange={onToggleVisualConfigurationView}
          showMethodConfiguration={configurationType === CONFIGURATION_TYPES.METHOD_CONFIGURATION}
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
          {/*updateTabView(tabs)*/}

          <TabView scrollable onTabClose={(e) => closeTabHandler(e)} onTabChange={(e) => changeTabHandler(e)}>
            {tabs.map((tab) => {
              console.log("in loop", tab);
              return (
              <TabPanel key={tab.id} header={tabHeaderTemplate(tab)} closable>
                <div style={{ height: "500px"}}>{displayContent(tab)}</div>
              </TabPanel>
              );
            })}
            {/* <TabPanel headerTemplate={tabHeaderTemplate(path, name)} closable>
              <div>
                here it should be
                <div>
                  {showEditor && <div className="editor-container">{editorContent}</div>}
                  {!showEditor && <SelectionInformation hint={hint} />}
                </div>
                <p>TEST</p>
                <input type="text" />
              </div>
            </TabPanel> */}
          </TabView>
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
  /** Callback which is triggered when the show yaml button is pressed. */
  showConfigurationDialog: PropTypes.func,
  /** Callback which is triggered when the convert button is pressed. */
  showConvertWarning: PropTypes.func,
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
  /** Array to save all the open tabs in the editor */
  //tabs: PropTypes.array,
};

EditorView.defaultProps = {
  showEditor: true,
  enableButtons: true,
  canSave: true,
  loading: false,
  showVisualConfigurationView: false,
  //tabs: new Array(),
};

export default EditorView;
