import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

interface NavSidebarProps {
  collapsed: boolean;
}

interface NavItem {
  to: string;
  label: string;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard' },
  { to: '/my-schedule', label: 'My Schedule' },
  { to: '/shifts', label: 'Shifts' },
  { to: '/shift-assignments', label: 'Shift Assignments' },
  { to: '/leave-requests', label: 'Leave Requests' },
  { to: '/employees', label: 'Employees' },
  { to: '/departments', label: 'Departments' },
];

const EMPLOYEE_ALLOWED = new Set<string>([
  '/',
  '/my-schedule',
  '/shifts',
  '/shift-assignments',
  '/leave-requests',
]);

const MANAGE_ROUTES = new Set<string>(['/employees', '/departments']);

export function NavSidebar({ collapsed }: NavSidebarProps) {
  const { user } = useAuth();
  const role = user?.roleName;

  const visible =
    role === 'Employee'
      ? NAV_ITEMS.filter((item) => EMPLOYEE_ALLOWED.has(item.to))
      : NAV_ITEMS;

  const primary = visible.filter((i) => !MANAGE_ROUTES.has(i.to));
  const manage = visible.filter((i) => MANAGE_ROUTES.has(i.to));

  return (
    <aside
      className={collapsed ? 'app-sidebar app-sidebar--collapsed' : 'app-sidebar'}
      aria-hidden={collapsed}
    >
      <div className="app-sidebar-inner">
        <div className="app-nav-section">Workspace</div>
        <nav className="app-nav">
          {primary.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              tabIndex={collapsed ? -1 : 0}
              className={({ isActive }) =>
                isActive ? 'app-nav-link app-nav-link--active' : 'app-nav-link'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        {manage.length > 0 && (
          <>
            <div className="app-nav-section">Manage</div>
            <nav className="app-nav">
              {manage.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  tabIndex={collapsed ? -1 : 0}
                  className={({ isActive }) =>
                    isActive ? 'app-nav-link app-nav-link--active' : 'app-nav-link'
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </>
        )}
      </div>
    </aside>
  );
}
