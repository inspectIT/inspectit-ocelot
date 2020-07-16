

// // eigene information an mich, kann übersprungen werden beim review

// import Heading from "./Heading";
// import InterfaceListContainer from "./InterfaceListContainer";
// import Item from "./Item";

// // diese Komponente soll die abstrakteste Form eines listenContainer sein, Sie kümmert sich um das updateden und beachtet dabei den index. (mein Gedanken zu dieser Komponente)
// function GenericListContainer( {items, parentAttribute, onUpdate, selectorType} ) {

//   const onUpdateListItem = ( updatedValue , index ) => {
//     let updatedItems = items;

//     if ( Object.keys(updatedValue).length == 0) { 
//       // the { } updatedValue is empty, thus it can be removed from the array 
//       updatedItems = updatedItems.filter( (item, filterIndex ) => {
//         if (filterIndex !== index ) return item;
//       })
//     } else { 
//       // updatedValue is not empty, and must be modified within the index postion in the array
//       updatedItems[index] = updatedValue;
//     }
//     onUpdate(updatedItems);
//   }
//   console.log('888', items)

//   return (
//     <React.Fragment>
//       { items && (
//         <React.Fragment>
//           {/* <Heading selectorType={selectorType} optionType={optionType} selectorContainerIndex={selectorContainerIndex} /> */}
//           <h4> ... and the class </h4>
//           {items.map( (item, index) => 
//             <React.Fragment>
//               {/* list of items */}
//               { 
//                 Array.isArray(item) && <InterfaceListContainer onUpdate={(updatedValue) => onUpdateListItem(updatedValue, index)} index={index} items={item} parentAttribute={'aaa optiontype'} />
//               }
      
//               {/* TODO: extract the optionType out of the json. Is that needed? */}
//               {/* single item */}
//               { 
//                 !Array.isArray(item) && <Item item={item} parentAttribute={'bbb optionType'} onUpdate={(updatedValue) => onUpdateListItem(updatedValue, index)}/>
//               }
//             </React.Fragment>
//             )}
//         </React.Fragment>
//       )}
//   </React.Fragment>     
//   )
// }

// export default GenericListContainer;