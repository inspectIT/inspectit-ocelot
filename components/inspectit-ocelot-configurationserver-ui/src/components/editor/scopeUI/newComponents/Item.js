// import ValueBox from "./ValueBox";
// import LowerHeader from "./LowerHeader";
// import AnnotationContainer from './AnnotationContainer';
// import deepCopy from 'json-deep-copy';

// import {SplitButton} from 'primereact/splitbutton';
// import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
// import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
// import Heading from "./Heading";
// import Name from "./Name";

// class Item extends React.Component {
//   state = { splitMenuItems: [] }

//   // reference for red highlighting over remove icon hover
//   componentBorderRef = React.createRef();

//   handleMouseOver = (e) => {
//     let tooltip = e.target.previousSibling;
//     tooltip.style.visibility = 'visible'
//     const element = this.componentBorderRef.current
//     // element.style.border = '1px solid transparent';
//     element.style.boxShadow = '0 0 0 3px red';
//   } 

//   handleMouseLeave = (e) => {
//     let tooltip = e.target.previousSibling;
//     tooltip.style.visibility = 'hidden';
//     const element = this.componentBorderRef.current
//     // element.style.border = '1px solid black';
//     element.style.boxShadow = '';
//   }


//   onGenericUpdate = ( updatedValue, optionType ) => {
//     let { onUpdate, item } = this.props;
//     let updatedItem = deepCopy(item);
    
//     if(Array.isArray(updatedValue) === true) {
//       if( updatedValue.length === 0 ) {
//         delete updatedItem[optionType];
//       } else {
//         updatedItem[optionType] = updatedValue;
//       }
//     } else {  // assumption, the updateValue is a json thus Object.keys
//       if ( Object.keys(updatedValue).length === 0) {
//         delete updatedItem[optionType];
//       } else {
//         updatedItem[optionType] = updatedValue;
//       }
//     }
//     onUpdate(updatedItem);
//   }

//   // eigene information an mich, kann übersprungen werden beim review
//   // // Objekt transformieren und zurück transformieren 
//   // // generische Schnittstelle 
//   // // TODO: obsolete wenn keine generische schnittstelle
//   // onUpdateNameSelector = (updatedNameSelector) => {
//   //   // Änderungen müssen hier getan werden, name
//   //   // name - matcher-mode , annotations unberührt 
//   //   // superclass { }   ,  { name, matcher-mode } 
//   //   const { item , onItemUpdate } = this.props;
//   //   const updated_item = deepCopy(item);
//   //   updated_item 

//   //   onItemUpdate(updatedItem);
//   // }

//   createSplitButtonItems = () => {
//     const { parentAttribute, item, onUpdate } = this.props; 
//     let splittButtonItems = getSplitButtonsItems(parentAttribute, item); 
//     // .command key must be added + item must be passed to createAttribute(item), since the function does not have the context to access item
//     splittButtonItems = enableCreateAttributeWithinSplitItemEntries(splittButtonItems, item, onUpdate);
    
//     // adjusting the items
//     splittButtonItems.map(splittButtonItem => {
//       const invalidActionJson = splittButtonItemIsInvalid(item, splittButtonItem)  // returns object with required information for the following actions or simply false
//       if(invalidActionJson) splittButtonItem = adjustInvalidSplitButtonItem(splittButtonItem, invalidActionJson);
//     })
//     return splittButtonItems;
//   }

//   render() {
//     const background_bigDiv = "#EEEEEE";   
//     const { item, parentAttribute, index, onUpdate } = this.props;

//     const splitButtonItems = this.createSplitButtonItems();

//     console.log('item', this.props)

//     return (
//       <div >
//         {/* <button> {optionText} </button> */}
//         { !isNaN(index) && <Heading attributeText={'interface, that'} connectionTypeAndOr={'and'} count={index} /> }
//         <div ref={this.componentBorderRef} style={{ marginBottom: '',  position:'relative', height: '', padding: '25px', background: background_bigDiv, borderRadius: '10px' }}>
//           {parentAttribute !== 'interfaces' && parentAttribute !== 'methods' && <LowerHeader optionType={parentAttribute} />}
//           <ValueBox  onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} >
//            <Name text={`has a name`}  onUpdate={onUpdate} style={{background: 'yellow'}} item={item} index={index}  />
//           </ValueBox> 
//           {item.annotations && <AnnotationContainer onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, 'annotations')} items={item.annotations} optionType={parentAttribute} />}
//           <SplitButton tooltip="TODO: tooltip? or not" style={{position:'absolute', top:'10px' , right:'10px'}} label="add " icon="pi pi-plus" onClick={this.save} model={splitButtonItems}></SplitButton>
//         </div>
        
//         <div style={{ position: 'relative', height: '20px' , display: 'flex', marginBottom: '5px',}}>
//           <p style={{ visibility: 'hidden' , color:'red', position:'absolute', right:'35px', marginTop:'-3px'}}> remove this option </p>
//           <i data-tobehighlighted={parentAttribute} onClick={this.removeOption} onMouseOver={this.handleMouseOver} onMouseLeave={this.handleMouseLeave} style={{ position: 'absolute', right: '5px', bottom:'-5px', fontSize:'30px',  color: 'red', opacity:'0.8'}} className="pi pi-times-circle"></i>
//         </div>
//       </div>
//     )
//   }

// }

// export default Item;