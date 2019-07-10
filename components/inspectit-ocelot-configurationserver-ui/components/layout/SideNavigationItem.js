import { withRouter } from 'next/router';
import Link from '../basics/Link'

/**
 * Component used by the side navigation as items.
 */
const SideNavigationItem = ({ href, icon, name, router }) => {
    const isActive = router.pathname.endsWith(href);

    return (
        <Link className="this" href={href}>
            <a className={"anchor" + (isActive ? " active" : "")} title={name}>
                <style jsx>{`
                .pi {
                    font-size: 2.5rem;
                }
                .anchor {
                    width: 100%;
                    text-align: center;
                    border-bottom: 1px solid #ccc;
                    padding: 0.25rem 0;
                }
                .anchor.active, .anchor:hover {
                    background-color: #00000022;
                }
                `}</style>
                <i className={"pi " + icon} />
            </a>
        </Link>
    )
}

export default withRouter(SideNavigationItem);