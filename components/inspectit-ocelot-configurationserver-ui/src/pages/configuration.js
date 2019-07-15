import React from 'react'
import MainLayout from '../layout/MainLayout'
import Head from 'next/head'
import dynamic from 'next/dynamic'
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';
import { ScrollPanel } from 'primereact/scrollpanel';
import ConfigurationView from '../components/views/configuration/ConfigurationView';

const AceEditor = dynamic(() => import('../components/editor/AceEditor'), {
  ssr: false
});

import { BASE_PAGE_TITLE } from '../data/constants'
import yamlEditorConfig from '../data/yaml-editor-config.json'


/**
 * The configuration page. Users can manage their configurations files on this page.
 */
class ConfigurationPage extends React.Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <MainLayout>
        <Head>
          <title>{BASE_PAGE_TITLE} | Configuration</title>
        </Head>

        <ConfigurationView />
        <div className="p-grid p-dir-col p-nogutter" style ={{height : "100%"}}>
          <div className="p-col-fixed">
            <Toolbar style={{padding: "0.2rem"}}>
              <div className="p-toolbar-group-left">
                  <div className="ocelot-text">my-cool-file.yml</div>
              </div>
              <div className="p-toolbar-group-right">
                <Button icon="pi pi-question" style={{ marginRight: '.25em' }} onClick = {() => this.editor.showKeyboardShortcuts()} />
                <Button icon="pi pi-search" style={{ marginRight: '.25em' }} onClick = {() => this.editor.execCommand("find")} />
                <Button icon="pi pi-save" style={{ marginRight: '2' }} />
              </div>
            </Toolbar>
          </div>
          <div className="p-col">
            <AceEditor mode="yaml" theme="cobalt" initEditor={this.initEditor} options={yamlEditorConfig}/>
          </div>
        </div>
      </MainLayout>
    )
  }

  /**
   * 
   */
  initEditor = (editor) => {
    this.editor = editor;
  }


}

export default ConfigurationPage;
