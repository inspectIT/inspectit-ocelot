// import NameSelector from "./NameSelector";
// import LowerHeader from "./LowerHeader";
// import AnnotationContainer from './AnnotationContainer';
// import deepCopy from 'json-deep-copy';

// import {SplitButton} from 'primereact/splitbutton';
// import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
// import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
// import Heading from "./Heading";

// class SelectorBox extends React.Component {
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
//         <SplitButton tooltip="TODO: tooltip? or not" style={{position:'absolute', top:'10px' , right:'10px', zIndex: 9001}} label="add " icon="pi pi-plus" onClick={this.save} model={splitButtonItems}></SplitButton>
          
//         { this.props.children}
//         </div>
//       </div>
//     )
//   }

// }

// export default SelectorBox;