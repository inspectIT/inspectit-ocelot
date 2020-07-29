import {ListBox} from 'primereact/listbox';

import { cloneDeep, isEqual, set, unset, create, update } from 'lodash';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { ColumnGroup } from 'primereact/columngroup';
import { InputText } from 'primereact/inputtext';
import { Menubar } from 'primereact/menubar';
import { Message } from 'primereact/message';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React from 'react';
import Scope from './scopeUI/Scope';
import { BreadCrumb } from 'primereact/breadcrumb';
// import './scopeUI/newComponents/attribute_explanations/scope9001.scoped.css';  // scoped ? 

// import { descriptionScope } from './scopeUI/newComponents/attribute_explanations/scope.js';

import {Dialog} from 'primereact/dialog';

// Faster deep-copy for json style object.

// Features
// Simplified lodash.deepClone, maybe 15 times faster when copy many compilcated objects.
// Only support plain objects with number, string, array, boolean
import deepCopy from 'json-deep-copy'


/**
 * Editor for showing the scopes of the config file as UI
 */
class ScopeEditor extends React.Component {

  state = { 
    showOverview: true,
    breadCrumbItems: [
      { label: 'Scope Overview' },
    ],
    currentlyDisplayScopeName: '',
    History: [],
    text: {}
  }

  componentDidMount(){
    this.addEventListenerToBreadCrumbs();
    this.setScopeNamesFromConfig();
  }

  // { !showOverview && ( <Scope ... to either display the Scopelist or the single scope 
  displayOverview = () => this.setState({ showOverview: true})

  handleOnEdit = () => this.setState({ showOverview: false});

  handleDoubleClick = (e) => {
    const name = e.target.dataset.name;
    const { config } = this.props;

    // TODO: inserting name into scopeObject is obsolete, since onUpdate= (updatedValue) => this.onUpdate(updatedValue, scopeName)
    // // getting the correct single scopeObject to pass as props
    let scopeObject = config.inspectit.instrumentation.scopes[name];
    // scopeObject['scopeName'] = name;

    const breadCrumbItems = [{'label': 'Scope Overview'}, {'label': name }];
    this.setState({ scopeObject, showOverview: false, breadCrumbItems, currentlyDisplayScopeName: name})
  }
      
  setScopeNamesFromConfig = () => {
    // asserting that accessing attributes does not happen on undefined. Since sometimes the inspectIt wasnt passed
    // TODO: hint: the scopeEditor is always shown, for each yml file. thinking about, how to safely access the props.
    // thinking about , if it is empty, it should still display the empty scopelist, and let the user create a "empty scope"
    if( this.props.config && this.props.config.inspectit &&  this.props.config.inspectit.instrumentation && this.props.config.inspectit.instrumentation.scopes) {
      let scopes = this.props.config.inspectit.instrumentation.scopes;
      let scopeNameList = [];
      Object.keys(scopes).map(name => {
        scopeNameList.push({label: name})
      })
      this.setState({scopeNameList})
    }
  }

  // TODO:
  handleBreadCrumbClick = () => {
    this.setState({ breadCrumbItems: [{ label: 'Scope Overview' }]});
    this.displayOverview();
  }

  // TODO: c-important. Please do not use limited time on less important code for the month goal.
  // <BreadCrumb> elements nagivate to other urls. example     { label: 'Lionel Messi', url: 'https://en.wikipedia.org/wiki/Lionel_Messi' }
  // <BreadCrumb> does not enable onClick listener on the elements, this manually adding.
  addEventListenerToBreadCrumbs = () => {   
    // Array.from(htmlCollection)
    // let breadCrumbArray = Array.from(document.getElementsByclassNameName('p-breadcrumb p-component')[0].getElementsByclassNameName('p-menuitem-link'));
    // breadCrumbArray.map(element => {
    //   if( element.innerText === 'Scope Overview' ) {
    //     element.addEventListener('click', this.handleBreadCrumbClick);
    //   }
    // })
  }

