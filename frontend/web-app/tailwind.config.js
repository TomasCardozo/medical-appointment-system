/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#ecfeff",
          100: "#cffafe",
          500: "#0f766e",
          600: "#0d5f58",
          700: "#134e4a"
        },
        accent: {
          100: "#ffedd5",
          400: "#fb923c",
          500: "#f97316"
        }
      },
      boxShadow: {
        soft: "0 10px 30px -12px rgba(15, 118, 110, 0.35)"
      }
    }
  },
  plugins: []
};
