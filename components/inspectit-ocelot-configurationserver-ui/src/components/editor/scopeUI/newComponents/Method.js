import NameSelector from "./NameSelector";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from './AnnotationContainer';
import deepCopy from 'json-deep-copy';

import {SplitButton} from 'primereact/splitbutton';
import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
import MethodVisibility from "./MethodVisibility";
import Name from "./Name";
import ValueBox from "./ValueBox";
import BooleanItem from "./BooleanItem";
import SelectorBox from "./SelectorBox";
import Annotations from "./Annotations";

class Method extends React.Component {
  state = { splitMenuItems: [] }

  // reference for red highlighting over remove icon hover
  componentBorderRef = React.createRef();

  handleMouseOver = (e) => {
    let tooltip = e.target.previousSibling;
    tooltip.style.visibility = 'visible'
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid transparent';
    element.style.boxShadow = '0 0 0 3px red';
  } 

  handleMouseLeave = (e) => {
    let tooltip = e.target.previousSibling;
    tooltip.style.visibility = 'hidden';
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid black';
    element.style.boxShadow = '';
  }


  onGenericUpdate = ( updatedValue, optionType ) => {
    let { onUpdate, item } = this.props;
    let updatedItem = deepCopy(item);
    
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
    onUpdate(updatedItem);
  }

  // eigene information an mich, kann übersprungen werden beim review
  // // Objekt transformieren und zurück transformieren 
  // // generische Schnittstelle 
  // // TODO: obsolete wenn keine generische schnittstelle
  // onUpdateNameSelector = (updatedNameSelector) => {
  //   // Änderungen müssen hier getan werden, name
  //   // name - matcher-mode , annotations unberührt 
  //   // superclass { }   ,  { name, matcher-mode } 
  //   const { item , onItemUpdate } = this.props;
  //   const updated_item = deepCopy(item);
  //   updated_item 

  //   onItemUpdate(updatedItem);
  // }

  createSplitButtonItems = () => {
    const { parentAttribute, item, onUpdate } = this.props; 
    let splittButtonItems = getSplitButtonsItems(parentAttribute, item); 
    // .command key must be added + item must be passed to createAttribute(item), since the function does not have the context to access item
    splittButtonItems = enableCreateAttributeWithinSplitItemEntries(splittButtonItems, item, onUpdate);
    
    // adjusting the items
    splittButtonItems.map(splittButtonItem => {
      const invalidActionJson = splittButtonItemIsInvalid(item, splittButtonItem)  // returns object with required information for the following actions or simply false
      if(invalidActionJson) splittButtonItem = adjustInvalidSplitButtonItem(splittButtonItem, invalidActionJson);
    })
    return splittButtonItems;
  }

  render() {
    const { item } = this.props;

    console.log('genericJsonWrapper', this.props)
    // mehrere attribute method visiblity annotaions x y bz e 
    return (
      <SelectorBox parentAttribute='methods'  item={item} onUpdate={(updatedValue) => this.props.onUpdate(updatedValue)}>
        {Object.keys(item).map( attribute => 
          <React.Fragment>
            
            { attribute === 'name' && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><Name text={' ... has a name, that ' } onUpdate={(updatedValue) => this.props.onUpdate(updatedValue)} item={item} /> </ValueBox> }
            { attribute === 'visibility' && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><MethodVisibility onUpdate={(updateObj) => this.onGenericUpdate(updateObj, 'visibility')} item={item[attribute]}  /> </ValueBox> }
            { attribute === 'annotations' && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><Annotations onUpdate={(updateObj) => this.onGenericUpdate(updateObj, 'annotations')} items={item[attribute]} optionType={'Biene '} /> </ValueBox>}
            {/* { attribute === 'arguments' && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.props.onUpdate(updatedValue, attribute)} ><MethodItem onUpdate={(updateObj) => this.onGenericUpdate(updateObj, 'name')} item={item} /> </ValueBox> } */}
            { attribute === 'is-constructor' && attribute && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><BooleanItem onUpdate={(updateObj) => this.onGenericUpdate(updateObj, 'is-constructor')} text={'is a constructor'} item={item[attribute]} /> </ValueBox> }
            { attribute === 'is-synchronized' && attribute && <ValueBox  item={item} attributesToDelete={[attribute]} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><BooleanItem onUpdate={(updateObj) => this.onGenericUpdate(updateObj, 'is-synchronized')} text={'is synchronized'} item={item[attribute]} /> </ValueBox> }
          </React.Fragment>
        )}
      </SelectorBox>
    )
  }

}

export default Method;