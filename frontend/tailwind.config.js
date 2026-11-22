/** @type {import('tailwindcss').Config} */

module.exports = {
    content: ['./**/*.html'],

    darkMode: 'class',

    theme: {
        extend: {
            colors: {
                primary: '#4F46E5',       // Indigo 600
                secondary: '#7C3AED',     // Violet 600
                success: '#10B981',       // Emerald 500
                danger: '#EF4444',        // Red 500
                warning: '#F59E0B',       // Amber 500
            },

            fontFamily: {
                sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
            },

            boxShadow: {
                'card': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
            }
        }
    },

    plugins: [
        // Future plugins para Nx:
        // @nx/tailwindcss
        // nxPlugin.tsConfigPaths
    ]
};
