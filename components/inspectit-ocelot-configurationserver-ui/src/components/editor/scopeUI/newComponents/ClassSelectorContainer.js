import UpperHeader from "./UpperHeader";
import InterfaceListContainer from "./InterfaceListContainer"
import Item from "./Item"

const { default: deepCopy } = require("json-deep-copy");

function ClassSelectorContainer({ scopeObject, onUpdate}) {

  const onGenericUpdate = (updatedValue, attribute)  => {
    const updatedItem = deepCopy(scopeObject);
    updatedItem[attribute] = updatedValue;
    onUpdate(updatedItem);
  }

  const classSelectorAttributes=['type','superclass','interfaces'];

  return (
    <React.Fragment>
      {Object.keys(scopeObject).filter( filteredAttribute =>  classSelectorAttributes.includes(filteredAttribute) ).map( (attribute, count) => 
      <React.Fragment>
        <UpperHeader attributeText={'class'} connectionTypeAndOr={'and'} count={count} />
        { 
          Array.isArray(scopeObject[attribute]) &&
          <InterfaceListContainer backgroundColor='#EEEEEE' onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} items={scopeObject[attribute]} parentAttribute={attribute} />} 
        { 
        !Array.isArray(scopeObject[attribute]) && <Item backgroundColor='#EEEEEE' item={scopeObject[attribute]} parentAttribute={attribute} onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)}/> 
        }
      </React.Fragment>
    )}
    </React.Fragment>
  )
}

export default ClassSelectorContainer;