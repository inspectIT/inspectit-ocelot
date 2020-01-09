import NavigationItem from './SideNavigationItem';

/** Data */
import itemData from '../../data/side-navigation-items.json'

/**
 * The application's side-navigation.
 * The items are defined in the JSON file which has been imported.
 */
const SideNavigation = () => {
  return (
    <div className="this">
      <style jsx>{`
        .this {
          position: fixed;
          top: 4rem;
          left: 0;
          bottom: 0;
          width: 4rem;
          background-color: #eee;
          display: flex;
          flex-direction: column;
          align-items: center;
          border-right: 1px solid #ddd;
        }
        .bottom {
          width: inherit;
          display: flex;
          flex-direction: column;
          position: fixed;
          bottom: 0;
        }
        .top {
          width: inherit;
          display: flex;
          flex-direction: column;
        }
      `}</style>
      <div className='top'>
        {itemData.map(item => (
          item.align === 'top' ? <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} /> : ''
        ))}
      </div>
      <div className='bottom'>
        {itemData.map(item => (
          item.align === 'bottom' ? <NavigationItem key={item.name} href={item.href} name={item.name} icon={item.icon} /> : ''
        ))}
      </div>
    </div>
  )
}

export default SideNavigation;