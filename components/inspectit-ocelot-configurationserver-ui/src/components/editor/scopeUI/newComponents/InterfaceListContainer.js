import Name from "./Name";
import ValueBox from "./ValueBox";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from "./AnnotationContainer";
import MultipleValueBox from "./MultipleItemBoxContainer";
import SelectorBox from "./SelectorBox";
import Heading from "./Heading";
import Annotations from "./Annotations";
import { Button } from "primereact/button";

// parent attribute = 'interfaces'
function InterfaceListContainer( {items, parentAttribute, onUpdate,} ) {

  const onUpdateListItem = ( updatedValue , index ) => {
    let updatedItems = items;
    console.log('item')
    if ( Object.keys(updatedValue).length == 0) { 
      // the { } updatedValue is empty, thus it can be removed from the array 
      updatedItems = updatedItems.filter( (item, filterIndex ) => {
        if (filterIndex !== index ) return item;
      })
    } else { 
      // updatedValue is not empty, and must be modified within the index postion in the array
      updatedItems[index] = updatedValue;
    }
    onUpdate(updatedItems);
  }

  return (
    <React.Fragment>
      <h4 style={{marginBottom:'5px'}}> The class must implement all of the following interfaces:</h4>
      <div data-optiontype={parentAttribute} style={{  marginBottom: '',  position:'relative', height: '', padding: '25px', background: '#EEEEEE' , borderRadius: '10px'}}>
        <Button tooltip='add another interface, that the class must implement' icon="pi pi-plus" style={{left:'50%'}}></Button>
        { items.map( (item, index) => 
        
        <MultipleValueBox>

          <SelectorBox  style={{border: '1px solid grey'}} attribute={'interfaces'} item={item} onUpdate={ updatedValue => onUpdateListItem(updatedValue, index)}>
          { item['matcher-mode'] && 
            <ValueBox item={item} attributesToDelete={['name','matcher-mode']} onUpdate={(updatedValue) => onUpdateListItem(updatedValue, index)} > 
              <Name text={`has a name`}  onUpdate={(updatedValue) => onUpdateListItem(updatedValue, index) } style={{background: 'yellow'}} item={item} />
            </ValueBox>
          }

          { item['annotations'] && 
            <ValueBox  item={item} attributesToDelete={['annotations']} onUpdate={(updatedValue) => onUpdateListItem(updatedValue, index)} > 
              {item.annotations && <Annotations onUpdate={(updatedValue) => onUpdateListItem(updatedValue, 'annotations')} items={item.annotations} optionType={parentAttribute} />}
            </ValueBox>
          }
          </SelectorBox>
        </MultipleValueBox>
        )}

      </div>
    </React.Fragment>
  )
}



export default InterfaceListContainer;