import UpperHeader from "./UpperHeader";
import InterfaceListContainer from "./InterfaceListContainer"
import MethodName from "./MethodName"
import MethodItemBox from "./ValueBox";
import AnnotationContainer from "./AnnotationContainer";
import deepCopy from "json-deep-copy";
import MultipleMethodItemBox from "./MultipleMethodItemBox";
import MethodAnnotations from "./MethodAnnotations";
import ContainerBox from "./ContainerBox";


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
        <ContainerBox upperHeaderText={'class'} count={count} semantic='and' parentAttribute={attribute} item={scopeObject} onUpdate={(updatedValue => onGenericUpdate(updatedValue, attribute))}>

        
        { 
          Array.isArray(scopeObject[attribute]) &&
          <InterfaceListContainer backgroundColor='#EEEEEE' onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} items={scopeObject[attribute]} parentAttribute={attribute} />} 
        { 
          !Array.isArray(scopeObject[attribute]) && 
            
            <MultipleMethodItemBox >
              <MethodItemBox  onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} >
                <MethodName text={`has a name`}  onUpdate={onUpdate} style={{background: 'yellow'}} item={scopeObject[attribute]} />
              </ MethodItemBox>
              {/* <MethodItemBox  onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} > */}
                { scopeObject[attribute].annotations && <MethodItemBox  onUpdate={(updatedValue) => this.onGenericUpdate(updatedValue, attribute)} ><MethodAnnotations onUpdate={(updateObj) => onGenericUpdate(updateObj, 'annotations')} items={scopeObject[attribute].annotations} optionType={attribute} /> </MethodItemBox>}
                {/* {scopeObject[attribute].annotations && <AnnotationContainer onUpdate={(updatedValue) => onGenericUpdate(updatedValue, 'annotations')} items={scopeObject[attribute].annotations} optionType={attribute} />} */}
              {/* </MethodItemBox> */}
            </MultipleMethodItemBox>
        }
        </ContainerBox>
        </React.Fragment>
    )}
    </React.Fragment>
  )
}

export default ClassSelectorContainer;