import InterfaceListContainer from "./InterfaceListContainer"
import Name from "./Name"
import ValueBox from "./ValueBox";
import Annotations from "./Annotations";
import SelectorBox from "./SelectorBox";
import MultipleItemBoxContainer from "./MultipleItemBoxContainer";
import deepCopy from "json-deep-copy";


function ClassSelectorContainer({ scopeObject, onUpdate}) {

  const onGenericUpdate = (updatedValue, attribute)  => {
    const updatedItem = deepCopy(scopeObject);
    updatedItem[attribute] = updatedValue;

    // removing empty items
    if(Array.isArray(updatedValue) === true) {
      if( updatedValue.length === 0 ) {
        delete updatedItem[attribute];
      } else {
        updatedItem[attribute] = updatedValue;
      }
    } else {  // assumption, the updateValue is a json thus Object.keys
      if ( Object.keys(updatedValue).length === 0) {
        delete updatedItem[attribute];
      } else {
        updatedItem[attribute] = updatedValue;
      }
    }
    onUpdate(updatedItem);
  }

  const updateAnnotationsInItem = (item, updatedValue, attribute) => {
    const updatedItem = deepCopy(item);
    updatedItem.annotations = updatedValue;
    onGenericUpdate(updatedItem, attribute);
  }

  const classSelectorAttributes=['type','superclass','interfaces'];

  return (
    <React.Fragment>
      {Object.keys(scopeObject).filter( filteredAttribute =>  classSelectorAttributes.includes(filteredAttribute) ).map( (attribute, count) => 
      <React.Fragment>
        { 
          Array.isArray(scopeObject[attribute]) &&
          <InterfaceListContainer backgroundColor='#EEEEEE' onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} items={scopeObject[attribute]} parentAttribute={attribute} />
        } 

        {/* TYPE, SUPERCLASS  */}
        { 
          !Array.isArray(scopeObject[attribute]) && scopeObject[attribute] && 
          <MultipleItemBoxContainer >
            <SelectorBox item={scopeObject[attribute]} attribute={attribute} text={'class'} count={count} semantic='and'  onUpdate={updatedValue => onGenericUpdate(updatedValue, attribute)}>
              <ValueBox item={scopeObject[attribute]} attributesToDelete={['name', 'matcher-mode']} onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} >
                <Name text={`... has a name`}  onUpdate={updatedValue => onGenericUpdate(updatedValue, attribute)} style={{background: 'yellow'}} item={scopeObject[attribute]} />
              </ ValueBox>
              {/* <ValueBox  onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} > */}
                { scopeObject[attribute].annotations && scopeObject[attribute].annotations.length > 0  && scopeObject[attribute].annotations &&
                  <ValueBox  item={scopeObject[attribute]} attributesToDelete={['annotations']} onUpdate={(updatedValue) => updateAnnotationsInItem(scopeObject[attribute], updatedValue, attribute)} >
                    <Annotations onUpdate={(updatedValue) => updateAnnotationsInItem( scopeObject[attribute], updatedValue, [attribute])} items={scopeObject[attribute].annotations} parentAttribute={attribute} /> 
                  </ValueBox>}
                {/* {scopeObject[attribute].annotations && <AnnotationContainer onUpdate={(updatedValue) => onGenericUpdate(updatedValue, 'annotations')} items={scopeObject[attribute].annotations} attribute={attribute} />} */}
              {/* </ValueBox> */}
            </SelectorBox>
          </MultipleItemBoxContainer>
        }



        </React.Fragment>
    )}
    </React.Fragment>
  )
}

export default ClassSelectorContainer;