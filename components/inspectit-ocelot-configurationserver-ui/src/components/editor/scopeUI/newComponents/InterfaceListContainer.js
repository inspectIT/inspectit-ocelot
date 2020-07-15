import MethodName from "./MethodName";
import MethodItemBox from "./ValueBox";
import LowerHeader from "./LowerHeader";
import AnnotationContainer from "./AnnotationContainer";
import MultipleMethodItemBox from "./MultipleMethodItemBox";
import ContainerBox from "./ContainerBox";

// parent attribute = 'interfaces'
function InterfaceListContainer( {items, parentAttribute, onUpdate,} ) {

  const onUpdateListItem = ( updatedValue , index ) => {
    let updatedItems = items;

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
      <div data-optiontype={parentAttribute} style={{  marginBottom: '',  position:'relative', height: '', padding: '25px', background: 'white', border: '1px solid lightgrey' , borderRadius: '10px'}}>
        <LowerHeader optionType={parentAttribute} />
        { items.map( item => 
        <MultipleMethodItemBox>
          <ContainerBox  parentAttribute={'interfaces'} item={items} onUpdate={ updatedValue => onUpdateListItem(updatedValue, index)}>
            <MethodItemBox  onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} > 
              <MethodName text={`has a name`}  onUpdate={onUpdate} style={{background: 'yellow'}} item={item} />
            </MethodItemBox>

            <MethodItemBox  onUpdate={(updatedValue) => onGenericUpdate(updatedValue, attribute)} > 
              {item.annotations && <AnnotationContainer onUpdate={(updatedValue) => onUpdateListItem(updatedValue, 'annotations')} items={item.annotations} optionType={parentAttribute} />}
            </MethodItemBox>
          </ContainerBox>
        </MultipleMethodItemBox>
        )}

      </div>
    </React.Fragment>
  )
}

export default InterfaceListContainer;