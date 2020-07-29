import {InputText} from 'primereact/inputtext';
import React from 'react';
import deepCopy from 'json-deep-copy';
import MethodsContainerList from './newComponents/Methods';
import ClassSelectorContainer from './newComponents/ClassSelectorContainer';
import { Button } from 'primereact/button';
import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './newComponents/utils/splitButtonItems/invalidLabelsTopDown';
import { SplitButton } from 'primereact/splitbutton';
import { getSplitButtonsItems, enableCreateAttributeWithinSplitItemEntries } from './newComponents/utils/splitButtonItems/getSplitButtonItems';
import ScopeEditor from '../ScopeEditor';
var SimpleUndo = require('simple-undo');

class Scope extends React.Component {
  // the specific icon is set to green, when for e.x scopename, a classSelector, a methodSelector exist
  state = { icon_scopeName: false, icon_classSelector: false , icon_methodSelector: false}
  
  // Distribution of responsibility.
  // the following variables are required to be split from the item. This approach can be used within every other component. 
  // [json,json,array]
  // [type,superclass,interface]
  // splitting up the attributes and letting a component handle the onUpdate of the single items. Distribution of responsibility.
  createGroupedItemsForClassSelector = () => {
    const { scopeObject } = this.props;
    const item = scopeObject;
    
    let groupedClassItems = [];
    let usedClassAttributes = [];
    let classAttributes = ['type', 'interfaces', 'superclass'];
    Object.keys(item).map( attribute => {
      if (classAttributes.includes(attribute)) { 
        groupedClassItems.push(item[attribute])
        usedClassAttributes.push(attribute);
      }
    })
    this.setState({groupedClassItems, usedClassAttributes})
  }

  componentDidMount(){
    this.addEventListenerToBreadCrumbs();
 
    // do not read! Obsolete information and is not in use
    // TODO: manually styling, this must be set through a <style> for the classNames [p-listbox p-inputtext p-component] and [p-listbox-item]
    // const anotherArray = Array.from(document.getElementsByClassName('p-listbox p-inputtext p-component'));
    // anotherArray.map( element => element.style.width= '1400px');
    // const listItems = Array.from(document.getElementsByClassName('p-listbox-item'));
    // listItems.map( item => {
    //   item.style.borderBottom = '1px solid black';
    //   item.style.paddingBottom = '15px';
    // })
  }

  addEventListenerToBreadCrumbs = () => {
    const { scopeName } = this.props.scopeObject;
    // <BreadCrumb> does not enable onClick listener on the elements, we manually get the elements and at the wished functionality
    let breadCrumbArray = Array.from(document.getElementsByClassName('p-breadcrumb p-component')[0].getElementsByClassName('p-menuitem-link'));
    breadCrumbArray.map(element => {
      if( element.innerText === scopeName ) {
        element.addEventListener('click', this.handleBreadCrumbClick);
      }
    })
  }


  onGenericUpdate = ( updatedValue, optionType ) => {
    let { onUpdate, scopeObject } = this.props;
    let updatedItem = deepCopy(scopeObject);
    
    if(Array.isArray(updatedValue) === true) {
      if( updatedValue.length === 0 ) {
        delete updatedItem[optionType];
      } else {
        updatedItem[optionType] = updatedValue;
      }
    } else {  // assumption, the updateValue is a json thus Object.keys
      if ( Object.keys(updatedValue).length === 0) {
        delete updatedItem[optionType];
      } else {
        updatedItem[optionType] = updatedValue;
      }
    }

    // undo redo functionality
    onUpdate(updatedItem);
  }

  
  // do not read, this function is not used
  // thinking how onUpdate must be implemented, when the original item got splitted up
  // thinking about how the reconstruction must look like 
  onGenericGroupUpdate = ( groupUpdate, groupAttributeAlias ) => {
    let { onUpdate, item } = this.props;
    let updatedItem = deepCopy(item);

    // returns an array of the correct attributes. Class Selector => type, interfaces, superclass
    // let toBeUpdatedAttributes = getAttributesFromGroupAlias(); 
    let toBeUpdatedAttributes = ['type', 'superclass','interfaces'];

    //
    toBeUpdatedAttributes.map( attribute => {
      // optimization, only the attributes of the item, which are have changed should be updated (maybe implement)

      // checking if updatedItem needs it value to be deleted (generic assuptiom)
          // checking wether the attribute is an array or object and then if the length is zero
      
      // reconstructing the update into the item
      updatedItem[attribute] = groupUpdate[attribute];
      
      onUpdate(updatedItem);
    })
  }

