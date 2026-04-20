/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#2563eb',
          hover: '#1d4ed8',
          soft: '#eef2ff',
        },
      },
      fontFamily: {
        sans: [
          'Inter',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'Roboto',
          'sans-serif',
        ],
      },
      boxShadow: {
        card: '0 1px 2px rgba(17, 24, 39, 0.04)',
        'card-hover': '0 4px 12px rgba(17, 24, 39, 0.08)',
      },
    },
  },
  plugins: [],
};
