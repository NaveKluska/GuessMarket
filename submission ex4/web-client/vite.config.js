import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        strictPort: true,
        open: true,
        // The browser only talks to this dev server; anything under /guessmarket is forwarded to
        // Tomcat, so page and API share one origin and the untouched Ex3 server needs no CORS.
        proxy: {
            '/guessmarket': 'http://localhost:8080'
        }
    }
});