  // do not read, this function is not used
  // thinking about how to groupUpItems 
  // thinking about how to reconstruced them
  // thinkin about how this is usefull
  // thinking about how this is a generic aspect of a hierarchy object 
  groupAttributesFromItemUnderAnAlias = () => {
    let { scopeObject } = this.props;
    let item = scopeObject;
    let copiedItem = deepCopy(item);

    let groupedUpItems // { groupAlias1: { attribute1: ..., attribute 2:,,, },   groupAlias2: { attribute2: ..., attribute2: }, groupAlias3: ... } 
    // let groupAliasMapping = getGroupAliasMapping(); TODO: implement a strucutre like that above
    let groupAliasMapping = { class: ['interfaces', 'superclass', 'type']  }

    Object.keys(copiedItem).map(itemAttribute => { // 
      // logic for splitting up, analysise
      Object.keys(groupAliasMapping).map( groupAlias => { // groupAlias, class
        let group = {groupAlias };
        groupAliasMapping[groupAlias].map( groupAttribute => { // interface, type, superclass 
          if (itemAttribute === groupAttribute ) { // item example interface, type, superclass, method, x , y , z 
            group[itemAttribute] = copiedItem[itemAttribute];
            delete copiedItem[itemAttribute];
          }
        })
        // { interface, type, superclass }
        copiedItem[groupAlias] = group;
      })
    })
  }

  componentWillMount(){
    // this.groupAttributesFromItemUnderAnAlias();
    this.createGroupedItemsForClassSelector();
  }

  onGroupUpdate = (updatedValues, group) => {
    const { scopeObject, onUpdate } = this.props;
    const item = scopeObject;
    const updatedItem = deepCopy(item);
    group.map( attribute => {
      updatedItem[attribute] = updatedValues[attribute];
    })
    onUpdate(updatedItem);
  }


  addMethod = () => {
    const { scopeObject, onUpdate } = this.props;
    const updatedItem = deepCopy(scopeObject);
    if( updatedItem.methods ) {
      updatedItem.methods.push({name: '', 'matcher-mode': 'EQUALS_FULLY'})
    } else {
      updatedItem.methods = [{name: '', 'matcher-mode': 'EQUALS_FULLY'}]
    }
    onUpdate(updatedItem);
  }

  createSplitButtonItems = (attribute) => {
    const { scopeObject, onUpdate } = this.props;
    const updatedItem = deepCopy(scopeObject);

    let splittButtonItems = getSplitButtonsItems(attribute, updatedItem); 
    // .command key must be added + item must be passed to createAttribute(item), since the function does not have the context to access item
    splittButtonItems = enableCreateAttributeWithinSplitItemEntries(splittButtonItems, updatedItem, onUpdate);
    
    // adjusting the items
    splittButtonItems.map(splittButtonItem => {
      const invalidActionJson = splittButtonItemIsInvalid(updatedItem, splittButtonItem)  // returns object with required information for the following actions or simply false
      if(invalidActionJson) splittButtonItem = adjustInvalidSplitButtonItem(splittButtonItem, invalidActionJson);
    })
    return splittButtonItems;
  }

  handleScopeNameChange = e => {
    this.props.changeName(e.target.value);
  }

  