  updateBreadCrumbs = breadCrumbItems => this.setState({ breadCrumbItems});
  
  itemTemplate = (option) => {
    return (
        <div >
          <p data-name={option.label} onDoubleClick={this.handleDoubleClick}> {option.label} </p>
        </div>
    );
  }

  onUpdate = ( updatedValue, scopeName ) => {
    console.log( 'Freezer', config );
    let { onUpdate, config } = this.props;
    // updating the scopeObject in state
    // this variable is being passed down, and must be updated
    // this variable is only setted in the doubleClick elsewise.
    let copyConfig = deepCopy(config)
    this.setState({ scopeObject: updatedValue})

    // TODO: überlegung. Kann bei keinen angelegten scopes überhaupt ...instrumentation.scopes <-- ein Attribute gesetzt werden ? 
    copyConfig.inspectit.instrumentation.scopes[scopeName] = updatedValue;
    onUpdate(copyConfig);
  }


  onClick(name, position) {
    let state = {
        [`${name}`]: true
    };

    if (position) {
        state = {
            ...state,
            position
        }
    }

    this.setState(state);
  }

  onHide(name) {
      this.setState({
          [`${name}`]: false
      });
  }

    renderFooter(name) {
      return (
          <div>
              <Button label="Yes" icon="pi pi-check" onClick={() => this.onHide(name)} />
              <Button label="No" icon="pi pi-times" onClick={() => this.onHide(name)} classNameName="p-button-secondary"/>
          </div>
      );
  }

