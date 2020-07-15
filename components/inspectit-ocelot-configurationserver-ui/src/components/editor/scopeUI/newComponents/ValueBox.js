import NameSelector from "./NameSelector";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from './AnnotationContainer';
import deepCopy from 'json-deep-copy';

import {SplitButton} from 'primereact/splitbutton';
import { getSplitButtonsItems , enableCreateAttributeWithinSplitItemEntries} from './utils/splitButtonItems/getSplitButtonItems';
import { splittButtonItemIsInvalid, adjustInvalidSplitButtonItem } from './utils/splitButtonItems/invalidLabelsTopDown';
import UpperHeader from "./UpperHeader";

class ValueBox extends React.Component {
  state = { splitMenuItems: [] }

  // reference for red highlighting over remove icon hover
  componentBorderRef = React.createRef();

  handleMouseOver = (e) => {
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid transparent';
    element.style.boxShadow = '0 0 0 3px red';
  } 

  handleMouseLeave = (e) => {
    const element = this.componentBorderRef.current
    // element.style.border = '1px solid black';
    element.style.boxShadow = '';
  }

  deleteItem = (e) => {
    const { onUpdate } = this.props;
    onUpdate({})
  }
 
  render() {
    const background_bigDiv = "red";   
    const { item, parentAttribute, index, onUpdate } = this.props;
 

    return (
        <div  ref={this.componentBorderRef}  style={{display: 'inline-flex',  marginBottom: '5px', position:'relative', background: 'white', padding: '10px 30px 0px 10px', borderRadius:'10px'}}>
          {/* Wird keine addAttribute Möglichkeit eingeholt, soll auch kein SplittButton angezeigt werden */}

          <i onMouseOver={this.handleMouseOver} onMouseLeave={this.handleMouseLeave} onClick={this.deleteItem} style={{ position: 'absolute', bottom:'0px', right: '0px', fontSize:'30px',  color: 'red', opacity:'0.8'}} className="pi pi-times-circle"/>
          { this.props.children}
        </div>
    )
  }

}

export default ValueBox;