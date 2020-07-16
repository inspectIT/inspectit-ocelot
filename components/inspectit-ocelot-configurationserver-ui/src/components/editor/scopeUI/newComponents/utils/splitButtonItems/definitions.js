
// diese Komponente soll die Inhalte der (add) splittButtons beinhalten für die jeweiligen Attribute. Diese Inhalte werden von der Komponente, welche ein Set von Attributen 
// darstellen möchte. Ein Kernaspekt hier ist, dass eine createAttribute( ) bereitgestellt wird. Diese muss über innerhalb des Kontextes der Komponenete, dass Item bekommen.


// informationen an mich, über die Kernproblematik dieser Komponente 
// the {  } is missing the required command key
// we dont use it here, since the functionality requires the item into which the attribute is added. The item cant be accessed from here
// solution. The component will take the values from here and set the .command key within the components context, thus knowing the item

// example ( within the components context)
// command = (e) => {
//   let updatedItem = deepCopy(item);
//   updatedItem = splittItem.createAttribute(updatedItem); // passing the required item to createAttribute
//   onUpdate(updatedItem)
// }

export const type = [
  {
    actionId: 'adding name',
    label: 'specify the class name by its name.',
    icon: 'pi pi-refresh',
    createAttribute: (item) => {
      if (!item.name) {
        item.name = '',
        item['matcher-mode'] = 'EQUALS_FULLY'
      }
      return item;
    },
  },
  {
    actionId: 'adding annotation',
    label: 'specify the class by the annotations, which are attached to it.',
    icon: 'pi pi-times',
    createAttribute: (item) => {
      item.annotations = item.annotations || [];
      item.annotations.push({ name:'', 'matcher-mode': 'EQUALS_FULLY'})
      return item;
    },
  },
]

export const interfaces = [
  {
    actionId: 'adding name',
    label: 'specify this interface by its name',
    icon: 'pi pi-refresh',
    createAttribute: (item) => {
      if (!item.name) {
        item.name = '',
        item['matcher-mode'] = 'EQUALS_FULLY'
      }
      return item;
    },
   },
   { 
    actionId: 'adding annotation',
    label: 'specify this interface by the anotations, that is attached to it',
    icon: 'pi pi-refresh',
    createAttribute: (item) => {
      item.annotations = item.annotations || [];
      item.annotations.push({ name:'', 'matcher-mode': 'EQUALS_FULLY'})
      return item;
    },
   }
]


export const superclass = [
  {
    actionId: 'adding name',
    label: 'specify the superclass by its name.',
    icon: 'pi pi-refresh',
    createAttribute: (item) => {
      if (!item.name) {
        item.name = '',
        item['matcher-mode'] = 'EQUALS_FULLY'
      }
      return item;
    },
  },
  {
    actionId: 'adding annotation',
    label: 'specify the superclass by the annotations, which are attached to it.',
    icon: 'pi pi-times',
    createAttribute: (item) => {
      console.log('xxxx', item);
      item.annotations = item.annotations || [];
      item.annotations.push({ name:'', 'matcher-mode': 'EQUALS_FULLY'})
      return item;
    },
  },
]

export const methods = [
  {
    actionId: 'adding name',
    label: 'add name',
    icon: 'pi pi-refresh',
    createAttribute: (item) => {
      if (!item.name) {
        item.name = '',
        item['matcher-mode'] = 'EQUALS_FULLY'
      }
      return item;
    },
  },
  {
    actionId: 'adding annotation',
    label: 'add annotation',
    icon: 'pi pi-times',
    createAttribute: (item) => {
      item.annotations = item.annotations || [];
      item.annotations.push({ name:'', 'matcher-mode': 'EQUALS_FULLY'})
      return item;
    },
  },
  {
    actionId: 'adding visibility',
    label: 'add visibility option',
    icon: 'pi pi-external-link',
    createAttribute: (item) => {
      item.visibility = [];
      return item;
    },
  },
  {   
    actionId: 'adding arguments',
    label: 'add arguments option',
    icon: 'pi pi-upload',
    createAttribute: (item) => {
      item.arguments = item.arguments || [];
      item.arguments.push('')
      return item;
    },
  },
  {   
    actionId: 'adding is-synchronized',
    label: 'add      is-synchronized option',
    icon: 'pi pi-upload',
    createAttribute: (item) => {
      item['is-synchronized'] = true;
      return item;
    },
  },
  {   
    actionId: 'adding is-constructor',
    label: 'add     is-constructor option',
    icon: 'pi pi-upload',
    createAttribute: (item) => {
      item['is-constructor'] = true;
      return item;
    },
  }
]