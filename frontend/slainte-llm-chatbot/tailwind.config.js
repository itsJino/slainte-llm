/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx,js,jsx}"],
  theme: {
    extend: {
      animation: {
        'spin': 'spin 1s linear infinite',
        'spin-fast': 'spin 0.8s linear infinite',
        'spin-slow': 'spin 1.5s linear infinite reverse',
        'spin-reverse': 'spin 1.2s linear infinite reverse',
        'pulse': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'pulse-strong': 'pulse-strong 1.5s ease-in-out infinite',
        'typing': 'typing 2.5s steps(12, end) infinite, blink 0.75s step-end infinite',
      },
      keyframes: {
        spin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        pulse: {
          '0%, 100%': { opacity: 1 },
          '50%': { opacity: 0.5 },
        },
        'pulse-strong': {
          '0%': { transform: 'scale(0.85)', opacity: 0.6 },
          '50%': { transform: 'scale(1.1)', opacity: 1 },
          '100%': { transform: 'scale(0.85)', opacity: 0.6 },
        },
        typing: {
          '0%': { width: '0ch' },
          '30%': { width: '9ch' }, 
          '80%': { width: '9ch' },
          '90%': { width: '0ch' },
          '100%': { width: '0ch' },
        },
        blink: {
          '0%': { borderRightColor: 'transparent' },
          '50%': { borderRightColor: '#006354' },
          '100%': { borderRightColor: 'transparent' },
        }
      },
      boxShadow: {
        'glow': '0 0 8px 1px rgba(115, 230, 194, 0.3)',
      },
      borderWidth: {
        '3': '3px',
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}