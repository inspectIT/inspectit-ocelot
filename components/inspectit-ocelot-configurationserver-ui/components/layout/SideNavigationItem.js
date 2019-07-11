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
                    font-size: 2rem;
                    color: #bbb;
                }
                .anchor.active .pi, .anchor:hover .pi {
                    color: #888;
                }
                .anchor {
                    width: 100%;
                    text-align: center;
                    padding: 0.75rem 0.25rem;
                }
                .anchor.active {
                    padding-left: 0;
                    border-left: 0.25rem solid #e8a034;
                }
                `}</style>
                <i className={"pi " + icon} />
            </a>
        </Link>
    )
}

export default withRouter(SideNavigationItem);