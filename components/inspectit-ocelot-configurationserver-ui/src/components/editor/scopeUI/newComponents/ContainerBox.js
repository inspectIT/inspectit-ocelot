import NameSelector from "./NameSelector";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from './AnnotationContainer';
import deepCopy from 'json-deep-copy';
import React, { useRef } from 'react'

import {SplitButton} from 'primereact/splitbutton';
import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
import UpperHeader from "./UpperHeader";

function ContainerBox ( props){
  const { parentAttribute, item, onUpdate, upperHeaderText, count, semantic, children } = props;
  const componentBorderRef = useRef(null);
  // reference for red highlighting over remove icon hover

  const handleMouseOver = (e) => {
    let tooltip = e.target.previousSibling;
    tooltip.style.visibility = 'visible'
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid transparent';
    element.style.boxShadow = '0 0 0 3px red';
  } 

  const handleMouseLeave = (e) => {
    let tooltip = e.target.previousSibling;
    tooltip.style.visibility = 'hidden';
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid black';
    element.style.boxShadow = '';
  }

  const createSplitButtonItems = () => {
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


  const splitButtonItems = createSplitButtonItems();

  return (
    <div ref={componentBorderRef}>
      {/* { !isNaN(index) && <UpperHeader attributeText={'interface, that'} connectionTypeAndOr={'and'} count={index} /> } */}
      <UpperHeader upperHeaderText={upperHeaderText} semantic={semantic} count={count} />
      <div ref={componentBorderRef} style={{ marginBottom: '',  position:'relative', height: '', padding: '25px', background: 'red', borderRadius: '10px' }}>
      { splitButtonItems.length <0 && <SplitButton tooltip="TODO: tooltip? or not" style={{position:'absolute', top:'10px' , right:'10px', zIndex: 9001}} label="add " icon="pi pi-plus" model={splitButtonItems}></SplitButton> }
        
      { children}
      </div>
    </div>
  )

}

export default ContainerBox;