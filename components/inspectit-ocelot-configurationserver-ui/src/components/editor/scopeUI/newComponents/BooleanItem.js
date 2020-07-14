import deepCopy from "json-deep-copy";
import {InputSwitch} from 'primereact/inputswitch';
function BooleanItem({item, value, onUpdate , text }) {

  return (
  item && (
      <div style={{ display:'flex', alignItems:'center'}}>
        <p style={{marginRight: '15px' }}> {text} </p>
        <InputSwitch checked={item} onChange={(e) => onUpdate(e.value)} />
      </div>
    )
  )
}

 
export default BooleanItem;