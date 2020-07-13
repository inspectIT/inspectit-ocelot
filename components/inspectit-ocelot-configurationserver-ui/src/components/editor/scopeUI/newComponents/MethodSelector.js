import UpperHeader from "./UpperHeader";
import deepCopy from "json-deep-copy";
import Item from "./Item";
import MethodAnnotationContainer from "./MethodAnnotationContainer";
import BooleanItem from "./BooleanItem";
import LowerHeader from "./LowerHeader";

// Either the container displays a single <Item> or an List of <Item>
function MethodSelector({item, parentAttribute, selectorType, selectorContainerIndex, onUpdate  }) {

  console.log('methodSelector',item);
  const onGenericUpdate = ( updatedValue, parentAttribute ) => {
    let updatedItem = deepCopy(item);
    
    if(Array.isArray(updatedValue) === true) {
      if( updatedValue.length === 0 ) {
      } else {
        updatedItem[parentAttribute] = updatedValue;
      }
    } else {  // assumption, the updateValue is a json thus Object.keys
      if ( Object.keys(updatedValue).length === 0) {
        delete updatedItem[parentAttribute];
      } else {
        updatedItem[parentAttribute] = updatedValue;
      }
    }
    onUpdate(updatedItem);
  }

  // distinction between array elements and non array elements
  return (
    // item[optionType] && (
      <div style={{ background: 'red', marginBottom: '25px', padding: '25px'}}>
        <h4> where am i </h4>
        { Object.keys(item).map( attribute => 
          <React.Fragment>
            <LowerHeader optionType={attribute} />
              {/* list of items */}
            { attribute === 'annotations' && console.log('x_1', item['annotations']) }
            { Array.isArray(item[attribute]) && attribute === 'annotations' &&  <MethodAnnotationContainer onUpdate={(updatedValue) => onGenericUpdate(updatedValue, parentAttribute)} index={selectorContainerIndex} items={item['annotations']} parentAttribute={parentAttribute} />}
            {/* { Array.isArray(item[optionType]) && optionType === 'arguments' &&  <ArgumentsContainer onUpdate={(updatedValue) => onGenericUpdate(updatedValue, optionType)} index={selectorContainerIndex} items={item[optionType]} parentAttribute={optionType} />}
            { Array.isArray(item[optionType]) && optionType === 'visibility' &&  <VisibilitynContainer onUpdate={(updatedValue) => onGenericUpdate(updatedValue, optionType)} index={selectorContainerIndex} items={item[optionType]} parentAttribute={optionType} />} */}

            {/* single item */}
            { !Array.isArray(item[attribute]) && (
              <React.Fragment>
                { typeof (item[attribute]) === 'boolean' && <BooleanItem item={item} attribute={attribute} onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} />}
                { parentAttribute === 'name' && <Item item={item} parentAttribute={attribute} onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} />}
              </React.Fragment>
            )}
          </React.Fragment>
        )})
      </div>
    // )
  )
}

export default MethodSelector;

