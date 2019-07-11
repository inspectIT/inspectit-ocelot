import Menubar from '../components/layout/Menubar';
import SideNavigation from '../components/layout/SideNavigation';

/**
 * The main layout component.
 */
const MainLayout = (props) => {
    return (
        <div>
            <style global jsx>{`
            body {
              margin: 0;
            }
            `}</style>
            <style jsx>{`
            .content {
                margin-left: 3rem;
            }
            `}</style>
            <Menubar />
            <SideNavigation />
            <div className="content">
                {props.children}
            </div>
        </div>
    )
}

export default MainLayout;