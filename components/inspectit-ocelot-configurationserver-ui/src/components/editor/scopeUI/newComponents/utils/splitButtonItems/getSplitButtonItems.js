
import { type, interfaces, superclass , methods } from './definitions';
import deepCopy from 'json-deep-copy';

const splitButtonItemsObject = {
  type: {
    splitButtonItems: type,
    alias: 'class'
  },
  interfaces: {
    splitButtonItems: interfaces
  },
  superclass: {
    splitButtonItems: superclass
  },
  methods: {
    splitButtonItems: methods
  }

}

// ## obsolete, since going this way the command has no access to the item, in which a attribute is going to be added
// ## on first overview, i did not find a way to "pass" the item , and the onUpdate function into this command
// ## the next solution is to create the command within the component, so we can access the requiered 2 props.
// const createSplitButtonArrayObject = () => {
//   attributes.map( attribute => {
//     let complettedSplittButtonArray = [];
//     let label = attribute.getLabel(attribute);
//     let command = attribute.getCommand(attribute);
//     let complettedSplittButtonArrayItem = { label: label, command: command};

//   })
// }


export const getSplitButtonsItems = (attribute, item) => {
  if (splitButtonItemsObject[attribute]) return splitButtonItemsObject[attribute].splitButtonItems;
};

// Diese Funktionalität ermöglicht, dass splittButtons Attribute innerhalb der Komponente erstellen können. 
// we got generic splitButtonItems, but they are missing the this.props.item information
// we duplicate the splitButtonItems into updatedSplittButtonItems and pass the item into the createAttribute via the requiered .command key of <SplitButton />
export const enableCreateAttributeWithinSplitItemEntries = (splittButtonItems, item, onUpdate) => {
  // updatedSplittButtonItems = splittButtonItems ( for each key, but 'createAttribute )
  let updatedSplittButtonItems = [];
  splittButtonItems && splittButtonItems.map(splittItem => {
    let updatedSplitItem = {};
    Object.keys(splittItem).map( key => { 
      // adding required .command key 
      if ( key === 'createAttribute' ) {
        updatedSplitItem.command = (e) => {
          let updatedItem = deepCopy(item);
          updatedItem = splittItem.createAttribute(updatedItem); // passing the required item to createAttribute
          onUpdate(updatedItem)
        }
      } else { // copy
        updatedSplitItem[key] = splittItem[key];
      }
    }) 
    updatedSplittButtonItems.push(updatedSplitItem);
  })
  // result
  return updatedSplittButtonItems;
}