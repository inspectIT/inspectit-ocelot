import NameSelector from "./NameSelector";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from './AnnotationContainer';
import deepCopy from 'json-deep-copy';
import React, { useRef } from 'react'

import {SplitButton} from 'primereact/splitbutton';
import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
import Heading from "./Heading";

// add attributes into item
// remove item from parentItem
function SelectorBox ( props){
  const { attribute, item, onUpdate, text, count, semantic, children , style ,indexOnDelete} = props;
  const componentBorderRef = useRef(null);
  // reference for red highlighting over remove icon hover

  const handleMouseOver = (e) => {
    // let tooltip = e.target.previousSibling;
    // tooltip.style.visibility = 'visible'
    const element = componentBorderRef.current
    // element.style.border = '1px solid transparent';
    element.style.boxShadow = '0 0 0 3px red';
  } 

  const handleMouseLeave = (e) => {
    // let tooltip = e.target.previousSibling;
    // tooltip.style.visibility = 'hidden';
    const element = componentBorderRef.current
    // element.style.border = '1px solid black';
    element.style.boxShadow = '';
  }

  const createSplitButtonItems = () => {
    let splittButtonItems = getSplitButtonsItems(attribute, item); 
    // .command key must be added + item must be passed to createAttribute(item), since the function does not have the context to access item
    splittButtonItems = enableCreateAttributeWithinSplitItemEntries(splittButtonItems, item, onUpdate);
    
    // adjusting the items
    splittButtonItems.map(splittButtonItem => {
      const invalidActionJson = splittButtonItemIsInvalid(item, splittButtonItem)  // returns object with required information for the following actions or simply false
      if(invalidActionJson) splittButtonItem = adjustInvalidSplitButtonItem(splittButtonItem, invalidActionJson);
    })
    return splittButtonItems;
  }

  const deleteItem = () => {
    onUpdate({});
  }
 
  const splitButtonItems = createSplitButtonItems();

  console.log(item)
  return (

      <div style={{width:'100%'}}>
      {/* { !isNaN(index) && <Heading attributeText={'interface, that'} connectionTypeAndOr={'and'} count={index} /> } */}
        
      <div ref={componentBorderRef} style={{ ...style, width: '100%', position:'relative', height: '', padding: '25px', background: '#EEEEEE' , borderRadius: '10px', marginTop: '25px', marginBottom: '25px' }}>
      <Heading  attribute={attribute} text={text} semantic={semantic} count={count} />
      { splitButtonItems.length >0  && <SplitButton tooltip="TODO: tooltip? or not" style={{position:'absolute', top:'10px' , right:'10px', zIndex: 9001}} label="add " icon="pi pi-plus" model={splitButtonItems}></SplitButton> }
      <i onMouseOver={handleMouseOver} onMouseLeave={handleMouseLeave} onClick={deleteItem} style={{ position: 'absolute', bottom:'-30px', right: '0px', fontSize:'30px',  color: 'red', opacity:'0.8'}} className="pi pi-times-circle"/>
        
      { children}
      </div>
    </div>


  )
}

export default SelectorBox;