  onClick = (e) => {
    const { config, onUpdate } = this.props;
    let updatedConfig = deepCopy(config);
    const scopeName = 'insert here a name for your scope'

    console.log(updatedConfig);
    if(!updatedConfig) {
      updatedConfig = { inspectit : { instrumentation : { scopes: { }}}};
    } else if ( !updatedConfig.inspectit) {
      updatedConfig.inspectit = { instrumentation : { scopes: {}}}
    }

    console.log('hierx', updatedConfig);

    const createType = e.target.parentElement.dataset.createtype;
    if ( createType === 'createNew') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 

      }
    }
    else if ( createType === 'a' ) {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        type: {'name': 'insert here the class name', 'matcher-mode': 'EQUALS_FULLY'}, 
        methods: [{name: 'insert here method name', 'matcher-mode': 'EQUALS_FULLY'}]
      }
      this.setState({text:{classText:' HEEEEEEEEEEEEEEEEEEEEEEEEEELO'}})
    } else if (createType === 'b') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        type: {'name': 'use dropdown', 'matcher-mode': 'STARTS_WITH', 
        annotations: [{'name': 'or use a common annotation', 'matcher-mode': 'EQUALS_FULLY'}]
      }, 
        methods: [{name: 'insert here method name', 'matcher-mode': 'EQUALS_FULLY'}]
      }
      this.setState({text: {classText:'To target multiple classes, you can use their common name, their common annotation, their common interface, their common superclass. Be aware having multiple options, restricts the targeted classes, because all options must be fullfilled'}})
    } else if (createType === 'c') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        type: {'name': 'insert here the class name', 'matcher-mode': 'EQUALS_FULLY'}, 
        methods: [{name: 'use dropdown, add common option, add new method matcher option set', 'matcher-mode': 'EQUALS_FULLY',}]
      }
      this.setState({text: {methodText: 'You can describe multiple methods, by their their common visibility property, their common annotations, their common arguments, their common method name for example'}})
    } else if (createType === 'd') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        interfaces: [{'name': 'insert here the class name', 'matcher-mode': 'EQUALS_FULLY'}], 
        methods: [{name: 'insert here method name', 'matcher-mode': 'EQUALS_FULLY',}]
      }
      this.setState({text: {methodText: 'If you know the names of multiple methods, you can name each in another selector box. Click "add method selector" '}})
    } else if (createType === 'e') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        type: { annotations: [{'name': 'insert here the class name', 'matcher-mode': 'EQUALS_FULLY'}] }, 
        methods: [{name: 'insert here method name', 'matcher-mode': 'EQUALS_FULLY',}]
      }
      this.setState({text: {methodText: 'If you know the names of multiple methods, you can name each in another selector box. Click "add method selector" '}})
    } else if (createType === 'f') {
      updatedConfig.inspectit.instrumentation.scopes[scopeName] = { 
        superclass: {'name': 'insert here the class name', 'matcher-mode': 'EQUALS_FULLY'}, 
        methods: [{name: 'insert here method name', 'matcher-mode': 'EQUALS_FULLY',}]
      }
      this.setState({text: {methodText: 'If you know the names of multiple methods, you can name each in another selector box. Click "add method selector" '}})
    }
    
    const breadCrumbItems = [{'label': 'Scope Overview'}, {'label': scopeName }];
    this.setState({ scopeObject: updatedConfig.inspectit.instrumentation.scopes[scopeName], showOverview: false, breadCrumbItems, currentlyDisplayScopeName: scopeName})

    console.log('xxa', updatedConfig);
    onUpdate(updatedConfig);
  }

  changeName = value => {
    const { config, onUpdate } = this.props;
    const copiedScope = deepCopy(config.inspectit.instrumentation.scopes[this.state.currentlyDisplayScopeName])
    const copiedScopes = deepCopy(config.inspectit.instrumentation.scopes)
    const updatedConfig = deepCopy(config);

    delete copiedScopes[this.state.currentlyDisplayScopeName];
    copiedScopes[value] = copiedScope;

    updatedConfig.inspectit.instrumentation.scopes = copiedScopes;
    const breadCrumbItems = [{'label': 'Scope Overview'}, {'label': value }];
    this.setState({currentlyDisplayScopeName: value, breadCrumbItems})
    onUpdate(updatedConfig);
  }

  showOverview = () => {
    this.setState({showOverview: true})
    this.props.showAceEditor();
  }

  render() {
    const { config, ...rest } = this.props;
    const { scopeNameList, showOverview, scopeObject, breadCrumbItems, currentlyDisplayScopeName ,text } = this.state;

    // TODO: consistent styling of all toolbarButtonStyles?
    const toolbarButtonStyle = {padding: '5px' , background: 'rgb(139, 172, 189)', margin: '5px'};
    const itemStyle = { background: 'white', padding: '15px', borderRadius:'10px', marginBottom: '15px'};

    return (
      <div classNameName="this">
        {/* the style here was copied from i believe tree table, it gives good indications about how the page should look like, so i used it */}
        <style jsx>{`
          .this {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            min-width: 760px;
          }
          .this :global(.p-menubar) {
            background-color: #f4f4f4;
          }
          .this :global(.p-menuitem-text) {
            font-size: smaller;
          }
          .this :global(.p-menuitem-icon) {
            font-size: smaller;
          }
          .errorBox {
            align-self: center;
            justify-content: center;
            flex: 1;
            display: flex;
            flex-direction: column;
            color: #bbb;
          }
          .this :global(.composite-row),
          .this :global(.composite-row) :global(.key-column) {
            color: grey;
            font-weight: normal;
          }
          .this :global(.key-column) {
            color: black;
            font-weight: bold;
          }
          .this :global(.value-column) {
            font-family: monospace;
          }
          .this :global(.edit-text) {
            align-self: center;
            align-items: center;
            margin-inline-end: 1em;
          }
          .this :global(.p-treetable .p-treetable-tbody > tr > td.p-cell-editing .p-button) {
            width: unset;
            min-width: 96px;
          }
        `}</style>


        <div>
          <BreadCrumb model={breadCrumbItems} />
          {
            showOverview && (
              <div style={{ padding: '25px', borderRadius: '10px' , marginLeft: '15px', marginTop: '25px', background:'#EEEEEE '}} classNameName="content-section implementation">

              <h4>The following scopes exist within the selected file</h4>
              <div style={{...itemStyle}}>
                <ListBox filter={true} filterPlaceholder="Search" options={scopeNameList} itemTemplate={this.itemTemplate}
                  style={{width:'500px'}} listStyle={{}}/>
                {/* <div style={{ margin: '25px'}}>
                  <Button onClick={this.createOption} label="create new" style={{ ...toolbarButtonStyle}}> </Button>
                  <Button onClick={this.handleOnEdit} label="edit" style={{ ...toolbarButtonStyle}}> </Button>
                  <Button onDoubleClick={this.deleteOption} label="delete" style={{ ...toolbarButtonStyle}}> </Button>
                </div> */}

              </div>
                <div style={{marginTop:'25px'}}>
                  <div >
                    <div style={{display:'inline-block', marginRight: '15px', alignContent: 'center', marginTop: '25px', border:'1px solid grey', background:'white', borderRadius:10, padding:15}} >
                      <p>create new scope</p>
                      <Button style={{marginBottom: '15px', marginLeft: '15px' }} data-createtype='createNew' onClick={this.onClick} label='create ' ></Button>
                    </div>

                    <div style={{display:'inline-block', height:'135px', marginRight: '15px', alignContent: 'center', marginTop: '25px', border:'1px solid grey', background:'white', borderRadius:10, padding:15}} >
                    <p> What is a scope</p> 
                    <Button style={{marginLeft:'15px'}} icon="pi pi-question"  tooltip=" A single scope can be considered as a set of methods within a set of class or classes and is used by rules to determine which instrumentation should apply to which method."></Button>
                    </div>
                  </div>

                    <h4 style={{marginBottom:0}}> Quick start</h4>
                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}>1 method, within 1 class </span></p>
                    <p> The method and class name are known to me</p>
                    <Button data-createtype='a' onClick={this.onClick} label='create'></Button>
                  </div>

                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}> 1 method, within multiple classes </span></p>
                    <p> The method name is known to me</p>
                    <Button data-createtype='b' onClick={this.onClick} label='create'></Button>
                  </div>

                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}>multiple methods, within 1 class</span></p>
                    <p> The class name is known to me</p>
                    <Button data-createtype='c' onClick={this.onClick } label='create'></Button>
                  </div>

                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}>a class, which implements an interface</span></p>
                    <p> The interface name is known to me</p>
                    <Button data-createtype='d' onClick={this.onClick } label='create'></Button>
                  </div>

                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}>a class, which has an specific annotation</span></p>
                    <p> The annotation is known to me</p>
                    <Button data-createtype='e' onClick={this.onClick } label='create'></Button>
                  </div>

                  <div style={{...itemStyle, marginRight:'15px', display:'inline-block', alignContent: 'center', borderRadius:10, padding:15}}>
                    <p> The scope should trace <span style={{color:'red'}}>a class, that inherits from a superclass</span></p>
                    <p> The superclass name is known to me</p>
                    <Button data-createtype='f' onClick={this.onClick } label='create'></Button>
                  </div>

          
                </div>
              </div>
          )}
          { !showOverview && (
            <Scope showOverview={this.props.showAceEditor} changeName={this.changeName} text={text} currentlyDisplayScopeName={currentlyDisplayScopeName} scopeObject={scopeObject} updateBreadCrumbs={this.updateBreadCrumbs} onUpdate={(updatedValue) => this.onUpdate(updatedValue, currentlyDisplayScopeName)} />
            )} 
        </div>
      </div>
    );
  }
}

ScopeEditor.propTypes = {
  /** The configuration object */
  config: PropTypes.object,
  /** Function to invoke for full config update */
  onUpdate: PropTypes.func,
};

export default ScopeEditor;
