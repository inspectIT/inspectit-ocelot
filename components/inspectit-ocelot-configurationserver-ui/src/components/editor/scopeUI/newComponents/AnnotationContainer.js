// import ValueBox from "./ValueBox";
// import Name from "./Name";
// import deepCopy from "json-deep-copy";

// function AnnotationContainer({items, onUpdate, parentAttribute}) {

//   const onUpdateListItem = ( updatedValue , index ) => {
//     const updatedItems = deepCopy(items);
//     // let updatedItems = items;

//     if ( Object.keys(updatedValue).length == 0) { // the { } updatedValue is empty, thus it can be removed from the array 
//       updatedItems = updatedItems.filter( (item, filterIndex ) => {
//         if (filterIndex !== index ) return item;
//       })
//     } else { // updatedValue is not empty, and must be modified within the index postion in the array
//       updatedItems[index] = updatedValue;
//     }
//     onUpdate(updatedItems);
//   }

    
//   return (
//     <React.Fragment>
//         {items.map( item => 
//         <ValueBox item={item}attributesToDelete={['name', 'matcher-mode']} onUpdate={(updatedValue) => onUpdateListItem(updatedValue, parentAttribute)} >
//           <Name text={`has an annotation`}  onUpdate={onUpdate} style={{background: 'yellow'}} item={item} />
//         </ValueBox>
//       )}
//     </React.Fragment>
//   )

// }

// export default AnnotationContainer;