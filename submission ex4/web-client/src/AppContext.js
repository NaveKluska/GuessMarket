import { createContext, useContext } from 'react';

/**
 * What every screen needs without passing it down by hand: who is logged in, and
 * runAction(work, afterSuccess) - runs a server call, shows the server's error message in the
 * error dialog if it fails, and refreshes events + users right away if it succeeds.
 */
export const AppContext = createContext(null);

export function useApp() {
    return useContext(AppContext);
}

export const NOT_YOURSELF_MESSAGE = 'You can only manage your own account.';