  render() {
    const { icon_scopeName, icon_classSelector , icon_methodSelector, groupedClassItems, usedClassAttributes} = this.state;


    const { scopeObject, onUpdate, currentlyDisplayScopeName } = this.props;

    const boxStyle = {background:'#EEEEEE', borderBottom: '1.5px solid white', padding: '25px'};
    const validCheckIcon = { border: '2px solid green', color: 'green', borderRadius:'15px' };
    const invalidCheckIcon = {border: '2px solid black', borderRadius:'15px', opacity: '0.1' };
    const marginBottom = '15px';
    const alignItems = 'center';
    const descriptionTextMaxWidth = '300px' // selector description max length, else the row expends to much

    const splitButtonItemsClass = this.createSplitButtonItems('_class', scopeObject);


    return (
      <div className="this">
        <div style={{width:'1121px'}}>

          <div >
            {/* scopenameBox */}
            <div style={{ ...boxStyle}}>
              {/* scopename */}
              <div style={{display:'flex', alignItems}}>
                <h3 style={{marginTop: '0px'}} >scopename</h3>
                <InputText style={{marginLeft: '50px', width: '375px'}} value={currentlyDisplayScopeName} onChange={(e) => this.handleScopeNameChange(e)} />


                <span style={{marginLeft:'.5em'}}>{this.state.value1}</span>
              
                {/* checkIcon for scopeName*/}
                  { !icon_scopeName && (
                    <i style={{...invalidCheckIcon, marginLeft: '5px' }}className="pi pi-check"></i>
                  )}
                  { icon_scopeName && (
                    <i style={{...validCheckIcon}}className="pi pi-check"></i>
                  )}
              </div>
            </div>

            {/* classSelectorBox */}
            <div style={{ ...boxStyle}}>
              <div style={{display:'flex', alignItems}}>
                <h3>class matcher</h3> 
                  {/* checkIcon for classSelector */}
                  { !icon_classSelector && (
                    <i style={{...invalidCheckIcon, marginLeft: '15px' }}className="pi pi-check"></i>
                  )}
                  { icon_classSelector && (
                    <i style={{...validCheckIcon}}className="pi pi-check"></i>
                  )}

              </div>
              <div style={{display:'flex', alignItems, marginBottom}}>

                {/* descriptionText */}
                <div style={{display:'inline-block', marginRight: '15px', alignContent: 'center', marginTop: '25px', border:'1px solid grey', background:'white', borderRadius:10, padding:15}}>
                  <span style={{display: 'inline-block'}}> 
                  In order to determine which methods should be instrumented all classes are checked against the defined interface, superclass and type matchers. If and only if a class matches on all matchers, each of their methods is checked against the defined method matchers below #
                  in order to determine the target methods. Minimum 1 class matcher must exist.
                  <span style={{color:'red'}}> Keep the diffrence in mind that while all of the type matchers have to match to determine a class, only one of the method matchers below has to match!</span>
                  </span>
                </div>

                {/* placeholder for adding attributes via splitbutton or placeholder */}

              </div>

              {this.props.text.classText && 
                    <div style={{padding:'10px', borderRadius:'10px', border: '1px solid red', marginBottom: '15px'}}>
                     <p> { this.props.text.classText } </p>
                    </div>
                  }

              {/* classSelector here */}
              <div id='classSelectorContainer' style={{width:'', background:'white', minHeight: '200px',  padding:'35px'}}>
              {/* TODO: Hint nicht wie die nächsten 3 Zeilen. Keine Information darüber, der wie vielte Class Selector gechained wurde 
               <Item item={scopeObject.type} />
               <Item item={scopeObject.superclass} />
               <Item item={scopeObject.annotations} />
              */}

              {/* The keys of the scopeObject are [interface, type, superclass, method, advanced ]
              we filter out method and advanced
              we use map on the filteredAttributeArray and get an index. The index is used to know display wether 'the class'  or '... and the class'  */}
                { splitButtonItemsClass.length >0  && <SplitButton style={{position:'relative', left:'50%' }} tooltip="specify classes by their implemented interfaces, superclass, or by the class name (and its attached annotations) " label='more options'  model={splitButtonItemsClass}></SplitButton> }
                <h4>To pick through the methods of a class, it must fullfill all of the following options </h4>
                { !scopeObject.interfaces && !scopeObject.type && !scopeObject.superclass && <p> no class matcher choosen, please specify the class by 1 option</p>} 
                <ClassSelectorContainer scopeObject={scopeObject} onUpdate={onUpdate} />


               {/* selectorType should be removed, i use it here to not duplicate selectorContainer, because only the "heading" changes.  */}
               {/* <GenericListContainer items={groupedClassItems} onUpdate={(updatedValue) => this.onGroupUpdate(updatedValue, usedClassAttributes)} selectorType='Class' />  selectorType should be removed, i use it here to not duplicate selectorContainer, because only the "heading" changes.  */}
              
              </div>
            </div>

            {/* methodSelector box */}
            <div style={{ ...boxStyle}}>
              <h3 style={{marginTop: '0px', marginBottom}}>method selectors</h3>

              {/* descriptionText */}
              <div style={{display:'flex', alignItems, marginBottom}}>
                <div style={{display:'inline-block', marginRight: '15px', alignContent: 'center', marginTop: '25px', border:'1px solid grey', background:'white', borderRadius:10, padding:15}} >
                  <span style={{display: 'inline-block'}}> 
                    hand-pick a method or group of methods by using a selector. Use the options to specify the selector. Each list-item wields a specific selector.
                  </span>

                </div>

                 {/* placeholder for adding attributes via splitbutton or placeholder */}
              
                {/* checkIcon for methodSelector */}
                { !icon_methodSelector && (
                  <i style={{...invalidCheckIcon , marginLeft: '15px'}}className="pi pi-check"></i>
                  )}
                { icon_methodSelector && (
                  <i style={{...validCheckIcon}}className="pi pi-check"></i>
                  )}
              </div>
                  {this.props.text.methodText && 
                    <div style={{padding:'10px', borderRadius:'10px', border: '1px solid red', marginBottom: '15px'}}>
                     <p> { this.props.text.methodText } </p>
                    </div>
                  }
                  {/* <React.Fragment>
                    <Heading attributeText={'class'} connectionTypeAndOr={'and'} count={count} />
                    { 
                      Array.isArray(scopeObject[attribute]) &&
                      <InterfaceListContainer backgroundColor='#EEEEEE' onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} items={scopeObject[attribute]} parentAttribute={attribute} />} 
                    { 
                    !Array.isArray(scopeObject[attribute]) && <Item backgroundColor='#EEEEEE' item={scopeObject[attribute]} parentAttribute={attribute} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)}/> 
                    }
                  </React.Fragment> */}
                <div style={{width:'', background:'white', minHeight: '200px',  padding:'35px'}}>
                  <Button onClick={this.addMethod} label="add new method matcher" tooltip='add a method selector. Describe with the + button, which option the methods must fullfill' icon="pi pi-plus" style={{left:'50%', top:'10px'}}></Button>

                  { !scopeObject.methods && <p> No method matcher, please specify the method you want by using the add new matcher</p>}
                  { scopeObject.methods && <MethodsContainerList items={scopeObject.methods} parentAttribute={'methods'} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, 'methods')} /> }
                </div>
              {/* obsolete, do not read , this will be removed */}
              {/* <ListBox value={scopeObject.methods} style={{ witdh: '800px' }} options={scopeObject.methods} onChange={(e) => this.setState({selectedCity: e.value})} 
              optionLabel="name" itemTemplate={this.methodSelectorListTemplate} /> */}
              
              <Button onClick={this.props.showOverview} style={{position:'relative', left:'90%', marginTop:'20px'}} label="display result"></Button>
            </div>
          </div>
        </div>
           
      </div>
    );
  }
}

export default Scope;
