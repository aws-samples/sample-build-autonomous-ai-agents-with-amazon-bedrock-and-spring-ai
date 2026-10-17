import { useEffect, useState } from 'react';
import { getUser, login, logout, type User } from './lib/auth';
import { applyTheme, getInitialTheme, type Theme } from './lib/theme';
import { Login } from './components/Login';
import { Dashboard } from './components/Dashboard';

export function App() {
  const [user, setUser] = useState<User | null>(getUser());
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  // Keep the document root and localStorage in sync with the active theme.
  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  function toggleTheme() {
    setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  }

  if (!user) {
    return <Login theme={theme} onToggleTheme={toggleTheme} onLogin={(name) => setUser(login(name))} />;
  }

  return (
    <Dashboard
      user={user}
      theme={theme}
      onToggleTheme={toggleTheme}
      onLogout={() => {
        logout();
        setUser(null);
      }}
    />
  );
}
